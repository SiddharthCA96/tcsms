package com.amdocs.telecom.service.impl;

import com.amdocs.telecom.dao.TelecomPlanDAO;
import com.amdocs.telecom.model.TelecomPlan;
import com.amdocs.telecom.service.PlanService;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Business logic implementation of PlanService.
 *
 * Only 'ACTIVE' appears in the seeded telecom_plans status data and the schema places no CHECK
 * constraint on the column, so 'INACTIVE' is used as the symmetric counterpart, following the
 * same convention already used in CustomerServiceImpl.
 */
public class PlanServiceImpl implements PlanService {

    private static final String PLAN_STATUS_ACTIVE = "ACTIVE";
    private static final String PLAN_STATUS_INACTIVE = "INACTIVE";

    private final TelecomPlanDAO planDAO;

    public PlanServiceImpl(TelecomPlanDAO planDAO) {
        this.planDAO = planDAO;
    }

    @Override
    public TelecomPlan getPlanById(Long planId) {
        if (planId == null) {
            throw new RuntimeException("Failed to get plan: planId must not be null.");
        }

        TelecomPlan plan = planDAO.findById(planId);
        if (plan == null) {
            throw new RuntimeException("Failed to get plan: no plan found with ID: " + planId);
        }
        return plan;
    }

    @Override
    public Optional<TelecomPlan> findPlanByCode(String planCode) {
        if (planCode == null || planCode.trim().isEmpty()) {
            throw new RuntimeException("Failed to find plan: planCode must not be null or empty.");
        }
        return Optional.ofNullable(planDAO.findByPlanCode(planCode));
    }

    @Override
    public List<TelecomPlan> getAllActivePlans() {
        return planDAO.findAllActive();
    }

    @Override
    public List<TelecomPlan> searchActivePlansByName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new RuntimeException("Failed to search plans: keyword must not be null or empty.");
        }

        String normalizedKeyword = keyword.trim().toLowerCase();
        return getAllActivePlans().stream()
                .filter(plan -> plan.getPlanName().toLowerCase().contains(normalizedKeyword))
                .collect(Collectors.toList());
    }

    @Override
    public List<TelecomPlan> filterActivePlansByMaxPrice(BigDecimal maxMonthlyRental) {
        if (maxMonthlyRental == null) {
            throw new RuntimeException("Failed to filter plans: maxMonthlyRental must not be null.");
        }

        return getAllActivePlans().stream()
                .filter(plan -> plan.getMonthlyRental().compareTo(maxMonthlyRental) <= 0)
                .collect(Collectors.toList());
    }

    @Override
    public List<TelecomPlan> filterActivePlansByMinDataAllowance(BigDecimal minDataAllowanceGb) {
        if (minDataAllowanceGb == null) {
            throw new RuntimeException("Failed to filter plans: minDataAllowanceGb must not be null.");
        }

        return getAllActivePlans().stream()
                .filter(plan -> plan.getDataAllowanceGb().compareTo(minDataAllowanceGb) >= 0)
                .collect(Collectors.toList());
    }

    @Override
    public List<TelecomPlan> sortActivePlansByPriceAscending() {
        Comparator<TelecomPlan> byPriceAscending = Comparator.comparing(TelecomPlan::getMonthlyRental);
        return getAllActivePlans().stream()
                .sorted(byPriceAscending)
                .collect(Collectors.toList());
    }

    @Override
    public List<TelecomPlan> sortActivePlansByPriceDescending() {
        Comparator<TelecomPlan> byPriceDescending = Comparator.comparing(TelecomPlan::getMonthlyRental).reversed();
        return getAllActivePlans().stream()
                .sorted(byPriceDescending)
                .collect(Collectors.toList());
    }

    @Override
    public List<TelecomPlan> comparePlans(List<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            throw new RuntimeException("Failed to compare plans: at least one planId must be provided.");
        }

        return planIds.stream()
                .map(this::getPlanById)
                .collect(Collectors.toList());
    }

    @Override
    public TelecomPlan createPlan(TelecomPlan plan) {
        if (plan == null) {
            throw new RuntimeException("Failed to create plan: plan must not be null.");
        }
        validateRequiredFields(plan);

        if (planDAO.existsByPlanCode(plan.getPlanCode())) {
            throw new RuntimeException("Failed to create plan: plan code '" + plan.getPlanCode()
                    + "' is already in use.");
        }

        if (plan.getStatus() == null || plan.getStatus().trim().isEmpty()) {
            plan.setStatus(PLAN_STATUS_ACTIVE);
        }

        planDAO.save(plan);
        return plan;
    }

    @Override
    public void updatePlanDetails(TelecomPlan plan) {
        if (plan == null || plan.getPlanId() == null) {
            throw new RuntimeException("Failed to update plan: plan and planId must not be null.");
        }

        TelecomPlan existingPlan = planDAO.findById(plan.getPlanId());
        if (existingPlan == null) {
            throw new RuntimeException("Failed to update plan: no plan found with ID: " + plan.getPlanId());
        }

        validateRequiredFields(plan);

        if (!existingPlan.getPlanCode().equals(plan.getPlanCode()) && planDAO.existsByPlanCode(plan.getPlanCode())) {
            throw new RuntimeException("Failed to update plan: plan code '" + plan.getPlanCode()
                    + "' is already in use by another plan.");
        }

        // status is a lifecycle field owned by activatePlan()/deactivatePlan(), not a generic detail update.
        plan.setStatus(existingPlan.getStatus());

        planDAO.update(plan);
    }

    @Override
    public void deactivatePlan(Long planId) {
        TelecomPlan plan = getPlanById(planId);
        plan.setStatus(PLAN_STATUS_INACTIVE);
        planDAO.update(plan);
    }

    @Override
    public void activatePlan(Long planId) {
        TelecomPlan plan = getPlanById(planId);
        plan.setStatus(PLAN_STATUS_ACTIVE);
        planDAO.update(plan);
    }

    private void validateRequiredFields(TelecomPlan plan) {
        requireNonBlank(plan.getPlanCode(), "planCode");
        requireNonBlank(plan.getPlanName(), "planName");
        requireNonBlank(plan.getPlanType(), "planType");
        if (plan.getMonthlyRental() == null) {
            throw new RuntimeException("Plan monthlyRental must not be null.");
        }
        if (plan.getDataAllowanceGb() == null) {
            throw new RuntimeException("Plan dataAllowanceGb must not be null.");
        }
        if (plan.getValidityDays() == null) {
            throw new RuntimeException("Plan validityDays must not be null.");
        }
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Plan " + fieldName + " must not be null or empty.");
        }
    }
}
