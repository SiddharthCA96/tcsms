package com.amdocs.telecom.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Bill {

    private Long billId;
    private String billNumber;
    private Long subscriptionId;
    private LocalDate billingMonth;
    private BigDecimal planRental;
    private BigDecimal usageCharges;
    private BigDecimal taxAmount;
    private BigDecimal discount;
    private BigDecimal totalAmount;
    private LocalDate dueDate;
    private String billStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Bill() {
    }

    public Bill(String billNumber, Long subscriptionId, LocalDate billingMonth, BigDecimal planRental,
                BigDecimal totalAmount, LocalDate dueDate, String billStatus) {
        this.billNumber = billNumber;
        this.subscriptionId = subscriptionId;
        this.billingMonth = billingMonth;
        this.planRental = planRental;
        this.totalAmount = totalAmount;
        this.dueDate = dueDate;
        this.billStatus = billStatus;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public LocalDate getBillingMonth() {
        return billingMonth;
    }

    public void setBillingMonth(LocalDate billingMonth) {
        this.billingMonth = billingMonth;
    }

    public BigDecimal getPlanRental() {
        return planRental;
    }

    public void setPlanRental(BigDecimal planRental) {
        this.planRental = planRental;
    }

    public BigDecimal getUsageCharges() {
        return usageCharges;
    }

    public void setUsageCharges(BigDecimal usageCharges) {
        this.usageCharges = usageCharges;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getBillStatus() {
        return billStatus;
    }

    public void setBillStatus(String billStatus) {
        this.billStatus = billStatus;
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
        return "Bill{" +
                "billId=" + billId +
                ", billNumber='" + billNumber + '\'' +
                ", subscriptionId=" + subscriptionId +
                ", billingMonth=" + billingMonth +
                ", planRental=" + planRental +
                ", usageCharges=" + usageCharges +
                ", taxAmount=" + taxAmount +
                ", discount=" + discount +
                ", totalAmount=" + totalAmount +
                ", dueDate=" + dueDate +
                ", billStatus='" + billStatus + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
