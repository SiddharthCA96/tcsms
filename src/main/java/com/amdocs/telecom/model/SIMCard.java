package com.amdocs.telecom.model;

import java.time.LocalDateTime;

public class SIMCard {

    private Long simId;
    private String simNumber;
    private String simType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public SIMCard() {
    }

    public SIMCard(String simNumber, String simType, String status) {
        this.simNumber = simNumber;
        this.simType = simType;
        this.status = status;
    }

    public Long getSimId() {
        return simId;
    }

    public void setSimId(Long simId) {
        this.simId = simId;
    }

    public String getSimNumber() {
        return simNumber;
    }

    public void setSimNumber(String simNumber) {
        this.simNumber = simNumber;
    }

    public String getSimType() {
        return simType;
    }

    public void setSimType(String simType) {
        this.simType = simType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "SIMCard{" +
                "simId=" + simId +
                ", simNumber='" + simNumber + '\'' +
                ", simType='" + simType + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
