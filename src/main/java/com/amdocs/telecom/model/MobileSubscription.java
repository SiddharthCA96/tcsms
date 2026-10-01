package com.amdocs.telecom.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MobileSubscription {

    private Long subscriptionId;
    private String subscriptionNumber;
    private Long customerId;
    private String mobileNumber;
    private Long simId;
    private Long planId;
    private LocalDate activationDate;
    private String subscriptionType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MobileSubscription() {
    }

    public MobileSubscription(String subscriptionNumber, Long customerId, String mobileNumber, Long simId,
                               Long planId, LocalDate activationDate, String subscriptionType, String status) {
        this.subscriptionNumber = subscriptionNumber;
        this.customerId = customerId;
        this.mobileNumber = mobileNumber;
        this.simId = simId;
        this.planId = planId;
        this.activationDate = activationDate;
        this.subscriptionType = subscriptionType;
        this.status = status;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getSubscriptionNumber() {
        return subscriptionNumber;
    }

    public void setSubscriptionNumber(String subscriptionNumber) {
        this.subscriptionNumber = subscriptionNumber;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public Long getSimId() {
        return simId;
    }

    public void setSimId(Long simId) {
        this.simId = simId;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public LocalDate getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(LocalDate activationDate) {
        this.activationDate = activationDate;
    }

    public String getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(String subscriptionType) {
        this.subscriptionType = subscriptionType;
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
        return "MobileSubscription{" +
                "subscriptionId=" + subscriptionId +
                ", subscriptionNumber='" + subscriptionNumber + '\'' +
                ", customerId=" + customerId +
                ", mobileNumber='" + mobileNumber + '\'' +
                ", simId=" + simId +
                ", planId=" + planId +
                ", activationDate=" + activationDate +
                ", subscriptionType='" + subscriptionType + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
