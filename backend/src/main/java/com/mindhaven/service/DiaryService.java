package com.mindhaven.service;

import java.time.LocalDate;
import java.util.List;

import com.mindhaven.common.BizException;
import com.mindhaven.common.ErrorCode;
import com.mindhaven.dto.DiaryRequest;
import com.mindhaven.entity.EmotionDiary;
import com.mindhaven.repository.EmotionDiaryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DiaryService {

    private final EmotionDiaryRepository diaryRepository;
    private final AuditService auditService;

    public DiaryService(EmotionDiaryRepository diaryRepository, AuditService auditService) {
        this.diaryRepository = diaryRepository;
        this.auditService = auditService;
    }

    private void validate(DiaryRequest request) {
        if (request.moodScore() == null || request.moodScore() < 1 || request.moodScore() > 10) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "心情评分必须在 1-10 之间");
        }
        if (request.sleepQuality() != null && (request.sleepQuality() < 1 || request.sleepQuality() > 5)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "睡眠质量必须在 1-5 之间");
        }
        if (request.stressLevel() != null && (request.stressLevel() < 1 || request.stressLevel() > 5)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "压力水平必须在 1-5 之间");
        }
        if (request.dominantEmotion() == null || request.dominantEmotion().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "请选择主要情绪");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "请写下今日感想");
        }
    }

    @Transactional
    public EmotionDiary create(Long userId, DiaryRequest request) {
        validate(request);
        if (diaryRepository.existsByUserIdAndDiaryDateAndDeletedAtIsNull(userId, request.diaryDate())) {
            throw new BizException(ErrorCode.RESOURCE_CONFLICT, "该日期已有一篇日记");
        }
        EmotionDiary diary = new EmotionDiary();
        diary.setUserId(userId);
        apply(diary, request);
        diary.setVersion(0);
        return diaryRepository.save(diary);
    }

    public Page<EmotionDiary> listMine(Long userId, int page, int size, LocalDate fromDate, LocalDate toDate) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<EmotionDiary> all = diaryRepository.findUserDiaries(userId, fromDate, toDate);
        int total = all.size();
        int start = Math.min((Math.max(page, 1) - 1) * safeSize, total);
        int end = Math.min(start + safeSize, total);
        List<EmotionDiary> sub = start >= total ? List.of() : all.subList(start, end);
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, safeSize);
        return new org.springframework.data.domain.PageImpl<>(sub, pageable, total);
    }

    public EmotionDiary getMine(Long userId, Long diaryId) {
        return diaryRepository.findByIdAndUserIdAndDeletedAtIsNull(diaryId, userId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Transactional
    public EmotionDiary update(Long userId, Long diaryId, DiaryRequest request) {
        validate(request);
        EmotionDiary diary = getMine(userId, diaryId);
        apply(diary, request);
        diary.setVersion((diary.getVersion() == null ? 0 : diary.getVersion()) + 1);
        return diaryRepository.save(diary);
    }

    @Transactional
    public void deleteMine(Long userId, Long diaryId) {
        EmotionDiary diary = getMine(userId, diaryId);
        diary.setDeletedAt(java.time.LocalDateTime.now());
        diaryRepository.save(diary);
    }

    private void apply(EmotionDiary diary, DiaryRequest request) {
        diary.setDiaryDate(request.diaryDate());
        diary.setMoodScore(request.moodScore());
        diary.setDominantEmotion(request.dominantEmotion());
        diary.setEmotionTriggers(request.emotionTriggers());
        diary.setContent(request.content());
        diary.setSleepQuality(request.sleepQuality() == null ? 1 : request.sleepQuality());
        diary.setStressLevel(request.stressLevel() == null ? 1 : request.stressLevel());
    }

    public Page<EmotionDiary> adminPage(Long userId, Integer minScore, Integer maxScore, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, safeSize);
        return diaryRepository.adminPage(userId, minScore, maxScore, pageable);
    }

    @Transactional
    public void adminDelete(Long operatorId, Long diaryId) {
        EmotionDiary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND));
        diary.setDeletedAt(java.time.LocalDateTime.now());
        diaryRepository.save(diary);
        auditService.record(operatorId, "diary:delete", "EMOTION_DIARY", String.valueOf(diaryId), "SUCCESS",
                "管理员删除情绪日记", null);
    }

    public List<EmotionDiary> listAllByUserId(Long userId, LocalDate fromDate, LocalDate toDate) {
        return diaryRepository.findUserDiaries(userId, fromDate, toDate);
    }
}
