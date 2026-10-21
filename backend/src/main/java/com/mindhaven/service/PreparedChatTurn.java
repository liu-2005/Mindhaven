package com.mindhaven.service;

import java.util.List;
import com.mindhaven.ai.AiAdapter;

/** Durable checkpoint containing only content that may be shown to the user. */
public record PreparedChatTurn(Long userMessageId, AiAdapter.ChatReply reply, AiAdapter.EmotionAnalysisResult emotion,
                               String emotionProvider, String emotionModel, boolean outputBlocked,
                               List<String> outputCodes, String policyVersion) {}
