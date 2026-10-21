package com.mindhaven.service;

import java.util.List;
import com.mindhaven.ai.AiAdapter;
import com.mindhaven.common.BizException;
import com.mindhaven.common.ErrorCode;
import com.mindhaven.dto.ChatStreamRequest;
import com.mindhaven.entity.*;
import com.mindhaven.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:chat_atomic;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false", "app.seed-demo-data=false",
        "app.ai.provider=local", "app.chat.recovery.enabled=false", "spring.jpa.properties.hibernate.format_sql=false"
})
class ChatEmotionTests {
    @Autowired ChatService chat;
    @Autowired ChatTurnStore turns;
    @Autowired ChatSessionRepository sessions;
    @Autowired ChatMessageRepository messages;
    @Autowired EmotionAnalysisRepository analyses;
    @Autowired RiskEventRepository risks;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean(name = "localRuleBasedAiAdapter") AiAdapter ai;
    private Long userId;
    private Long sessionId;

    @BeforeEach
    void setup() {
        jdbc.execute("create table if not exists mh_chat_turn (id bigint auto_increment primary key, user_id bigint not null, session_id bigint not null, "
                + "client_request_id varchar(64) not null, message_hash varchar(64) not null, status varchar(20) not null, owner_token varchar(36), "
                + "lease_until timestamp, result_json clob, assistant_message_id bigint, unique(session_id,client_request_id))");
        jdbc.execute("alter table mh_risk_event drop constraint if exists injected_risk_failure");
        jdbc.execute("alter table mh_chat_session drop constraint if exists injected_counter_failure");
        jdbc.update("delete from mh_chat_turn"); risks.deleteAll(); analyses.deleteAll(); messages.deleteAll(); sessions.deleteAll(); users.deleteAll();
        User user = new User(); user.setUsername("fixture"); user.setStatus("ACTIVE"); userId = users.saveAndFlush(user).getId();
        ChatSession session = new ChatSession(); session.setUserId(userId); session.setTitle("Test"); session.setStatus("ACTIVE"); session.setMessageCount(0);
        sessionId = sessions.saveAndFlush(session).getId();
        when(ai.reply(anyString(), any())).thenReturn(new AiAdapter.ChatReply("我们可以一起梳理今天的感受。", "NORMAL", List.of(), null, "mock", "mock-v1"));
        when(ai.analyzeEmotion(anyString())).thenReturn(emotion("NORMAL"));
        when(ai.reviewOutput(anyString(), anyString())).thenReturn(new AiAdapter.OutputReview(true, List.of()));
        when(ai.emotionProviderName()).thenReturn("mock"); when(ai.emotionModelVersion()).thenReturn("mock-v1");
    }

    private AiAdapter.EmotionAnalysisResult emotion(String risk) {
        return new AiAdapter.EmotionAnalysisResult("CRISIS".equals(risk) ? 10 : 63, "CALM", false, risk,
                "用户表达平稳。", "记录一件小事。", List.of("散步十分钟"), List.of());
    }
    private ChatStreamRequest request() { return new ChatStreamRequest(sessionId, "request", "今天想聊聊近况"); }
    private SseEmitter send() { var emitter = mock(SseEmitter.class); chat.stream(userId, request(), emitter); return emitter; }
    private long assistantCount() { return messages.findBySessionIdOrderByIdAsc(sessionId, org.springframework.data.domain.Pageable.unpaged()).stream().filter(m -> "ASSISTANT".equals(m.getSenderType())).count(); }
    private String turnStatus() { return jdbc.queryForObject("select status from mh_chat_turn where session_id=?", String.class, sessionId); }
    private String assistantText() { return messages.findBySessionIdAndClientRequestIdAndSenderType(sessionId, "request", "ASSISTANT").orElseThrow().getContent(); }

