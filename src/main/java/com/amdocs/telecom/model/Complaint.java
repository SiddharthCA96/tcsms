package com.amdocs.telecom.model;

import java.time.LocalDateTime;

public class Complaint {

    private Long complaintId;
    private String complaintNumber;
    private Long customerId;
    private Long subscriptionId;
    private String category;
    private String description;
    private String priority;
    private LocalDateTime createdDate;
    private String status;
    private String resolution;
    private LocalDateTime updatedAt;

    public Complaint() {
    }

    public Complaint(String complaintNumber, Long customerId, String category, String description,
                      String priority, String status) {
        this.complaintNumber = complaintNumber;
        this.customerId = customerId;
        this.category = category;
        this.description = description;
        this.priority = priority;
        this.status = status;
    }

    public Long getComplaintId() {
        return complaintId;
    }

    public void setComplaintId(Long complaintId) {
        this.complaintId = complaintId;
    }

    public String getComplaintNumber() {
        return complaintNumber;
    }

    public void setComplaintNumber(String complaintNumber) {
        this.complaintNumber = complaintNumber;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Complaint{" +
                "complaintId=" + complaintId +
                ", complaintNumber='" + complaintNumber + '\'' +
                ", customerId=" + customerId +
                ", subscriptionId=" + subscriptionId +
                ", category='" + category + '\'' +
                ", description='" + description + '\'' +
                ", priority='" + priority + '\'' +
                ", createdDate=" + createdDate +
                ", status='" + status + '\'' +
                ", resolution='" + resolution + '\'' +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
