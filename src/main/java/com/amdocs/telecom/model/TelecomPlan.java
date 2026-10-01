package com.amdocs.telecom.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TelecomPlan {

    private Long planId;
    private String planCode;
    private String planName;
    private String planType;
    private BigDecimal monthlyRental;
    private BigDecimal dataAllowanceGb;
    private Integer voiceMinutes;
    private Integer smsAllowance;
    private Integer validityDays;
    private Boolean internationalRoaming;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TelecomPlan() {
    }

    public TelecomPlan(String planCode, String planName, String planType, BigDecimal monthlyRental,
                        BigDecimal dataAllowanceGb, Integer validityDays, String status) {
        this.planCode = planCode;
        this.planName = planName;
        this.planType = planType;
        this.monthlyRental = monthlyRental;
        this.dataAllowanceGb = dataAllowanceGb;
        this.validityDays = validityDays;
        this.status = status;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public BigDecimal getMonthlyRental() {
        return monthlyRental;
    }

    public void setMonthlyRental(BigDecimal monthlyRental) {
        this.monthlyRental = monthlyRental;
    }

    public BigDecimal getDataAllowanceGb() {
        return dataAllowanceGb;
    }

    public void setDataAllowanceGb(BigDecimal dataAllowanceGb) {
        this.dataAllowanceGb = dataAllowanceGb;
    }

    public Integer getVoiceMinutes() {
        return voiceMinutes;
    }

    public void setVoiceMinutes(Integer voiceMinutes) {
        this.voiceMinutes = voiceMinutes;
    }

    public Integer getSmsAllowance() {
        return smsAllowance;
    }

    public void setSmsAllowance(Integer smsAllowance) {
        this.smsAllowance = smsAllowance;
    }

    public Integer getValidityDays() {
        return validityDays;
    }

    public void setValidityDays(Integer validityDays) {
        this.validityDays = validityDays;
    }

    public Boolean getInternationalRoaming() {
        return internationalRoaming;
    }

    public void setInternationalRoaming(Boolean internationalRoaming) {
        this.internationalRoaming = internationalRoaming;
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
        return "TelecomPlan{" +
                "planId=" + planId +
                ", planCode='" + planCode + '\'' +
                ", planName='" + planName + '\'' +
                ", planType='" + planType + '\'' +
                ", monthlyRental=" + monthlyRental +
                ", dataAllowanceGb=" + dataAllowanceGb +
                ", voiceMinutes=" + voiceMinutes +
                ", smsAllowance=" + smsAllowance +
                ", validityDays=" + validityDays +
                ", internationalRoaming=" + internationalRoaming +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