    @Test
    void onlyUserStatementsAreScoredAndEventsAreSentAfterCommit() throws Exception {
        ChatMessage earlier = new ChatMessage(); earlier.setSessionId(sessionId); earlier.setSenderType("ASSISTANT"); earlier.setContent("assistant-not-user-evidence"); earlier.setStatus("COMPLETED"); messages.saveAndFlush(earlier);
        var emitter = mock(SseEmitter.class);
        doAnswer(call -> { assertThat(turnStatus()).isEqualTo("COMPLETED"); return null; }).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        chat.stream(userId, request(), emitter);
        var input = ArgumentCaptor.forClass(String.class); verify(ai).analyzeEmotion(input.capture());
        assertThat(input.getValue()).contains(request().message()).doesNotContain("assistant-not-user-evidence");
        assertThat(chat.emotionOf(userId, sessionId).emotionScore()).isEqualTo(63);
        assertThat(sessions.findById(sessionId).orElseThrow().getMessageCount()).isEqualTo(3);
        verify(emitter).complete();
    }

    @Test
    void analysisFailureKeepsChatButDoesNotReuseOldScore() {
        EmotionAnalysis old = new EmotionAnalysis(); old.setSourceType("CHAT_SESSION"); old.setSourceId(sessionId); old.setCurrentResult(true); old.setEmotionScore(80); old.setRiskLevel("NORMAL"); old.setStatus("SUCCEEDED"); analyses.saveAndFlush(old);
        when(ai.analyzeEmotion(anyString())).thenThrow(new BizException(ErrorCode.AI_PROVIDER_UNAVAILABLE));
        send();
        assertThat(chat.emotionOf(userId, sessionId).analysisStatus()).isEqualTo("FAILED");
        assertThat(chat.emotionOf(userId, sessionId).emotionScore()).isNull(); assertThat(assistantCount()).isEqualTo(1);
        assertThat(analyses.findById(old.getId()).orElseThrow().getCurrentResult()).isFalse();
    }

    @Test
    void completedRequestReplaysWithoutAnyModelCallsOrDuplicateRecords() {
        send(); clearInvocations(ai); send();
        verifyNoInteractions(ai); assertThat(assistantCount()).isEqualTo(1); assertThat(analyses.count()).isEqualTo(1);
        assertThat(sessions.findById(sessionId).orElseThrow().getMessageCount()).isEqualTo(2);
    }

