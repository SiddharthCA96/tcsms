package com.amdocs.telecom.service.impl;

import com.amdocs.telecom.dao.BillingDAO;
import com.amdocs.telecom.dao.MobileSubscriptionDAO;
import com.amdocs.telecom.dao.TelecomPlanDAO;
import com.amdocs.telecom.dao.UsageDAO;
import com.amdocs.telecom.model.Bill;
import com.amdocs.telecom.model.MobileSubscription;
import com.amdocs.telecom.model.TelecomPlan;
import com.amdocs.telecom.model.UsageRecord;
import com.amdocs.telecom.service.BillingService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business logic implementation of BillingService.
 *
 * 'UNPAID' is used as the initial status for a newly generated bill - it is an actual value
 * already used in seed_data.sql (alongside PAID/OVERDUE), representing a bill with no payment
 * applied yet, rather than an invented status.
 */
public class BillingServiceImpl implements BillingService {

    private static final String BILL_STATUS_UNPAID = "UNPAID";
    private static final String BILL_STATUS_PAID = "PAID";
    private static final String BILL_STATUS_OVERDUE = "OVERDUE";

    private final BillingDAO billingDAO;
    private final MobileSubscriptionDAO subscriptionDAO;
    private final TelecomPlanDAO planDAO;
    private final UsageDAO usageDAO;

    public BillingServiceImpl(BillingDAO billingDAO, MobileSubscriptionDAO subscriptionDAO, TelecomPlanDAO planDAO,
                               UsageDAO usageDAO) {
        this.billingDAO = billingDAO;
        this.subscriptionDAO = subscriptionDAO;
        this.planDAO = planDAO;
        this.usageDAO = usageDAO;
    }

    @Override
    public Bill getBillById(Long billId) {
        if (billId == null) {
            throw new RuntimeException("Failed to get bill: billId must not be null.");
        }

        Bill bill = billingDAO.findById(billId);
        if (bill == null) {
            throw new RuntimeException("Failed to get bill: no bill found with ID: " + billId);
        }
        return bill;
    }

    @Override
    public Optional<Bill> findBillByNumber(String billNumber) {
        if (billNumber == null || billNumber.trim().isEmpty()) {
            throw new RuntimeException("Failed to find bill: billNumber must not be null or empty.");
        }
        return Optional.ofNullable(billingDAO.findByBillNumber(billNumber));
    }

    @Override
    public List<Bill> getSubscriptionBills(Long subscriptionId) {
        if (subscriptionId == null) {
            throw new RuntimeException("Failed to get subscription bills: subscriptionId must not be null.");
        }
        return billingDAO.findBySubscriptionId(subscriptionId);
    }

    @Override
    public Optional<Bill> findBillForMonth(Long subscriptionId, LocalDate billingMonth) {
        if (subscriptionId == null || billingMonth == null) {
            throw new RuntimeException("Failed to find bill: subscriptionId and billingMonth must not be null.");
        }
        return Optional.ofNullable(billingDAO.findBySubscriptionIdAndBillingMonth(subscriptionId, billingMonth));
    }

    @Override
    public List<Bill> getBillsByStatus(String billStatus) {
        if (billStatus == null || billStatus.trim().isEmpty()) {
            throw new RuntimeException("Failed to get bills: billStatus must not be null or empty.");
        }
        return billingDAO.findByStatus(billStatus);
    }

    @Override
    public BigDecimal calculatePlanRental(Long subscriptionId) {
        MobileSubscription subscription = requireSubscription(subscriptionId);

        TelecomPlan plan = planDAO.findById(subscription.getPlanId());
        if (plan == null) {
            throw new RuntimeException("Failed to calculate plan rental: no plan found with ID: "
                    + subscription.getPlanId());
        }
        return plan.getMonthlyRental();
    }

    @Override
    public BigDecimal calculateUsageCharges(Long subscriptionId, LocalDate billingMonth) {
        requireSubscription(subscriptionId);
        if (billingMonth == null) {
            throw new RuntimeException("Failed to calculate usage charges: billingMonth must not be null.");
        }

        List<UsageRecord> usageRecords = usageDAO.findBySubscriptionId(subscriptionId);

        return usageRecords.stream()
                .filter(record -> isWithinBillingMonth(record.getUsageDate().toLocalDate(), billingMonth))
                .map(UsageRecord::getCharge)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Bill generateMonthlyBill(String billNumber, Long subscriptionId, LocalDate billingMonth,
                                     BigDecimal taxAmount, BigDecimal discount, LocalDate dueDate) {
        if (billNumber == null || billNumber.trim().isEmpty()) {
            throw new RuntimeException("Failed to generate bill: billNumber must not be null or empty.");
        }
        requireSubscription(subscriptionId);
        if (billingMonth == null) {
            throw new RuntimeException("Failed to generate bill: billingMonth must not be null.");
        }
        if (dueDate == null) {
            throw new RuntimeException("Failed to generate bill: dueDate must not be null.");
        }

        if (billingDAO.findBySubscriptionIdAndBillingMonth(subscriptionId, billingMonth) != null) {
            throw new RuntimeException("Failed to generate bill: a bill already exists for subscription ID "
                    + subscriptionId + " and billing month " + billingMonth + ".");
        }

        BigDecimal planRental = calculatePlanRental(subscriptionId);
        BigDecimal usageCharges = calculateUsageCharges(subscriptionId, billingMonth);
        BigDecimal effectiveTax = taxAmount != null ? taxAmount : BigDecimal.ZERO;
        BigDecimal effectiveDiscount = discount != null ? discount : BigDecimal.ZERO;

        BigDecimal totalAmount = planRental.add(usageCharges).add(effectiveTax).subtract(effectiveDiscount);

        Bill bill = new Bill(billNumber, subscriptionId, billingMonth, planRental, totalAmount, dueDate,
                BILL_STATUS_UNPAID);
        bill.setUsageCharges(usageCharges);
        bill.setTaxAmount(effectiveTax);
        bill.setDiscount(effectiveDiscount);

        billingDAO.save(bill);
        return bill;
    }

    @Override
    public void markBillOverdue(Long billId) {
        Bill bill = getBillById(billId);
        if (BILL_STATUS_PAID.equals(bill.getBillStatus())) {
            throw new RuntimeException("Failed to mark bill overdue: bill ID " + billId
                    + " is already PAID.");
        }

        bill.setBillStatus(BILL_STATUS_OVERDUE);
        billingDAO.update(bill);
    }

    private MobileSubscription requireSubscription(Long subscriptionId) {
        if (subscriptionId == null) {
            throw new RuntimeException("subscriptionId must not be null.");
        }

        MobileSubscription subscription = subscriptionDAO.findById(subscriptionId);
        if (subscription == null) {
            throw new RuntimeException("No subscription found with ID: " + subscriptionId);
        }
        return subscription;
    }

    private boolean isWithinBillingMonth(LocalDate usageDate, LocalDate billingMonth) {
        return usageDate.getYear() == billingMonth.getYear()
                && usageDate.getMonthValue() == billingMonth.getMonthValue();
    }
}
