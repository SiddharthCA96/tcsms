package com.amdocs.telecom.service;

import com.amdocs.telecom.model.TelecomPlan;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Business-level contract for telecom plan browsing and administration.
 * Customer-facing search/filter/sort/compare operations work over the active plan catalog only,
 * since an inactive plan cannot be selected/subscribed to.
 */
public interface PlanService {

    TelecomPlan getPlanById(Long planId);

    Optional<TelecomPlan> findPlanByCode(String planCode);

    List<TelecomPlan> getAllActivePlans();

    List<TelecomPlan> searchActivePlansByName(String keyword);

    List<TelecomPlan> filterActivePlansByMaxPrice(BigDecimal maxMonthlyRental);

    List<TelecomPlan> filterActivePlansByMinDataAllowance(BigDecimal minDataAllowanceGb);

    List<TelecomPlan> sortActivePlansByPriceAscending();

    List<TelecomPlan> sortActivePlansByPriceDescending();

    /**
     * Retrieves the given plans (by ID) side-by-side for comparison.
     */
    List<TelecomPlan> comparePlans(List<Long> planIds);

    TelecomPlan createPlan(TelecomPlan plan);

    void updatePlanDetails(TelecomPlan plan);

    void deactivatePlan(Long planId);

    void activatePlan(Long planId);
}