    @Test
    void unsafeOutputIsNotStoredOrStreamedEvenWhenUserEmotionIsNormal() throws Exception {
        String unsafe = "你确诊患有抑郁症，我是你的心理医生。";
        when(ai.reply(anyString(), any())).thenReturn(new AiAdapter.ChatReply(unsafe, "NORMAL", List.of(), null));
        var emitter = send();
        assertThat(assistantText()).doesNotContain("确诊", "心理医生");
        assertThat(jdbc.queryForObject("select result_json from mh_chat_turn", String.class)).doesNotContain(unsafe);
        assertThat(chat.emotionOf(userId, sessionId).riskLevel()).isEqualTo("NORMAL");
        assertThat(risks.findAll()).hasSize(1).allSatisfy(event -> assertThat(event.getTriggerCodesJson()).contains("MEDICAL_DIAGNOSIS"));
        var events = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class); verify(emitter, atLeastOnce()).send(events.capture());
        String emitted = events.getAllValues().stream().flatMap(event -> event.build().stream()).map(part -> String.valueOf(part.getData())).collect(java.util.stream.Collectors.joining());
        assertThat(emitted).doesNotContain(unsafe).contains("OUTPUT_BLOCKED");
        verify(ai, never()).reviewOutput(anyString(), anyString());
    }

    @Test
    void independentSemanticReviewRejectsUnsafeSuggestion() {
        when(ai.reviewOutput(anyString(), anyString())).thenReturn(new AiAdapter.OutputReview(false, List.of("OTHER_UNSAFE")));
        send(); assertThat(assistantText()).contains("暂时无法提供可靠"); assertThat(risks.count()).isEqualTo(1);
        assertThat(chat.emotionOf(userId, sessionId).emotionScore()).isNull();
    }

    @Test
    void reviewOutageFailsClosedWithoutLeakingProviderError() {
        when(ai.reviewOutput(anyString(), anyString())).thenThrow(new BizException(ErrorCode.AI_PROVIDER_UNAVAILABLE, "private-provider-details"));
        send(); assertThat(assistantText()).contains("暂时无法提供可靠").doesNotContain("private-provider-details");
        assertThat(risks.findAll().getFirst().getTriggerCodesJson()).contains("OUTPUT_REVIEW_UNAVAILABLE");
    }

    @Test
    void modelCrisisOverridesReplyAndCreatesRiskRecord() {
        when(ai.analyzeEmotion(anyString())).thenReturn(emotion("CRISIS")); send();
        assertThat(assistantText()).doesNotContain("一起梳理今天"); assertThat(risks.findAll().getFirst().getRiskLevel()).isEqualTo("CRISIS");
        verify(ai, never()).reviewOutput(anyString(), anyString());
    }

    @Test
    void riskWriteFailureRollsBackAllBusinessWritesAndRetryUsesCheckpoint() {
        when(ai.analyzeEmotion(anyString())).thenReturn(emotion("CRISIS"));
        jdbc.execute("alter table mh_risk_event add constraint injected_risk_failure check (source_id < 0)");
        send();
        assertThat(turnStatus()).isEqualTo("PREPARED"); assertThat(assistantCount()).isZero(); assertThat(analyses.count()).isZero(); assertThat(risks.count()).isZero();
        assertThat(sessions.findById(sessionId).orElseThrow().getMessageCount()).isZero();
        jdbc.execute("alter table mh_risk_event drop constraint injected_risk_failure"); clearInvocations(ai);
        send(); verifyNoInteractions(ai); assertThat(turnStatus()).isEqualTo("COMPLETED"); assertThat(assistantCount()).isEqualTo(1); assertThat(risks.count()).isEqualTo(1);
        send(); assertThat(risks.count()).isEqualTo(1);
    }

    @Test
    void counterFailureCanBeRecoveredWithoutAnotherModelCall() {
        jdbc.execute("alter table mh_chat_session add constraint injected_counter_failure check (message_count < 2)");
        send(); assertThat(turnStatus()).isEqualTo("PREPARED"); assertThat(assistantCount()).isZero(); assertThat(analyses.count()).isZero();
        jdbc.execute("alter table mh_chat_session drop constraint injected_counter_failure"); clearInvocations(ai);
        for (var claim : turns.pending()) chat.recoverPrepared(claim);
        verifyNoInteractions(ai); assertThat(turnStatus()).isEqualTo("COMPLETED"); assertThat(assistantCount()).isEqualTo(1);
        assertThat(sessions.findById(sessionId).orElseThrow().getMessageCount()).isEqualTo(2);
    }

    @Test
    void legacyPartialReplyIsReviewedAndMissingRiskIsRepaired() {
        ChatMessage user = new ChatMessage(); user.setSessionId(sessionId); user.setSenderType("USER"); user.setContent(request().message()); user.setClientRequestId("request"); user.setStatus("COMPLETED"); messages.saveAndFlush(user);
        ChatMessage legacy = new ChatMessage(); legacy.setSessionId(sessionId); legacy.setSenderType("ASSISTANT"); legacy.setContent("legacy reply"); legacy.setClientRequestId("request"); legacy.setStatus("COMPLETED"); messages.saveAndFlush(legacy);
        when(ai.analyzeEmotion(anyString())).thenReturn(emotion("CRISIS")); send();
        verify(ai, never()).reply(anyString(), any()); assertThat(assistantCount()).isEqualTo(1); assertThat(risks.count()).isEqualTo(1);
        assertThat(assistantText()).isNotEqualTo("legacy reply");
    }

    @Test
    void claimsAreDurableAndRejectParallelOrChangedRequests() {
        turns.claim(userId, request());
        assertThatThrownBy(() -> turns.claim(userId, request())).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> turns.claim(userId, new ChatStreamRequest(sessionId, "other", "other message"))).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> turns.claim(userId, new ChatStreamRequest(sessionId, "request", "changed message"))).isInstanceOf(BizException.class);
    }

    @Test
    void deletingSessionAlsoDeletesDurableSnapshots() {
        send(); chat.deleteSession(userId, sessionId);
        assertThat(jdbc.queryForObject("select count(*) from mh_chat_turn", Long.class)).isZero();
        assertThat(messages.countBySessionId(sessionId)).isZero(); assertThat(analyses.count()).isZero();
    }
}
