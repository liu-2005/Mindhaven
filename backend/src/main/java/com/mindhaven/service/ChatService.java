package com.mindhaven.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindhaven.ai.AiAdapter;
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

    public ChatService(ChatSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       EmotionAnalysisRepository analysisRepository,
                       RiskEventRepository riskEventRepository,
                       AiAdapter aiAdapter,
                       ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.analysisRepository = analysisRepository;
        this.riskEventRepository = riskEventRepository;
        this.aiAdapter = aiAdapter;
        this.objectMapper = objectMapper;
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
        ChatSession session = getSession(userId, sessionId);
        session.setDeletedAt(LocalDateTime.now());
        session.setStatus("DELETED");
        sessionRepository.save(session);
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
                        List.of(), List.of(), null, aiAdapter.modelVersion()));
    }

    public void stream(Long userId, ChatStreamRequest request, SseEmitter emitter) {
        try {
            ChatSession session = sessionRepository.findByIdAndUserIdAndDeletedAtIsNull(request.sessionId(), userId)
                    .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND, "会话不存在"));
            if (!"ACTIVE".equals(session.getStatus())) {
                throw new BizException(ErrorCode.RESOURCE_CONFLICT, "会话已关闭，请新建会话");
            }

            ChatMessage userMsg = messageRepository
                    .findBySessionIdAndClientRequestIdAndSenderType(session.getId(), request.requestId(), "USER")
                    .orElseGet(() -> {
                        ChatMessage msg = new ChatMessage();
                        msg.setSessionId(session.getId());
                        msg.setSenderType("USER");
                        msg.setContent(request.message().trim());
                        msg.setClientRequestId(request.requestId());
                        msg.setStatus("COMPLETED");
                        return messageRepository.save(msg);
                    });

            List<AiAdapter.ChatTurn> history = loadHistory(session.getId());
            AiAdapter.ChatReply reply = aiAdapter.reply(request.message(), history);

            ChatMessage assistantMsg = saveAssistantMessage(session, reply.content());
            updateSessionAfterMessage(session, request.message());

            analyzeSession(session);
            if (!"NORMAL".equals(reply.riskLevel())) {
                createRiskEvent(session, userMsg, reply);
            }

            sendChunks(emitter, reply, assistantMsg);
        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event().name("error")
                        .data(Map.of("code", "INTERNAL_ERROR", "message", "对话服务异常，请稍后再试")));
            } catch (Exception ignored) {
                // ignore
            }
            emitter.completeWithError(e);
        }
    }

    private List<AiAdapter.ChatTurn> loadHistory(Long sessionId) {
        Pageable pageable = PageRequest.of(0, 20);
        List<ChatMessage> recent = messageRepository.findBySessionIdOrderByIdAsc(sessionId, pageable).getContent();
        List<AiAdapter.ChatTurn> turns = new ArrayList<>();
        for (ChatMessage m : recent) {
            String role = "USER".equals(m.getSenderType()) ? "user" : "assistant";
            turns.add(new AiAdapter.ChatTurn(role, m.getContent()));
        }
        return turns;
    }

    private ChatMessage saveAssistantMessage(ChatSession session, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(session.getId());
        msg.setSenderType("ASSISTANT");
        msg.setContent(content);
        msg.setStatus("COMPLETED");
        msg.setFinishReason("STOP");
        msg.setModelProvider(aiAdapter.providerName());
        msg.setModelVersion(aiAdapter.modelVersion());
        msg.setCompletedAt(LocalDateTime.now());
        return messageRepository.save(msg);
    }

    private void updateSessionAfterMessage(ChatSession session, String firstMessage) {
        int count = session.getMessageCount() == null ? 0 : session.getMessageCount();
        session.setMessageCount(count + 2);
        session.setLastMessageAt(LocalDateTime.now());
        if ("新会话".equals(session.getTitle()) && StringUtils.hasText(firstMessage)) {
            String title = firstMessage.trim();
            session.setTitle(title.length() > 20 ? title.substring(0, 20) : title);
        }
        sessionRepository.save(session);
    }

    private void analyzeSession(ChatSession session) {
        Pageable pageable = PageRequest.of(0, 20);
        List<ChatMessage> recent = messageRepository.findBySessionIdOrderByIdAsc(session.getId(), pageable).getContent();
        StringBuilder sb = new StringBuilder();
        for (ChatMessage m : recent) {
            if ("USER".equals(m.getSenderType())) {
                sb.append("用户：").append(m.getContent()).append("\n");
            } else {
                sb.append("助手：").append(m.getContent()).append("\n");
            }
        }
        AiAdapter.EmotionAnalysisResult result = aiAdapter.analyzeEmotion(sb.toString());

        for (EmotionAnalysis old : analysisRepository.findBySourceTypeAndSourceIdAndCurrentResultTrue("CHAT_SESSION", session.getId())) {
            old.setCurrentResult(false);
            analysisRepository.save(old);
        }
        EmotionAnalysis analysis = new EmotionAnalysis();
        analysis.setSourceType("CHAT_SESSION");
        analysis.setSourceId(session.getId());
        analysis.setEmotionScore(result.emotionScore());
        analysis.setLabel(result.label());
        analysis.setIsNegative(Boolean.TRUE.equals(result.isNegative()));
        analysis.setRiskLevel(result.riskLevel());
        analysis.setRiskDescription(result.riskDescription());
        analysis.setSuggestion(result.suggestion());
        analysis.setImprovementSuggestionsJson(toJson(result.improvements()));
        analysis.setModelProvider(aiAdapter.providerName());
        analysis.setModelVersion(aiAdapter.modelVersion());
        analysis.setPolicyVersion(policyVersionOf());
        analysis.setStatus("SUCCEEDED");
        analysis.setCurrentResult(true);
        analysis.setAnalyzedAt(LocalDateTime.now());
        analysisRepository.save(analysis);
    }

    private void createRiskEvent(ChatSession session, ChatMessage userMsg, AiAdapter.ChatReply reply) {
        RiskEvent event = new RiskEvent();
        event.setUserId(session.getUserId());
        event.setSourceType("CHAT_MESSAGE");
        event.setSourceId(userMsg.getId());
        event.setRiskLevel(reply.riskLevel());
        event.setTriggerCodesJson(toJson(reply.triggerCodes()));
        event.setRedactedSummary(userMsg.getContent().length() > 100 ? userMsg.getContent().substring(0, 100) : userMsg.getContent());
        event.setPolicyVersion(policyVersionOf());
        event.setModelVersion(aiAdapter.modelVersion());
        event.setStatus("OPEN");
        riskEventRepository.save(event);
    }

    private void sendChunks(SseEmitter emitter, AiAdapter.ChatReply reply, ChatMessage assistantMsg) throws Exception {
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
        if (!"NORMAL".equals(reply.riskLevel())) {
            Map<String, Object> riskData = new java.util.LinkedHashMap<>();
            riskData.put("level", reply.riskLevel());
            riskData.put("guidanceCode", reply.guidanceCode());
            emitter.send(SseEmitter.event().name("risk").data(riskData));
        }
        emitter.send(SseEmitter.event().name("done").data(Map.of("messageId", assistantMsg.getId(), "finishReason", "STOP")));
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
                a.getAnalyzedAt(), a.getModelVersion());
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
