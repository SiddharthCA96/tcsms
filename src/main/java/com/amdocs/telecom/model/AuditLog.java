package com.amdocs.telecom.model;

import java.time.LocalDateTime;

public class AuditLog {

    private Long auditId;
    private String actorType;
    private Long actorId;
    private String action;
    private String entityType;
    private Long entityId;
    private String description;
    private LocalDateTime createdAt;

    public AuditLog() {
    }

    public AuditLog(String actorType, Long actorId, String action, String entityType, Long entityId,
                     String description) {
        this.actorType = actorType;
        this.actorId = actorId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
    }

    public Long getAuditId() {
        return auditId;
    }

    public void setAuditId(Long auditId) {
        this.auditId = auditId;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "AuditLog{" +
                "auditId=" + auditId +
                ", actorType='" + actorType + '\'' +
                ", actorId=" + actorId +
                ", action='" + action + '\'' +
                ", entityType='" + entityType + '\'' +
                ", entityId=" + entityId +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
