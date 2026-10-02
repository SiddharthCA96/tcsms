package com.amdocs.telecom.dao;

import com.amdocs.telecom.model.MobileSubscription;

import java.util.List;

public interface MobileSubscriptionDAO {

    MobileSubscription findById(Long subscriptionId);

    MobileSubscription findBySubscriptionNumber(String subscriptionNumber);

    List<MobileSubscription> findByCustomerId(Long customerId);

    MobileSubscription findByMobileNumber(String mobileNumber);

    boolean existsByMobileNumber(String mobileNumber);

    boolean existsByCustomerAndPlan(Long customerId, Long planId);

    void save(MobileSubscription subscription);

    void update(MobileSubscription subscription);

    void deleteById(Long subscriptionId);
}
