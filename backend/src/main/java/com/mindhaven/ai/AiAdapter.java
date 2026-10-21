package com.mindhaven.ai;

import java.util.List;

public interface AiAdapter {

    /**
     * 生成会话回复；客户端 SSE 切片由调用方完成。
     */
    ChatReply reply(String userMessage, List<ChatTurn> history);

    /**
     * 对一段文本做情绪分析。
     */
    EmotionAnalysisResult analyzeEmotion(String text);

    default OutputReview reviewOutput(String userMessage, String generatedContent) {
        return new OutputSafetyRules().review(generatedContent);
    }

    record OutputReview(boolean allowed, List<String> codes) {}

    String providerName();

    String modelVersion();

    default String emotionProviderName() {
        return providerName();
    }

    default String emotionModelVersion() {
        return modelVersion();
    }

    record ChatTurn(String role, String content) {
    }

    record ChatReply(String content, String riskLevel, List<String> triggerCodes, String guidanceCode,
                     String provider, String model) {
        public ChatReply(String content, String riskLevel, List<String> triggerCodes, String guidanceCode) {
            this(content, riskLevel, triggerCodes, guidanceCode, null, null);
        }
    }

    record EmotionAnalysisResult(Integer emotionScore, String label, Boolean isNegative, String riskLevel,
                                 String riskDescription, String suggestion, List<String> improvements,
                                 List<String> keywords) {
    }
}
