package com.amdocs.telecom.dao;

import com.amdocs.telecom.model.UsageRecord;

import java.util.List;

public interface UsageDAO {

    UsageRecord findById(Long usageId);

    List<UsageRecord> findBySubscriptionId(Long subscriptionId);

    List<UsageRecord> findBySubscriptionIdAndUsageType(Long subscriptionId, String usageType);

    void save(UsageRecord usageRecord);
}
