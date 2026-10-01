package com.amdocs.telecom.model;

import java.time.LocalDateTime;

public class Notification {

    private Long notificationId;
    private Long customerId;
    private String notificationType;
    private String message;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;

    public Notification() {
    }

    public Notification(Long customerId, String notificationType, String message, String status) {
        this.customerId = customerId;
        this.notificationType = notificationType;
        this.message = message;
        this.status = status;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "notificationId=" + notificationId +
                ", customerId=" + customerId +
                ", notificationType='" + notificationType + '\'' +
                ", message='" + message + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", sentAt=" + sentAt +
                '}';
    }
}
