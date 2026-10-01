package com.amdocs.telecom.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UsageRecord {

    private Long usageId;
    private Long subscriptionId;
    private LocalDateTime usageDate;
    private String usageType;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal charge;
    private LocalDateTime createdAt;

    public UsageRecord() {
    }

    public UsageRecord(Long subscriptionId, LocalDateTime usageDate, String usageType, BigDecimal quantity,
                        String unit) {
        this.subscriptionId = subscriptionId;
        this.usageDate = usageDate;
        this.usageType = usageType;
        this.quantity = quantity;
        this.unit = unit;
    }

    public Long getUsageId() {
        return usageId;
    }

    public void setUsageId(Long usageId) {
        this.usageId = usageId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public LocalDateTime getUsageDate() {
        return usageDate;
    }

    public void setUsageDate(LocalDateTime usageDate) {
        this.usageDate = usageDate;
    }

    public String getUsageType() {
        return usageType;
    }

    public void setUsageType(String usageType) {
        this.usageType = usageType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getCharge() {
        return charge;
    }

    public void setCharge(BigDecimal charge) {
        this.charge = charge;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "UsageRecord{" +
                "usageId=" + usageId +
                ", subscriptionId=" + subscriptionId +
                ", usageDate=" + usageDate +
                ", usageType='" + usageType + '\'' +
                ", quantity=" + quantity +
                ", unit='" + unit + '\'' +
                ", charge=" + charge +
                ", createdAt=" + createdAt +
                '}';
    }
}
