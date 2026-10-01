package com.amdocs.telecom.model;

import java.time.LocalDateTime;

public class SubscriptionHistory {

    private Long historyId;
    private Long subscriptionId;
    private Long oldPlanId;
    private Long newPlanId;
    private LocalDateTime changeDate;
    private String changeReason;
    private String changedBy;

    public SubscriptionHistory() {
    }

    public SubscriptionHistory(Long subscriptionId, Long newPlanId, String changedBy) {
        this.subscriptionId = subscriptionId;
        this.newPlanId = newPlanId;
        this.changedBy = changedBy;
    }

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    public Long getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(Long subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Long getOldPlanId() {
        return oldPlanId;
    }

    public void setOldPlanId(Long oldPlanId) {
        this.oldPlanId = oldPlanId;
    }

    public Long getNewPlanId() {
        return newPlanId;
    }

    public void setNewPlanId(Long newPlanId) {
        this.newPlanId = newPlanId;
    }

    public LocalDateTime getChangeDate() {
        return changeDate;
    }

    public void setChangeDate(LocalDateTime changeDate) {
        this.changeDate = changeDate;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    @Override
    public String toString() {
        return "SubscriptionHistory{" +
                "historyId=" + historyId +
                ", subscriptionId=" + subscriptionId +
                ", oldPlanId=" + oldPlanId +
                ", newPlanId=" + newPlanId +
                ", changeDate=" + changeDate +
                ", changeReason='" + changeReason + '\'' +
                ", changedBy='" + changedBy + '\'' +
                '}';
    }
}
