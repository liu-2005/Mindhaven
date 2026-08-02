package com.mindhaven.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record DiaryRequest(
        @NotNull LocalDate diaryDate,
        @NotNull Integer moodScore,
        @NotNull String dominantEmotion,
        String emotionTriggers,
        @NotNull String content,
        Integer sleepQuality,
        Integer stressLevel) {
}
