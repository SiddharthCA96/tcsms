package com.amdocs.telecom.service;

import com.amdocs.telecom.model.Bill;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business-level contract for bill generation and retrieval.
 *
 * The schema/case study define no tax percentage, discount formula, bill-numbering scheme, or
 * due-date offset, so generateMonthlyBill() takes billNumber, taxAmount, discount and dueDate as
 * explicit inputs rather than computing them from an invented policy. planRental and
 * usageCharges ARE computed here, since they come directly from the subscription's plan and its
 * usage_records.charge values.
 *
 * Bill-status transitions tied to payment (e.g. marking a bill PAID) are intentionally NOT
 * exposed here - PaymentService performs that update inside its own JDBC transaction, alongside
 * payment creation and the audit record, so it must not go through a separate connection here.
 */
public interface BillingService {

    Bill getBillById(Long billId);

    Optional<Bill> findBillByNumber(String billNumber);

    List<Bill> getSubscriptionBills(Long subscriptionId);

    Optional<Bill> findBillForMonth(Long subscriptionId, LocalDate billingMonth);

    List<Bill> getBillsByStatus(String billStatus);

    BigDecimal calculatePlanRental(Long subscriptionId);

    /**
     * Sums usage_records.charge for the subscription's usage within the given billing month.
     */
    BigDecimal calculateUsageCharges(Long subscriptionId, LocalDate billingMonth);

    /**
     * Generates a bill for a subscription/billing month, computing planRental and usageCharges
     * from the subscription's plan and usage records; taxAmount, discount and dueDate are
     * supplied by the caller. Rejects a duplicate bill for the same subscription/billing month.
     */
    Bill generateMonthlyBill(String billNumber, Long subscriptionId, LocalDate billingMonth,
                              BigDecimal taxAmount, BigDecimal discount, LocalDate dueDate);

    /**
     * Marks a not-yet-paid bill as OVERDUE (e.g. from a scheduled job once the due date passes).
     */
    void markBillOverdue(Long billId);
}
