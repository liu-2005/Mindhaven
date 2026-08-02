package com.mindhaven.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "mh_audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    private String action;

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "resource_id")
    private String resourceId;

    private String result;

    private String reason;

    @Column(name = "request_id")
    private String requestId;

    @Column(name = "client_ip_hash")
    private String clientIpHash;

    @Column(name = "user_agent_summary")
    private String userAgentSummary;

    @Column(name = "metadata_json")
    private String metadataJson;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setClientIpHash(String clientIpHash) {
        this.clientIpHash = clientIpHash;
    }

    public void setUserAgentSummary(String userAgentSummary) {
        this.userAgentSummary = userAgentSummary;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
