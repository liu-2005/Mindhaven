package com.mindhaven.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.mindhaven.entity.EmotionDiary;

public record DiaryDto(Long id, Long userId, LocalDate diaryDate, Integer moodScore, String dominantEmotion,
                       String emotionTriggers, String content, Integer sleepQuality, Integer stressLevel,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static DiaryDto of(EmotionDiary d) {
        return new DiaryDto(d.getId(), d.getUserId(), d.getDiaryDate(), d.getMoodScore(), d.getDominantEmotion(),
                d.getEmotionTriggers(), d.getContent(), d.getSleepQuality(), d.getStressLevel(),
                d.getCreatedAt(), d.getUpdatedAt());
    }
}
