package com.amdocs.telecom.model;

import java.time.LocalDateTime;

public class LoginHistory {

    private Long loginHistoryId;
    private Long customerId;
    private Long adminId;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private String ipAddress;
    private String loginStatus;
    private String failureReason;

    public LoginHistory() {
    }

    public LoginHistory(Long customerId, Long adminId, String ipAddress, String loginStatus) {
        this.customerId = customerId;
        this.adminId = adminId;
        this.ipAddress = ipAddress;
        this.loginStatus = loginStatus;
    }

    public Long getLoginHistoryId() {
        return loginHistoryId;
    }

    public void setLoginHistoryId(Long loginHistoryId) {
        this.loginHistoryId = loginHistoryId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public LocalDateTime getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(LocalDateTime logoutTime) {
        this.logoutTime = logoutTime;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getLoginStatus() {
        return loginStatus;
    }

    public void setLoginStatus(String loginStatus) {
        this.loginStatus = loginStatus;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    @Override
    public String toString() {
        return "LoginHistory{" +
                "loginHistoryId=" + loginHistoryId +
                ", customerId=" + customerId +
                ", adminId=" + adminId +
                ", loginTime=" + loginTime +
                ", logoutTime=" + logoutTime +
                ", ipAddress='" + ipAddress + '\'' +
                ", loginStatus='" + loginStatus + '\'' +
                ", failureReason='" + failureReason + '\'' +
                '}';
    }
}
