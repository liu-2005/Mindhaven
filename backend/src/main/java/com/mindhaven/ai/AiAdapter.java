package com.mindhaven.ai;

import java.util.List;

public interface AiAdapter {

    /**
     * 生成会话回复（本地规则式，非流式接口本身；流式切片由调用方完成）。
     */
    ChatReply reply(String userMessage, List<ChatTurn> history);

    /**
     * 对一段文本做情绪分析。
     */
    EmotionAnalysisResult analyzeEmotion(String text);

    String providerName();

    String modelVersion();

    record ChatTurn(String role, String content) {
    }

    record ChatReply(String content, String riskLevel, List<String> triggerCodes, String guidanceCode) {
    }

    record EmotionAnalysisResult(Integer emotionScore, String label, Boolean isNegative, String riskLevel,
                                 String riskDescription, String suggestion, List<String> improvements,
                                 List<String> keywords) {
    }
}
