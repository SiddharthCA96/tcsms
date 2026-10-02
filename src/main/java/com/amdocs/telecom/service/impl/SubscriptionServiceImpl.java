package com.amdocs.telecom.service.impl;

import com.amdocs.telecom.dao.MobileSubscriptionDAO;
import com.amdocs.telecom.dao.SubscriptionHistoryDAO;
import com.amdocs.telecom.dao.TelecomPlanDAO;
import com.amdocs.telecom.model.MobileSubscription;
import com.amdocs.telecom.model.SubscriptionHistory;
import com.amdocs.telecom.model.TelecomPlan;
import com.amdocs.telecom.service.SubscriptionService;

import java.util.List;
import java.util.Optional;

/**
 * Business logic implementation of SubscriptionService.
 *
 * Only 'ACTIVE' appears in the seeded mobile_subscriptions status data and the schema places no
 * CHECK constraint on the column, so 'INACTIVE' is used as the symmetric counterpart, following
 * the same convention already used in CustomerServiceImpl/PlanServiceImpl.
 *
 * Upgrade/downgrade direction is determined purely by comparing TelecomPlan.monthlyRental, since
 * the case study does not define a specific upgrade-restriction formula and monthlyRental is the
 * only comparable numeric attribute the schema/model provides for ranking plans.
 */
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final String SUBSCRIPTION_STATUS_ACTIVE = "ACTIVE";
    private static final String SUBSCRIPTION_STATUS_INACTIVE = "INACTIVE";
    private static final String PLAN_STATUS_ACTIVE = "ACTIVE";

    private final MobileSubscriptionDAO subscriptionDAO;
    private final TelecomPlanDAO planDAO;
    private final SubscriptionHistoryDAO subscriptionHistoryDAO;

    public SubscriptionServiceImpl(MobileSubscriptionDAO subscriptionDAO, TelecomPlanDAO planDAO,
                                    SubscriptionHistoryDAO subscriptionHistoryDAO) {
        this.subscriptionDAO = subscriptionDAO;
        this.planDAO = planDAO;
        this.subscriptionHistoryDAO = subscriptionHistoryDAO;
    }

    @Override
    public MobileSubscription getSubscriptionById(Long subscriptionId) {
        if (subscriptionId == null) {
            throw new RuntimeException("Failed to get subscription: subscriptionId must not be null.");
        }

        MobileSubscription subscription = subscriptionDAO.findById(subscriptionId);
        if (subscription == null) {
            throw new RuntimeException("Failed to get subscription: no subscription found with ID: "
                    + subscriptionId);
        }
        return subscription;
    }

    @Override
    public Optional<MobileSubscription> findSubscriptionByNumber(String subscriptionNumber) {
        if (subscriptionNumber == null || subscriptionNumber.trim().isEmpty()) {
            throw new RuntimeException("Failed to find subscription: subscriptionNumber must not be null or empty.");
        }
        return Optional.ofNullable(subscriptionDAO.findBySubscriptionNumber(subscriptionNumber));
    }

    @Override
    public List<MobileSubscription> getCustomerSubscriptions(Long customerId) {
        if (customerId == null) {
            throw new RuntimeException("Failed to get customer subscriptions: customerId must not be null.");
        }
        return subscriptionDAO.findByCustomerId(customerId);
    }

    @Override
    public MobileSubscription subscribeToPlan(MobileSubscription subscription) {
        if (subscription == null) {
            throw new RuntimeException("Failed to subscribe to plan: subscription must not be null.");
        }
        validateRequiredFields(subscription);

        TelecomPlan plan = requireActivePlan(subscription.getPlanId());

        if (subscriptionDAO.existsByCustomerAndPlan(subscription.getCustomerId(), plan.getPlanId())) {
            throw new RuntimeException("Failed to subscribe to plan: customer ID " + subscription.getCustomerId()
                    + " is already subscribed to plan ID " + plan.getPlanId() + ".");
        }

        if (subscriptionDAO.existsByMobileNumber(subscription.getMobileNumber())) {
            throw new RuntimeException("Failed to subscribe to plan: mobile number '"
                    + subscription.getMobileNumber() + "' is already in use.");
        }

        if (subscription.getStatus() == null || subscription.getStatus().trim().isEmpty()) {
            subscription.setStatus(SUBSCRIPTION_STATUS_ACTIVE);
        }

        subscriptionDAO.save(subscription);
        return subscription;
    }

    @Override
    public void changePlan(Long subscriptionId, Long newPlanId, String changedBy, String changeReason) {
        MobileSubscription subscription = getSubscriptionById(subscriptionId);
        TelecomPlan newPlan = requireActivePlan(newPlanId);

        Long oldPlanId = subscription.getPlanId();
        if (oldPlanId.equals(newPlan.getPlanId())) {
            throw new RuntimeException("Failed to change plan: subscription ID " + subscriptionId
                    + " is already on plan ID " + newPlanId + ".");
        }

        subscription.setPlanId(newPlan.getPlanId());
        subscriptionDAO.update(subscription);

        SubscriptionHistory history = new SubscriptionHistory(subscriptionId, newPlan.getPlanId(), changedBy);
        history.setOldPlanId(oldPlanId);
        history.setChangeReason(changeReason);
        subscriptionHistoryDAO.save(history);
    }

    @Override
    public void upgradePlan(Long subscriptionId, Long newPlanId, String changedBy) {
        MobileSubscription subscription = getSubscriptionById(subscriptionId);
        TelecomPlan currentPlan = requireActivePlanOrAny(subscription.getPlanId());
        TelecomPlan newPlan = requireActivePlan(newPlanId);

        if (newPlan.getMonthlyRental().compareTo(currentPlan.getMonthlyRental()) <= 0) {
            throw new RuntimeException("Failed to upgrade plan: plan ID " + newPlanId
                    + " is not more expensive than the current plan.");
        }

        changePlan(subscriptionId, newPlanId, changedBy, "Plan upgrade");
    }

    @Override
    public void downgradePlan(Long subscriptionId, Long newPlanId, String changedBy) {
        MobileSubscription subscription = getSubscriptionById(subscriptionId);
        TelecomPlan currentPlan = requireActivePlanOrAny(subscription.getPlanId());
        TelecomPlan newPlan = requireActivePlan(newPlanId);

        if (newPlan.getMonthlyRental().compareTo(currentPlan.getMonthlyRental()) >= 0) {
            throw new RuntimeException("Failed to downgrade plan: plan ID " + newPlanId
                    + " is not cheaper than the current plan.");
        }

        changePlan(subscriptionId, newPlanId, changedBy, "Plan downgrade");
    }

    @Override
    public void changeSubscriptionType(Long subscriptionId, String newSubscriptionType) {
        if (newSubscriptionType == null
                || !(newSubscriptionType.equals("PREPAID") || newSubscriptionType.equals("POSTPAID"))) {
            throw new RuntimeException("Failed to change subscription type: '" + newSubscriptionType
                    + "' is not a supported subscription type (expected PREPAID or POSTPAID).");
        }

        MobileSubscription subscription = getSubscriptionById(subscriptionId);
        if (newSubscriptionType.equals(subscription.getSubscriptionType())) {
            throw new RuntimeException("Failed to change subscription type: subscription ID " + subscriptionId
                    + " is already " + newSubscriptionType + ".");
        }

        subscription.setSubscriptionType(newSubscriptionType);
        subscriptionDAO.update(subscription);
    }

    @Override
    public void activateSubscription(Long subscriptionId) {
        MobileSubscription subscription = getSubscriptionById(subscriptionId);
        subscription.setStatus(SUBSCRIPTION_STATUS_ACTIVE);
        subscriptionDAO.update(subscription);
    }

    @Override
    public void deactivateSubscription(Long subscriptionId) {
        MobileSubscription subscription = getSubscriptionById(subscriptionId);
        subscription.setStatus(SUBSCRIPTION_STATUS_INACTIVE);
        subscriptionDAO.update(subscription);
    }

    @Override
    public List<SubscriptionHistory> getSubscriptionHistory(Long subscriptionId) {
        if (subscriptionId == null) {
            throw new RuntimeException("Failed to get subscription history: subscriptionId must not be null.");
        }
        return subscriptionHistoryDAO.findBySubscriptionId(subscriptionId);
    }

    private TelecomPlan requireActivePlan(Long planId) {
        if (planId == null) {
            throw new RuntimeException("Failed: planId must not be null.");
        }

        TelecomPlan plan = planDAO.findById(planId);
        if (plan == null) {
            throw new RuntimeException("Failed: no plan found with ID: " + planId);
        }
        if (!PLAN_STATUS_ACTIVE.equals(plan.getStatus())) {
            throw new RuntimeException("Failed: plan ID " + planId + " is not active and cannot be selected.");
        }
        return plan;
    }

    /**
     * Looks up the subscription's CURRENT plan for price comparison. Unlike requireActivePlan(),
     * this does not reject an inactive current plan - a subscription already on a plan that has
     * since been deactivated must still be able to upgrade/downgrade away from it.
     */
    private TelecomPlan requireActivePlanOrAny(Long planId) {
        TelecomPlan plan = planDAO.findById(planId);
        if (plan == null) {
            throw new RuntimeException("Failed: no plan found with ID: " + planId);
        }
        return plan;
    }

    private void validateRequiredFields(MobileSubscription subscription) {
        if (subscription.getCustomerId() == null) {
            throw new RuntimeException("Subscription customerId must not be null.");
        }
        if (subscription.getPlanId() == null) {
            throw new RuntimeException("Subscription planId must not be null.");
        }
        if (subscription.getSimId() == null) {
            throw new RuntimeException("Subscription simId must not be null.");
        }
        requireNonBlank(subscription.getSubscriptionNumber(), "subscriptionNumber");
        requireNonBlank(subscription.getMobileNumber(), "mobileNumber");
        requireNonBlank(subscription.getSubscriptionType(), "subscriptionType");
        if (subscription.getActivationDate() == null) {
            throw new RuntimeException("Subscription activationDate must not be null.");
        }
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Subscription " + fieldName + " must not be null or empty.");
        }
    }
}
