package com.mindhaven.controller;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mindhaven.common.ApiResponse;
import com.mindhaven.common.PageResult;
import com.mindhaven.dto.DiaryDto;
import com.mindhaven.dto.DiaryRequest;
import com.mindhaven.entity.EmotionDiary;
import com.mindhaven.security.AuthContextHolder;
import com.mindhaven.service.DiaryService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/diaries")
public class DiaryController {

    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    @PostMapping
    public ApiResponse<DiaryDto> create(@Valid @RequestBody DiaryRequest request) {
        return ApiResponse.success(DiaryDto.of(diaryService.create(AuthContextHolder.userId(), request)));
    }

    @GetMapping
    public ApiResponse<PageResult<DiaryDto>> list(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        var result = diaryService.listMine(AuthContextHolder.userId(), page, size, fromDate, toDate);
        var items = result.getContent().stream().map(DiaryDto::of).toList();
        return ApiResponse.success(PageResult.of(items, page, size, result.getTotalElements()));
    }

    @GetMapping("/trends")
    public ApiResponse<Map<String, Object>> trends(@RequestParam(defaultValue = "30") int days) {
        Long userId = AuthContextHolder.userId();
        int n = Math.min(Math.max(days, 1), 365);
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(n - 1L);
        List<EmotionDiary> diaries = diaryService.listAllByUserId(userId, from, to);
        Map<LocalDate, List<Integer>> scoresByDay = new LinkedHashMap<>();
        Map<String, Integer> emotionCount = new LinkedHashMap<>();
        for (EmotionDiary d : diaries) {
            scoresByDay.computeIfAbsent(d.getDiaryDate(), k -> new java.util.ArrayList<>()).add(d.getMoodScore());
            emotionCount.merge(d.getDominantEmotion(), 1, Integer::sum);
        }
        var daily = new java.util.ArrayList<Map<String, Object>>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            List<Integer> scores = scoresByDay.getOrDefault(date, List.of());
            double avg = scores.isEmpty() ? 0.0 : scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            daily.add(Map.of("date", date.toString(), "avgScore", Math.round(avg * 10) / 10.0, "recordCount", scores.size()));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("periodDays", n);
        data.put("totalRecords", diaries.size());
        data.put("averageScore", diaries.isEmpty() ? 0.0
                : Math.round(diaries.stream().mapToInt(EmotionDiary::getMoodScore).average().orElse(0) * 10) / 10.0);
        data.put("dominantEmotions", emotionCount);
        data.put("daily", daily);
        return ApiResponse.success(data);
    }

    @GetMapping("/{diaryId}")
    public ApiResponse<DiaryDto> get(@PathVariable Long diaryId) {
        return ApiResponse.success(DiaryDto.of(diaryService.getMine(AuthContextHolder.userId(), diaryId)));
    }

    @PutMapping("/{diaryId}")
    public ApiResponse<DiaryDto> update(@PathVariable Long diaryId, @Valid @RequestBody DiaryRequest request) {
        return ApiResponse.success(DiaryDto.of(diaryService.update(AuthContextHolder.userId(), diaryId, request)));
    }

    @DeleteMapping("/{diaryId}")
    public ApiResponse<Void> delete(@PathVariable Long diaryId) {
        diaryService.deleteMine(AuthContextHolder.userId(), diaryId);
        return ApiResponse.success(null);
    }
}
