package com.amdocs.telecom.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment {

    private Long paymentId;
    private String transactionReference;
    private Long billId;
    private Long customerId;
    private BigDecimal amount;
    private String paymentMode;
    private LocalDateTime paymentDate;
    private String paymentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Payment() {
    }

    public Payment(String transactionReference, Long billId, Long customerId, BigDecimal amount,
                   String paymentMode, String paymentStatus) {
        this.transactionReference = transactionReference;
        this.billId = billId;
        this.customerId = customerId;
        this.amount = amount;
        this.paymentMode = paymentMode;
        this.paymentStatus = paymentStatus;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
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
        return "Payment{" +
                "paymentId=" + paymentId +
                ", transactionReference='" + transactionReference + '\'' +
                ", billId=" + billId +
                ", customerId=" + customerId +
                ", amount=" + amount +
                ", paymentMode='" + paymentMode + '\'' +
                ", paymentDate=" + paymentDate +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
