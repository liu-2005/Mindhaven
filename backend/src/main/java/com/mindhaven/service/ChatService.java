package com.mindhaven.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindhaven.ai.AiAdapter;
import com.mindhaven.ai.OutputSafetyService;
import com.mindhaven.ai.OutputSafetyRules;
import com.mindhaven.common.BizException;
import com.mindhaven.common.ErrorCode;
import com.mindhaven.dto.ChatStreamRequest;
import com.mindhaven.dto.CreateSessionRequest;
import com.mindhaven.dto.EmotionAnalysisDto;
import com.mindhaven.entity.ChatMessage;
import com.mindhaven.entity.ChatSession;
import com.mindhaven.entity.EmotionAnalysis;
import com.mindhaven.entity.RiskEvent;
import com.mindhaven.repository.ChatMessageRepository;
import com.mindhaven.repository.ChatSessionRepository;
import com.mindhaven.repository.EmotionAnalysisRepository;
import com.mindhaven.repository.RiskEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class ChatService {

    private static final long SSE_TIMEOUT_MS = 180_000L;

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final EmotionAnalysisRepository analysisRepository;
    private final RiskEventRepository riskEventRepository;
    private final AiAdapter aiAdapter;
    private final ObjectMapper objectMapper;
    private final ChatTurnStore turnStore;
    private final OutputSafetyService outputSafety;
    private final java.util.concurrent.locks.ReentrantLock[] conversationLocks = java.util.stream.IntStream.range(0, 256)
            .mapToObj(i -> new java.util.concurrent.locks.ReentrantLock()).toArray(java.util.concurrent.locks.ReentrantLock[]::new);
    private final com.mindhaven.security.RequestLimits limits = new com.mindhaven.security.RequestLimits();
    private final com.mindhaven.ai.LocalRuleBasedAiAdapter localSafety = new com.mindhaven.ai.LocalRuleBasedAiAdapter();

    private java.util.concurrent.locks.ReentrantLock lockFor(Long userId) {
        return conversationLocks[Math.floorMod(userId.hashCode(), conversationLocks.length)];
    }

    public ChatService(ChatSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       EmotionAnalysisRepository analysisRepository,
                       RiskEventRepository riskEventRepository,
                       AiAdapter aiAdapter,
                       ObjectMapper objectMapper, ChatTurnStore turnStore, OutputSafetyService outputSafety) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.analysisRepository = analysisRepository;
        this.riskEventRepository = riskEventRepository;
        this.aiAdapter = aiAdapter;
        this.objectMapper = objectMapper;
        this.turnStore = turnStore;
        this.outputSafety = outputSafety;
    }

    @Transactional
    public ChatSession createSession(Long userId, CreateSessionRequest request) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        String title = StringUtils.hasText(request.title()) ? request.title().trim() : "新会话";
        session.setTitle(title);
        session.setStatus("ACTIVE");
        session.setMessageCount(0);
        session = sessionRepository.save(session);

        if (StringUtils.hasText(request.initialMessage())) {
            ChatMessage msg = new ChatMessage();
            msg.setSessionId(session.getId());
            msg.setSenderType("USER");
            msg.setContent(request.initialMessage().trim());
            msg.setStatus("COMPLETED");
            messageRepository.save(msg);
            session.setMessageCount(1);
            session.setLastMessageAt(LocalDateTime.now());
            session = sessionRepository.save(session);
        }
        return session;
    }

    public Page<ChatSession> listSessions(Long userId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, safeSize);
        return sessionRepository.findByUserIdOrderByLastMessage(userId, pageable);
    }

    public ChatSession getSession(Long userId, Long sessionId) {
        return sessionRepository.findByIdAndUserIdAndDeletedAtIsNull(sessionId, userId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Transactional
    public ChatSession updateSession(Long userId, Long sessionId, String title, String status) {
        ChatSession session = getSession(userId, sessionId);
        if (StringUtils.hasText(title)) {
            session.setTitle(title.trim());
        }
        if (StringUtils.hasText(status)) {
            if (!List.of("ACTIVE", "ARCHIVED").contains(status)) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "非法的会话状态");
            }
            session.setStatus(status);
        }
        return sessionRepository.save(session);
    }

    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        var lock = lockFor(userId);
        if (!lock.tryLock()) throw new BizException(ErrorCode.RESOURCE_CONFLICT, "会话处理中，请稍后删除");
        try {
        ChatSession session = getSession(userId, sessionId);
        turnStore.deleteSession(sessionId);
        messageRepository.deleteBySessionId(sessionId);
        analysisRepository.deleteBySourceTypeAndSourceId("CHAT_SESSION", sessionId);
        session.setDeletedAt(LocalDateTime.now());
        session.setStatus("DELETED");
        sessionRepository.save(session);
        } finally { lock.unlock(); }
    }

    public Page<ChatMessage> listMessages(Long userId, Long sessionId, int page, int size) {
        getSession(userId, sessionId);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, safeSize);
        return messageRepository.findBySessionIdOrderByIdAsc(sessionId, pageable);
    }

    public EmotionAnalysisDto emotionOf(Long userId, Long sessionId) {
        getSession(userId, sessionId);
        return analysisRepository.findFirstBySourceTypeAndSourceIdAndCurrentResultOrderByIdDesc("CHAT_SESSION", sessionId, true)
                .map(this::toEmotionDto)
                .orElse(new EmotionAnalysisDto(null, null, null, "NORMAL", null, null,
                        List.of(), List.of(), null, aiAdapter.emotionModelVersion(), "PENDING"));
    }

    public void stream(Long userId, ChatStreamRequest request, SseEmitter emitter) {
        var lock = lockFor(userId);
        boolean acquired = lock.tryLock();
        ChatTurnStore.Claim claim = null;
        try {
            if (!acquired) throw new BizException(ErrorCode.RESOURCE_CONFLICT, "已有对话正在处理，请等待完成");
            claim = turnStore.claim(userId, request);
            if (claim.prepared() == null) {
                limits.check("chat-minute:" + userId, 20, java.time.Duration.ofMinutes(1));
                limits.check("chat-day:" + userId, 200, java.time.Duration.ofDays(1));
                ChatSession session = getSession(userId, request.sessionId());
                var previousUser = messageRepository.findBySessionIdAndClientRequestIdAndSenderType(session.getId(), request.requestId(), "USER");
                if (previousUser.isPresent() && !previousUser.get().getContent().equals(request.message().trim())) {
                    throw new BizException(ErrorCode.RESOURCE_CONFLICT, "同一请求标识不能用于不同消息");
                }
                ChatMessage userMsg = previousUser.orElseGet(() -> {
                    ChatMessage msg = new ChatMessage();
                    msg.setSessionId(session.getId());
                    msg.setSenderType("USER");
                    msg.setContent(request.message().trim());
                    msg.setClientRequestId(request.requestId());
                    msg.setStatus("COMPLETED");
                    return messageRepository.saveAndFlush(msg);
                });
                // Legacy replies have no checkpoint. Reuse their content as a candidate, then review and repair once.
                var legacy = messageRepository.findBySessionIdAndClientRequestIdAndSenderType(session.getId(), request.requestId(), "ASSISTANT");
                PreparedChatTurn prepared = prepare(session, userMsg, request, legacy.orElse(null));
                turnStore.checkpoint(claim, prepared);
            }
            ChatTurnStore.Delivery delivery = turnStore.finish(claim, prepared -> persistPrepared(userId, request, prepared));
            sendChunks(emitter, delivery);
        } catch (Exception e) {
            try {
                String code = e instanceof BizException biz ? biz.errorCode().code() : "INTERNAL_ERROR";
                String message = e instanceof BizException ? e.getMessage() : "对话服务异常，请稍后再试";
                emitter.send(SseEmitter.event().name("error").data(Map.of("code", code, "message", message)));
                emitter.complete();
            } catch (Exception ignored) {
                emitter.completeWithError(e);
            }
        } finally {
            try { turnStore.release(claim); } catch (RuntimeException ignored) { /* The generation lease also expires. */ }
            if (acquired) lock.unlock();
        }
    }

    private PreparedChatTurn prepare(ChatSession session, ChatMessage userMsg, ChatStreamRequest request, ChatMessage legacy) {
        var inputSafety = localSafety.reply(request.message(), List.of());
        boolean fixedInput = riskRank(inputSafety.riskLevel()) >= 2;
        AiAdapter.ChatReply candidate = fixedInput ? inputSafety : legacy == null ? aiAdapter.reply(request.message(), loadHistory(session.getId()))
                : new AiAdapter.ChatReply(legacy.getContent(), inputSafety.riskLevel(), inputSafety.triggerCodes(), inputSafety.guidanceCode(), legacy.getModelProvider(), legacy.getModelVersion());
        var analysis = computeAnalysis(session, candidate, request.message());
        var emotion = analysis.result();
        String risk = emotion != null && riskRank(emotion.riskLevel()) > riskRank(candidate.riskLevel()) ? emotion.riskLevel() : candidate.riskLevel();
        if (riskRank(inputSafety.riskLevel()) > riskRank(risk)) risk = inputSafety.riskLevel();
        if (riskRank(risk) >= 2) {
            var safeReply = fixedReply(risk);
            if (emotion != null) emotion = new AiAdapter.EmotionAnalysisResult(Math.min(emotion.emotionScore(), "CRISIS".equals(risk) ? 19 : 39),
                    "CRISIS".equals(risk) ? "CRISIS" : "SEVERE_DISTRESS", true, risk, "用户表达中检测到高风险信号，请优先寻求现实支持。",
                    safeReply.content(), List.of("联系可信任的人", "寻求现实支持"), List.of());
            return new PreparedChatTurn(userMsg.getId(), safeReply, emotion, analysis.provider(), analysis.model(), false, List.of(), OutputSafetyRules.VERSION);
        }
        String surface = candidate.content();
        if (emotion != null) surface += "\n情绪说明：" + emotion.riskDescription() + "\n建议：" + emotion.suggestion() + "\n行动：" + String.join("；", emotion.improvements());
        var review = new OutputSafetyRules().review(candidate.content());
        if (review.allowed()) review = outputSafety.review(request.message(), surface);
        boolean blocked = !review.allowed();
        String content = blocked ? "这次我暂时无法提供可靠的建议。你可以继续描述自己的感受；需要专业判断时，请联系有资质的专业人士。" : candidate.content();
        var reply = new AiAdapter.ChatReply(content, risk, candidate.triggerCodes(), candidate.guidanceCode(),
                blocked ? "local-safety" : candidate.provider() == null ? aiAdapter.providerName() : candidate.provider(),
                blocked ? OutputSafetyRules.VERSION : candidate.model() == null ? aiAdapter.modelVersion() : candidate.model());
        return new PreparedChatTurn(userMsg.getId(), reply, blocked ? null : emotion, analysis.provider(), analysis.model(), blocked, review.codes(), OutputSafetyRules.VERSION);
    }

    private AiAdapter.ChatReply fixedReply(String risk) {
        String content = "CRISIS".equals(risk) ? localSafety.reply("不想活", List.of()).content()
                : "听到你正承受很大的压力，请先暂停手头的事情，联系你信任的人陪伴你，必要时寻求专业人士或当地紧急服务的支持。";
        return new AiAdapter.ChatReply(content, risk, List.of("HIGH_RISK_DETECTED"), "CRISIS".equals(risk) ? "IMMEDIATE_SUPPORT" : "LOCAL_SUPPORT", "local-safety", OutputSafetyRules.VERSION);
    }

    private int riskRank(String level) {
        return switch (level == null ? "NORMAL" : level) { case "CRISIS" -> 3; case "WARNING" -> 2; case "CONCERN" -> 1; default -> 0; };
    }

    public void recoverPrepared(ChatTurnStore.Claim claim) {
        var lock = lockFor(claim.userId());
        if (!lock.tryLock()) return;
        try {
            ChatMessage userMsg = messageRepository.findById(claim.prepared().userMessageId())
                    .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND));
            ChatStreamRequest request = new ChatStreamRequest(claim.sessionId(), userMsg.getClientRequestId(), userMsg.getContent());
            turnStore.finish(claim, prepared -> persistPrepared(claim.userId(), request, prepared));
        } finally { lock.unlock(); }
    }

    private List<AiAdapter.ChatTurn> loadHistory(Long sessionId) {
        Pageable pageable = PageRequest.of(0, 20);
        List<ChatMessage> recent = messageRepository.findBySessionIdOrderByIdDesc(sessionId, pageable).getContent().reversed();
        List<AiAdapter.ChatTurn> turns = new ArrayList<>();
        for (ChatMessage m : recent) {
            String role = "USER".equals(m.getSenderType()) ? "user" : "assistant";
            turns.add(new AiAdapter.ChatTurn(role, m.getContent()));
        }
        return turns;
    }

    private ChatMessage saveAssistantMessage(ChatSession session, AiAdapter.ChatReply reply, String requestId) {
        ChatMessage msg = messageRepository.findBySessionIdAndClientRequestIdAndSenderType(session.getId(), requestId, "ASSISTANT").orElseGet(ChatMessage::new);
        msg.setSessionId(session.getId());
        msg.setSenderType("ASSISTANT");
        msg.setClientRequestId(requestId);
        msg.setContent(reply.content());
        msg.setStatus("COMPLETED");
        msg.setFinishReason("STOP");
        msg.setModelProvider(reply.provider() == null ? aiAdapter.providerName() : reply.provider());
        msg.setModelVersion(reply.model() == null ? aiAdapter.modelVersion() : reply.model());
        msg.setCompletedAt(LocalDateTime.now());
        return messageRepository.saveAndFlush(msg);
    }

    private Long persistPrepared(Long userId, ChatStreamRequest request, PreparedChatTurn prepared) {
        ChatSession session = getSession(userId, request.sessionId());
        ChatMessage userMsg = messageRepository.findById(prepared.userMessageId()).orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!session.getId().equals(userMsg.getSessionId()) || !request.requestId().equals(userMsg.getClientRequestId())) {
            throw new BizException(ErrorCode.RESOURCE_CONFLICT, "会话结果与原始请求不一致");
        }
        ChatMessage assistant = saveAssistantMessage(session, prepared.reply(), request.requestId());
        saveAnalysis(session, prepared);
        updateSessionAfterMessage(session, request.message());
        if (!"NORMAL".equals(prepared.reply().riskLevel())) createRiskEvent(session, userMsg.getId(), prepared.reply());
        if (prepared.outputBlocked()) {
            List<String> codes = new ArrayList<>(); codes.add("OUTPUT_BLOCKED"); codes.addAll(prepared.outputCodes());
            createRiskEvent(session, assistant.getId(), new AiAdapter.ChatReply("", "WARNING", codes, "OUTPUT_BLOCKED", "local-safety", prepared.policyVersion()));
        }
        return assistant.getId();
    }

    private void updateSessionAfterMessage(ChatSession session, String firstMessage) {
        session.setMessageCount((int) messageRepository.countBySessionId(session.getId()));
        session.setLastMessageAt(LocalDateTime.now());
        if ("新会话".equals(session.getTitle()) && StringUtils.hasText(firstMessage)) {
            String title = firstMessage.trim();
            session.setTitle(title.length() > 20 ? title.substring(0, 20) : title);
        }
        sessionRepository.save(session);
    }

    private record AnalysisDraft(AiAdapter.EmotionAnalysisResult result, String provider, String model) {}

    private AnalysisDraft computeAnalysis(ChatSession session, AiAdapter.ChatReply reply, String latestMessage) {
        Pageable pageable = PageRequest.of(0, 20);
        List<ChatMessage> recent = messageRepository.findBySessionIdOrderByIdDesc(session.getId(), pageable).getContent().reversed();
        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : recent) {
            if ("USER".equals(m.getSenderType())) {
                sb.append("用户发言：").append(m.getContent()).append("\n\n");
            }
        }
        AiAdapter.EmotionAnalysisResult result = null;
        try {
            result = List.of("WARNING", "CRISIS").contains(reply.riskLevel())
                    ? localSafety.analyzeEmotion(latestMessage) : aiAdapter.analyzeEmotion(sb.toString());
        } catch (BizException ignored) {
            // Analysis failure must not discard a successful chat reply or reuse a stale score.
        }

        boolean fixedSafety = List.of("WARNING", "CRISIS").contains(reply.riskLevel());
        return new AnalysisDraft(result, fixedSafety ? localSafety.emotionProviderName() : aiAdapter.emotionProviderName(),
                fixedSafety ? localSafety.emotionModelVersion() : aiAdapter.emotionModelVersion());
    }

    private void saveAnalysis(ChatSession session, PreparedChatTurn prepared) {
        var result = prepared.emotion();
        var reply = prepared.reply();
        for (EmotionAnalysis old : analysisRepository.findBySourceTypeAndSourceIdAndCurrentResultTrue("CHAT_SESSION", session.getId())) {
            old.setCurrentResult(false);
            analysisRepository.save(old);
        }
        EmotionAnalysis analysis = new EmotionAnalysis();
        analysis.setSourceType("CHAT_SESSION");
        analysis.setSourceId(session.getId());
        if (result == null) {
            analysis.setRiskLevel(reply.riskLevel());
            analysis.setSuggestion("本轮情绪分析暂不可用，下一轮对话后会重新分析。");
            analysis.setImprovementSuggestionsJson("[]");
            analysis.setStatus("FAILED");
        } else {
            analysis.setEmotionScore(result.emotionScore());
            analysis.setLabel(result.label());
            analysis.setIsNegative(Boolean.TRUE.equals(result.isNegative()));
            analysis.setRiskLevel(result.riskLevel());
            analysis.setRiskDescription(result.riskDescription());
            analysis.setSuggestion(result.suggestion());
            analysis.setImprovementSuggestionsJson(toJson(result.improvements()));
            if (List.of("WARNING", "CRISIS").contains(reply.riskLevel())) {
                String risk = "CRISIS".equals(reply.riskLevel()) || "CRISIS".equals(result.riskLevel()) ? "CRISIS" : "WARNING";
                analysis.setRiskLevel(risk);
                analysis.setLabel("CRISIS".equals(risk) ? "CRISIS" : "SEVERE_DISTRESS");
                analysis.setEmotionScore(Math.min(result.emotionScore(), "CRISIS".equals(risk) ? 19 : 39));
                analysis.setIsNegative(true);
            }
            analysis.setStatus("SUCCEEDED");
        }
        analysis.setModelProvider(prepared.emotionProvider());
        analysis.setModelVersion(prepared.emotionModel());
        analysis.setPolicyVersion(prepared.policyVersion());
        analysis.setCurrentResult(true);
        analysis.setAnalyzedAt(LocalDateTime.now());
        analysisRepository.saveAndFlush(analysis);
    }

    private void createRiskEvent(ChatSession session, Long sourceId, AiAdapter.ChatReply reply) {
        var previous = riskEventRepository.findFirstBySourceTypeAndSourceIdOrderByIdDesc("CHAT_MESSAGE", sourceId);
        if (previous.isPresent() && riskRank(previous.get().getRiskLevel()) >= riskRank(reply.riskLevel())) return;
        RiskEvent event = previous.orElseGet(RiskEvent::new);
        event.setUserId(session.getUserId());
        event.setSourceType("CHAT_MESSAGE");
        event.setSourceId(sourceId);
        event.setRiskLevel(reply.riskLevel());
        event.setTriggerCodesJson(toJson(reply.triggerCodes()));
        event.setRedactedSummary("OUTPUT_BLOCKED".equals(reply.guidanceCode()) ? "模型输出未通过安全检查，已返回固定安全提示。"
                : "会话中检测到" + reply.riskLevel() + "风险信号，详情仅供授权人员查看。");
        event.setPolicyVersion(OutputSafetyRules.VERSION);
        event.setModelVersion(reply.model() == null ? aiAdapter.modelVersion() : reply.model());
        event.setStatus("OPEN");
        event.setResolvedAt(null);
        event.setResolvedBy(null);
        riskEventRepository.saveAndFlush(event);
    }

    private void sendChunks(SseEmitter emitter, ChatTurnStore.Delivery delivery) throws Exception {
        var prepared = delivery.prepared();
        var reply = prepared.reply();
        if (!"NORMAL".equals(reply.riskLevel())) {
            Map<String, Object> risk = new java.util.LinkedHashMap<>();
            risk.put("level", reply.riskLevel());
            risk.put("guidanceCode", reply.guidanceCode());
            emitter.send(SseEmitter.event().name("risk").data(risk));
        }
        if (prepared.outputBlocked()) emitter.send(SseEmitter.event().name("risk").data(Map.of("level", "WARNING", "guidanceCode", "OUTPUT_BLOCKED")));
        String content = reply.content();
        int chunkSize = 4;
        for (int i = 0; i < content.length(); i += chunkSize) {
            String chunk = content.substring(i, Math.min(i + chunkSize, content.length()));
            emitter.send(SseEmitter.event().name("message").data(Map.of("delta", chunk)));
            try {
                Thread.sleep(18);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        emitter.send(SseEmitter.event().name("done").data(Map.of("messageId", delivery.messageId(), "finishReason", "STOP")));
        emitter.complete();
    }

    private String policyVersionOf() {
        if (aiAdapter instanceof com.mindhaven.ai.LocalRuleBasedAiAdapter local) {
            return local.policyVersion();
        }
        return "policy-v1";
    }

    private EmotionAnalysisDto toEmotionDto(EmotionAnalysis a) {
        return new EmotionAnalysisDto(a.getEmotionScore(), a.getLabel(),
                Boolean.TRUE.equals(a.getIsNegative()), a.getRiskLevel(), a.getRiskDescription(),
                a.getSuggestion(), parseStringList(a.getImprovementSuggestionsJson()), List.of(),
                a.getAnalyzedAt(), a.getModelVersion(), a.getStatus());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    public long sseTimeoutMs() {
        return SSE_TIMEOUT_MS;
    }
}
