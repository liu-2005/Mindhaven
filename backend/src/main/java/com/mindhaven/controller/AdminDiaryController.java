package com.mindhaven.controller;

import com.mindhaven.common.ApiResponse;
import com.mindhaven.common.PageResult;
import com.mindhaven.dto.DiaryDto;
import com.mindhaven.entity.EmotionDiary;
import com.mindhaven.security.AuthContextHolder;
import com.mindhaven.security.RequirePermission;
import com.mindhaven.service.DiaryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/diaries")
public class AdminDiaryController {

    private final DiaryService diaryService;

    public AdminDiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    @GetMapping
    @RequirePermission("risk:read-sensitive")
    public ApiResponse<PageResult<DiaryDto>> page(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(required = false) Long userId,
                                                  @RequestParam(required = false) Integer minMoodScore,
                                                  @RequestParam(required = false) Integer maxMoodScore) {
        var result = diaryService.adminPage(userId, minMoodScore, maxMoodScore, page, size);
        var items = result.getContent().stream().map(DiaryDto::of).toList();
        return ApiResponse.success(PageResult.of(items, page, size, result.getTotalElements()));
    }

    @DeleteMapping("/{diaryId}")
    @RequirePermission("risk:resolve")
    public ApiResponse<Void> delete(@PathVariable Long diaryId) {
        diaryService.adminDelete(AuthContextHolder.userId(), diaryId);
        return ApiResponse.success(null);
    }
}
