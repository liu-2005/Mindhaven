package com.mindhaven.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindhaven.common.ApiResponse;
import com.mindhaven.common.BizException;
import com.mindhaven.common.ErrorCode;
import com.mindhaven.common.PageResult;
import com.mindhaven.dto.RiskEventDto;
import com.mindhaven.entity.RiskEvent;
import com.mindhaven.repository.RiskEventRepository;
import com.mindhaven.security.AuthContextHolder;
import com.mindhaven.security.RequirePermission;
import com.mindhaven.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/risk-events")
public class AdminRiskController {

    private final RiskEventRepository riskEventRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AdminRiskController(RiskEventRepository riskEventRepository, AuditService auditService, ObjectMapper objectMapper) {
        this.riskEventRepository = riskEventRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @RequirePermission("risk:read")
    public ApiResponse<PageResult<RiskEventDto>> page(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size,
                                                      @RequestParam(required = false) String status,
                                                      @RequestParam(required = false) String level) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, safeSize, Sort.by(Sort.Direction.DESC, "detectedAt"));
        Page<RiskEvent> result = riskEventRepository.search(status, level, pageable);
        var items = result.getContent().stream().map(e -> RiskEventDto.of(e, parseCodes(e.getTriggerCodesJson()))).toList();
        return ApiResponse.success(PageResult.of(items, page, safeSize, result.getTotalElements()));
    }

    @GetMapping("/{eventId}")
    @RequirePermission("risk:read-sensitive")
    public ApiResponse<RiskEventDto> detail(@PathVariable Long eventId) {
        RiskEvent event = riskEventRepository.findById(eventId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND));
        auditService.record(AuthContextHolder.userId(), "risk:view-sensitive", "RISK_EVENT", String.valueOf(eventId),
                "SUCCESS", "查看风险敏感详情", null);
        return ApiResponse.success(RiskEventDto.of(event, parseCodes(event.getTriggerCodesJson())));
    }

    @PutMapping("/{eventId}/status")
    @RequirePermission("risk:resolve")
    public ApiResponse<RiskEventDto> updateStatus(@PathVariable Long eventId, @RequestBody Map<String, String> body) {
        RiskEvent event = riskEventRepository.findById(eventId)
                .orElseThrow(() -> new BizException(ErrorCode.RESOURCE_NOT_FOUND));
        String status = body.get("status");
        if (!List.of("OPEN", "REVIEWING", "RESOLVED", "FALSE_POSITIVE").contains(status)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "非法的风险事件状态");
        }
        event.setStatus(status);
        if ("RESOLVED".equals(status) || "FALSE_POSITIVE".equals(status)) {
            event.setResolvedBy(AuthContextHolder.userId());
            event.setResolvedAt(LocalDateTime.now());
        }
        event.setResolutionNote(body.get("note"));
        riskEventRepository.save(event);
        auditService.record(AuthContextHolder.userId(), "risk:update-status", "RISK_EVENT", String.valueOf(eventId),
                "SUCCESS", "更新风险事件状态为 " + status, null);
        return ApiResponse.success(RiskEventDto.of(event, parseCodes(event.getTriggerCodesJson())));
    }

    private List<String> parseCodes(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
