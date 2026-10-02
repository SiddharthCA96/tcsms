package com.amdocs.telecom.service;

import com.amdocs.telecom.model.MobileSubscription;
import com.amdocs.telecom.model.SubscriptionHistory;

import java.util.List;
import java.util.Optional;

/**
 * Business-level contract for mobile subscription management.
 *
 * Note: the case study mentions "activate/deactivate add-on services", but the schema has no
 * add-on table or model - only the subscription's own status column. activateSubscription()/
 * deactivateSubscription() operate on that column; a true add-on feature is out of scope here
 * until a supporting model/table exists.
 */
public interface SubscriptionService {

    MobileSubscription getSubscriptionById(Long subscriptionId);

    Optional<MobileSubscription> findSubscriptionByNumber(String subscriptionNumber);

    List<MobileSubscription> getCustomerSubscriptions(Long customerId);

    /**
     * Subscribes a customer to a plan, creating a new subscription. Rejects an inactive plan and
     * rejects subscribing the same customer to the same plan twice.
     */
    MobileSubscription subscribeToPlan(MobileSubscription subscription);

    /**
     * Changes the subscription's plan and records the change in SubscriptionHistory.
     */
    void changePlan(Long subscriptionId, Long newPlanId, String changedBy, String changeReason);

    /**
     * Changes to a higher-priced plan (by monthlyRental). Rejects a new plan that isn't actually
     * more expensive than the current one.
     */
    void upgradePlan(Long subscriptionId, Long newPlanId, String changedBy);

    /**
     * Changes to a lower-priced plan (by monthlyRental). Rejects a new plan that isn't actually
     * cheaper than the current one.
     */
    void downgradePlan(Long subscriptionId, Long newPlanId, String changedBy);

    void changeSubscriptionType(Long subscriptionId, String newSubscriptionType);

    void activateSubscription(Long subscriptionId);

    void deactivateSubscription(Long subscriptionId);

    List<SubscriptionHistory> getSubscriptionHistory(Long subscriptionId);
}
