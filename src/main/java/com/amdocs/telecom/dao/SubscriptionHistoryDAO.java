package com.amdocs.telecom.dao;

import com.amdocs.telecom.model.SubscriptionHistory;

import java.util.List;

public interface SubscriptionHistoryDAO {

    SubscriptionHistory findById(Long historyId);

    List<SubscriptionHistory> findBySubscriptionId(Long subscriptionId);

    void save(SubscriptionHistory history);
}
