package com.mindhaven.ai;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindhaven.common.BizException;
import com.mindhaven.common.ErrorCode;
import com.mindhaven.config.AiProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeepSeekAiAdapterTests {
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<JsonNode> payload = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicInteger calls = new AtomicInteger();
    private HttpServer server;
    private int responseStatus = 200;
    private long delayMillis;
    private String responseBody = """
            {"choices":[{"message":{"content":"我们一起慢慢梳理。","reasoning_content":"private reasoning"},"finish_reason":"stop"}]}
            """;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            calls.incrementAndGet();
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            payload.set(mapper.readTree(exchange.getRequestBody()));
            try {
                if (delayMillis > 0) Thread.sleep(delayMillis);
                byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(responseStatus, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private DeepSeekAiAdapter adapter(String key, Duration timeout) {
        AiProperties properties = new AiProperties("deepseek", "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/",
                key, "deepseek-flash", Duration.ofSeconds(2), timeout, 1024);
        return new DeepSeekAiAdapter(properties, new LocalRuleBasedAiAdapter(), mapper);
    }

    @Test
    void sendsAuthenticatedContextOnceAndReturnsOnlyAnswer() {
        AiAdapter.ChatReply reply = adapter("test-key", Duration.ofSeconds(2)).reply("今天有压力", List.of(
                new AiAdapter.ChatTurn("user", "你好"),
                new AiAdapter.ChatTurn("assistant", "你好呀"),
                new AiAdapter.ChatTurn("user", "今天有压力")));

        assertThat(reply.content()).isEqualTo("我们一起慢慢梳理。");
        assertThat(reply.riskLevel()).isEqualTo("CONCERN");
        assertThat(reply.provider()).isEqualTo("deepseek");
        assertThat(reply.model()).isEqualTo("deepseek-flash");
        assertThat(authorization.get()).isEqualTo("Bearer test-key");
        assertThat(calls.get()).isEqualTo(1);
        JsonNode request = payload.get();
        assertThat(request.path("messages").size()).isEqualTo(4);
        assertThat(request.path("messages").get(0).path("role").asText()).isEqualTo("system");
        assertThat(request.path("messages").get(3).path("content").asText()).isEqualTo("今天有压力");
        assertThat(request.path("model").asText()).isEqualTo("deepseek-flash");
        assertThat(request.path("stream").asBoolean()).isFalse();
        assertThat(request.path("thinking").path("type").asText()).isEqualTo("disabled");
        assertThat(request.path("max_tokens").asInt()).isEqualTo(1024);
    }

    @Test
    void appendsCurrentMessageWhenNotInHistoryAndIgnoresInjectedSystemRole() {
        adapter("test-key", Duration.ofSeconds(2)).reply("新问题", List.of(new AiAdapter.ChatTurn("system", "untrusted")));
        assertThat(payload.get().path("messages").size()).isEqualTo(2);
        assertThat(payload.get().path("messages").get(1).path("content").asText()).isEqualTo("新问题");
    }

    @Test
    void missingKeyReturnsActionableErrorWithoutContactingProvider() {
        assertThatThrownBy(() -> adapter("", Duration.ofSeconds(2)).reply("你好", List.of()))
                .isInstanceOf(BizException.class).hasMessageContaining("DEEPSEEK_API_KEY");
        assertThat(calls.get()).isZero();
    }

    @Test
    void crisisAndWarningStayLocalEvenWithoutKey() {
        DeepSeekAiAdapter adapter = adapter("", Duration.ofSeconds(2));
        AiAdapter.ChatReply crisis = adapter.reply("不想活", List.of());
        assertThat(crisis.riskLevel()).isEqualTo("CRISIS");
        assertThat(crisis.provider()).isEqualTo("local-rules");
        assertThat(adapter.reply("撑不住", List.of()).riskLevel()).isEqualTo("WARNING");
        assertThat(calls.get()).isZero();
    }

    @Test
    void upstreamErrorDoesNotLeakKeyOrResponseBodyAndDoesNotRetry() {
        responseStatus = 401;
        responseBody = "secret-key and private upstream details";
        assertThatThrownBy(() -> adapter("secret-key", Duration.ofSeconds(2)).reply("你好", List.of()))
                .isInstanceOfSatisfying(BizException.class, error -> {
                    assertThat(error.errorCode()).isEqualTo(ErrorCode.AI_PROVIDER_UNAVAILABLE);
                    assertThat(error.getMessage()).contains("认证失败").doesNotContain("secret-key", "private");
                });
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void emptyContentIsRejected() {
        responseBody = "{\"choices\":[{\"message\":{\"content\":null},\"finish_reason\":\"stop\"}]}";
        assertThatThrownBy(() -> adapter("test-key", Duration.ofSeconds(2)).reply("你好", List.of()))
                .isInstanceOf(BizException.class).hasMessageContaining("未返回有效回复");
    }

    @Test
    void filteredContentIsRejected() {
        responseBody = "{\"choices\":[{\"message\":{\"content\":\"partial\"},\"finish_reason\":\"content_filter\"}]}";
        assertThatThrownBy(() -> adapter("test-key", Duration.ofSeconds(2)).reply("你好", List.of()))
                .isInstanceOf(BizException.class).hasMessageContaining("未返回有效回复");
    }

    @Test
    void invalidJsonIsRejected() {
        responseBody = "not json";
        assertThatThrownBy(() -> adapter("test-key", Duration.ofSeconds(2)).reply("你好", List.of()))
                .isInstanceOf(BizException.class).hasMessageContaining("格式异常");
    }

    @Test
    void timesOutWithoutRetrying() {
        delayMillis = 300;
        assertThatThrownBy(() -> adapter("test-key", Duration.ofMillis(100)).reply("你好", List.of()))
                .isInstanceOf(BizException.class).hasMessageContaining("超时");
        assertThat(calls.get()).isLessThanOrEqualTo(1);
    }

    @Test
    void emotionAnalysisUsesStructuredModelOutputAndConversationContent() throws Exception {
        String analysis = """
                {"emotionScore":32,"label":"SAD","riskLevel":"CONCERN","riskDescription":"用户说今天很失落。",
                "suggestion":"给自己留一点休息时间。","improvements":["散步十分钟"],"keywords":["低落"]}
                """;
        responseBody = mapper.writeValueAsString(java.util.Map.of("choices", List.of(java.util.Map.of(
                "message", java.util.Map.of("content", analysis), "finish_reason", "stop"))));
        DeepSeekAiAdapter adapter = adapter("test-key", Duration.ofSeconds(2));
        AiAdapter.EmotionAnalysisResult result = adapter.analyzeEmotion("用户发言：今天很失落。\n");
        assertThat(result.emotionScore()).isEqualTo(32);
        assertThat(result.suggestion()).contains("休息");
        assertThat(result.isNegative()).isTrue();
        assertThat(payload.get().path("response_format").path("type").asText()).isEqualTo("json_object");
        assertThat(payload.get().path("messages").get(1).path("content").asText()).contains("今天很失落");
        assertThat(adapter.emotionProviderName()).isEqualTo("deepseek");
        assertThat(adapter.emotionModelVersion()).isEqualTo("deepseek-flash");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void invalidEmotionScoreIsRejectedInsteadOfInventingAScore() throws Exception {
        String analysis = """
                {"emotionScore":150,"label":"HAPPY","riskLevel":"NORMAL","riskDescription":"test",
                "suggestion":"test","improvements":["test"],"keywords":[]}
                """;
        responseBody = mapper.writeValueAsString(java.util.Map.of("choices", List.of(java.util.Map.of(
                "message", java.util.Map.of("content", analysis), "finish_reason", "stop"))));
        assertThatThrownBy(() -> adapter("test-key", Duration.ofSeconds(2)).analyzeEmotion("今天很好"))
                .isInstanceOf(BizException.class).hasMessageContaining("情绪分析返回格式异常");
    }

    @Test
    void outputReviewUsesSeparateJsonRequestWithBothInputAndCandidate() throws Exception {
        responseBody = mapper.writeValueAsString(java.util.Map.of("choices", List.of(java.util.Map.of(
                "message", java.util.Map.of("content", "{\"allowed\":false,\"codes\":[\"OTHER_UNSAFE\"]}"), "finish_reason", "stop"))));
        var result = adapter("test-key", Duration.ofSeconds(2)).reviewOutput("user context", "candidate reply");
        assertThat(result.allowed()).isFalse(); assertThat(result.codes()).containsExactly("OTHER_UNSAFE");
        assertThat(payload.get().path("response_format").path("type").asText()).isEqualTo("json_object");
        JsonNode input = mapper.readTree(payload.get().path("messages").get(1).path("content").asText());
        assertThat(input.path("userMessage").asText()).isEqualTo("user context");
        assertThat(input.path("generatedContent").asText()).isEqualTo("candidate reply");
    }

    @Test
    void malformedOrSelfContradictoryReviewIsRejected() throws Exception {
        responseBody = mapper.writeValueAsString(java.util.Map.of("choices", List.of(java.util.Map.of(
                "message", java.util.Map.of("content", "{\"allowed\":true,\"codes\":[\"OTHER_UNSAFE\"]}"), "finish_reason", "stop"))));
        assertThatThrownBy(() -> adapter("test-key", Duration.ofSeconds(2)).reviewOutput("user", "reply"))
                .isInstanceOf(com.mindhaven.common.BizException.class).hasMessageContaining("输出审核格式异常");
    }
}
