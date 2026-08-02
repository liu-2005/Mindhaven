package com.mindhaven.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.mindhaven.common.ApiResponse;
import com.mindhaven.security.RequirePermission;
import com.mindhaven.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/analytics")
public class AdminAnalyticsController {

    private final AnalyticsService analyticsService;

    public AdminAnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    @RequirePermission("analytics:read")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.success(analyticsService.overview());
    }

    @GetMapping("/trends")
    @RequirePermission("analytics:read")
    public ApiResponse<List<Map<String, Object>>> trends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String granularity) {
        return ApiResponse.success(analyticsService.trends(from, to, granularity));
    }
}
