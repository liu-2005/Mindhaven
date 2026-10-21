package com.mindhaven.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.mindhaven.entity.RiskEvent;

public record RiskEventDto(Long id, Long userId, String sourceType, Long sourceId, String riskLevel,
                           List<String> triggerCodes, String redactedSummary, String policyVersion,
                           String modelVersion, String status, Long assignedTo, Long resolvedBy,
                           String resolutionNote, LocalDateTime detectedAt, LocalDateTime resolvedAt) {

    public static RiskEventDto of(RiskEvent e, List<String> triggerCodes) {
        return new RiskEventDto(e.getId(), e.getUserId(), e.getSourceType(), e.getSourceId(), e.getRiskLevel(),
                triggerCodes, e.getRedactedSummary(), e.getPolicyVersion(), e.getModelVersion(), e.getStatus(),
                e.getAssignedTo(), e.getResolvedBy(), e.getResolutionNote(), e.getDetectedAt(), e.getResolvedAt());
    }

    public static RiskEventDto summary(RiskEvent e, List<String> triggerCodes) {
        return new RiskEventDto(e.getId(), e.getUserId(), e.getSourceType(), e.getSourceId(), e.getRiskLevel(),
                triggerCodes, triggerCodes.contains("OUTPUT_BLOCKED") ? "模型输出审核未通过，已返回安全提示。" : "检测到" + e.getRiskLevel() + "风险信号，请由授权人员核查。", e.getPolicyVersion(),
                e.getModelVersion(), e.getStatus(), e.getAssignedTo(), e.getResolvedBy(), null, e.getDetectedAt(), e.getResolvedAt());
    }
}
