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
}
