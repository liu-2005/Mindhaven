package com.mindhaven.dto;

import java.time.LocalDateTime;
import java.util.List;

public record EmotionAnalysisDto(Integer emotionScore, String label, Boolean isNegative, String riskLevel,
                                 String riskDescription, String suggestion, List<String> improvementSuggestions,
                                 List<String> keywords, LocalDateTime analyzedAt, String modelVersion) {
}
