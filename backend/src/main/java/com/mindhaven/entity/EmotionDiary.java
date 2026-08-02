package com.mindhaven.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "mh_emotion_diary")
public class EmotionDiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "diary_date")
    private java.time.LocalDate diaryDate;

    @Column(name = "mood_score")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer moodScore;

    @Column(name = "dominant_emotion")
    private String dominantEmotion;

    @Column(name = "emotion_triggers")
    private String emotionTriggers;

    private String content;

    @Column(name = "sleep_quality")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer sleepQuality;

    @Column(name = "stress_level")
    @JdbcTypeCode(SqlTypes.TINYINT)
    private Integer stressLevel;

    private Integer version;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public java.time.LocalDate getDiaryDate() {
        return diaryDate;
    }

    public void setDiaryDate(java.time.LocalDate diaryDate) {
        this.diaryDate = diaryDate;
    }

    public Integer getMoodScore() {
        return moodScore;
    }

    public void setMoodScore(Integer moodScore) {
        this.moodScore = moodScore;
    }

    public String getDominantEmotion() {
        return dominantEmotion;
    }

    public void setDominantEmotion(String dominantEmotion) {
        this.dominantEmotion = dominantEmotion;
    }

    public String getEmotionTriggers() {
        return emotionTriggers;
    }

    public void setEmotionTriggers(String emotionTriggers) {
        this.emotionTriggers = emotionTriggers;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSleepQuality() {
        return sleepQuality;
    }

    public void setSleepQuality(Integer sleepQuality) {
        this.sleepQuality = sleepQuality;
    }

    public Integer getStressLevel() {
        return stressLevel;
    }

    public void setStressLevel(Integer stressLevel) {
        this.stressLevel = stressLevel;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
