-- V3: 情绪日记主情绪列支持中文（前端使用中文情绪名，如 平静/开心/焦虑）
ALTER TABLE mh_emotion_diary
    MODIFY COLUMN dominant_emotion VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL;

-- 情绪分析标签列同步放宽，便于后续支持本地化标签
ALTER TABLE mh_emotion_analysis
    MODIFY COLUMN label VARCHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL;
