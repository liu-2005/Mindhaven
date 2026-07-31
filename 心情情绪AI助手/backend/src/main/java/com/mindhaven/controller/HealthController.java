package com.mindhaven.controller;

import java.time.Instant;
import java.util.Map;

import com.mindhaven.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "service", "mindhaven-backend",
                "timestamp", Instant.now().toString()
        ));
    }
}
