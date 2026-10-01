package com.amdocs.telecom.model;

import java.time.LocalDateTime;

public class PasswordResetOTP {

    private Long otpId;
    private Long customerId;
    private String otpHash;
    private LocalDateTime expiresAt;
    private Boolean used;
    private LocalDateTime createdAt;

    public PasswordResetOTP() {
    }

    public PasswordResetOTP(Long customerId, String otpHash, LocalDateTime expiresAt) {
        this.customerId = customerId;
        this.otpHash = otpHash;
        this.expiresAt = expiresAt;
    }

    public Long getOtpId() {
        return otpId;
    }

    public void setOtpId(Long otpId) {
        this.otpId = otpId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public void setOtpHash(String otpHash) {
        this.otpHash = otpHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getUsed() {
        return used;
    }

    public void setUsed(Boolean used) {
        this.used = used;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "PasswordResetOTP{" +
                "otpId=" + otpId +
                ", customerId=" + customerId +
                ", expiresAt=" + expiresAt +
                ", used=" + used +
                ", createdAt=" + createdAt +
                '}';
    }
}
