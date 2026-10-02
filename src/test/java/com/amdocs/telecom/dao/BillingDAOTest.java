package com.amdocs.telecom.dao;

// import com.amdocs.telecom.dao.BillingDAO;
import com.amdocs.telecom.dao.impl.BillingDAOImpl;
import com.amdocs.telecom.model.Bill;
import com.amdocs.telecom.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Manual integration test for BillingDAO / BillingDAOImpl against the real MySQL database.
 *
 * database/seed_data.sql already contains bills for subscriptions 1-4 (bill_id 1-4), so
 * findById/findByBillNumber/findBySubscriptionIdAndBillingMonth/findBySubscriptionId/findByStatus
 * are first exercised against that seeded data. A temporary Bill is then created via save() -
 * reusing seeded subscription 1 with a billing month that does not collide with its existing
 * seeded bill - to exercise save(), update() and the read methods again. BillingDAO intentionally
 * exposes no delete method, so the temporary row is removed at the end with a direct, test-only
 * JDBC statement - this cleanup logic is NOT part of the DAO or its implementation.
 */
public class BillingDAOTest {

    // Reused seeded values from database/seed_data.sql
    private static final Long SEEDED_BILL_ID = 1L;
    private static final String SEEDED_BILL_NUMBER = "INV-2026-08-10001";
    private static final Long SEEDED_SUBSCRIPTION_ID = 1L;
    private static final LocalDate SEEDED_BILLING_MONTH = LocalDate.of(2026, 8, 1);
    private static final LocalDate NON_EXISTENT_BILLING_MONTH = LocalDate.of(2026, 9, 1);
    private static final String SEEDED_STATUS = "PAID";             // bill_id 1 and 3
    private static final String NON_MATCHING_STATUS = "DAO_TEST_NONEXISTENT_STATUS";

    // Temporary bill used to test save(), update() and the re-reads
    private static final String TEMP_BILL_NUMBER = "DAO-TEST-BILL-0001";
    private static final LocalDate TEMP_BILLING_MONTH = LocalDate.of(2026, 9, 1); // no clash for subscription 1
    private static final BigDecimal TEMP_PLAN_RENTAL = new BigDecimal("999.00");
    private static final BigDecimal TEMP_USAGE_CHARGES = new BigDecimal("50.00");
    private static final BigDecimal TEMP_TAX_AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal TEMP_DISCOUNT = new BigDecimal("0.00");
    private static final BigDecimal TEMP_TOTAL_AMOUNT = new BigDecimal("1149.00");
    private static final LocalDate TEMP_DUE_DATE = LocalDate.of(2026, 9, 20);
    private static final String TEMP_BILL_STATUS = "UNPAID";

    // Values used to exercise update()
    private static final BigDecimal UPDATED_PLAN_RENTAL = new BigDecimal("1099.00");
    private static final BigDecimal UPDATED_USAGE_CHARGES = new BigDecimal("80.00");
    private static final BigDecimal UPDATED_TAX_AMOUNT = new BigDecimal("120.00");
    private static final BigDecimal UPDATED_DISCOUNT = new BigDecimal("20.00");
    private static final BigDecimal UPDATED_TOTAL_AMOUNT = new BigDecimal("1279.00");
    private static final LocalDate UPDATED_DUE_DATE = LocalDate.of(2026, 9, 25);
    private static final String UPDATED_BILL_STATUS = "PAID";

    public static void main(String[] args) {
        BillingDAO billingDAO = new BillingDAOImpl();

        System.out.println("=== BillingDAO Test ===");

        Long tempBillId = null;

        try {
            // Test 1 - findById using an existing seeded bill
            Bill seededBill = billingDAO.findById(SEEDED_BILL_ID);
            boolean test1Pass = seededBill != null;
            System.out.println("Test 1 - findById (seeded bill): " + (test1Pass ? "PASS" : "FAIL"));
            if (test1Pass) {
                System.out.println("  billId: " + seededBill.getBillId());
                System.out.println("  billNumber: " + seededBill.getBillNumber());
                System.out.println("  subscriptionId: " + seededBill.getSubscriptionId());
                System.out.println("  billingMonth: " + seededBill.getBillingMonth());
                System.out.println("  planRental: " + seededBill.getPlanRental());
                System.out.println("  usageCharges: " + seededBill.getUsageCharges());
                System.out.println("  taxAmount: " + seededBill.getTaxAmount());
                System.out.println("  discount: " + seededBill.getDiscount());
                System.out.println("  totalAmount: " + seededBill.getTotalAmount());
                System.out.println("  dueDate: " + seededBill.getDueDate());
                System.out.println("  billStatus: " + seededBill.getBillStatus());
                System.out.println("  createdAt: " + seededBill.getCreatedAt());
                System.out.println("  updatedAt: " + seededBill.getUpdatedAt());
            }

            // Test 2 - findByBillNumber using an existing seeded bill number
            Bill byBillNumber = billingDAO.findByBillNumber(SEEDED_BILL_NUMBER);
            boolean test2Pass = byBillNumber != null && SEEDED_BILL_ID.equals(byBillNumber.getBillId());
            System.out.println("Test 2 - findByBillNumber (seeded bill): " + (test2Pass ? "PASS" : "FAIL"));

            // Test 3 - findBySubscriptionIdAndBillingMonth: existing month and a non-existent month
            Bill byMonth = billingDAO.findBySubscriptionIdAndBillingMonth(SEEDED_SUBSCRIPTION_ID, SEEDED_BILLING_MONTH);
            boolean test3ExistingPass = byMonth != null && SEEDED_BILL_ID.equals(byMonth.getBillId());

            Bill byNonExistentMonth = billingDAO.findBySubscriptionIdAndBillingMonth(
                    SEEDED_SUBSCRIPTION_ID, NON_EXISTENT_BILLING_MONTH);
            boolean test3NonExistentPass = byNonExistentMonth == null;

            boolean test3Pass = test3ExistingPass && test3NonExistentPass;
            System.out.println("Test 3 - findBySubscriptionIdAndBillingMonth (seeded): "
                    + (test3Pass ? "PASS" : "FAIL"));

            // Test 4 - findBySubscriptionId using the seeded subscription
            List<Bill> bySubscription = billingDAO.findBySubscriptionId(SEEDED_SUBSCRIPTION_ID);
            boolean test4ListNotNull = bySubscription != null;
            boolean test4HasExpectedBill = test4ListNotNull && bySubscription.stream()
                    .anyMatch(b -> SEEDED_BILL_ID.equals(b.getBillId()));
            boolean test4Ordered = isChronologicallyOrdered(bySubscription);
            boolean test4Pass = test4ListNotNull && test4HasExpectedBill && test4Ordered;
            System.out.println("Test 4 - findBySubscriptionId (seeded): " + (test4Pass ? "PASS" : "FAIL"));
            System.out.println("  Bill count for subscription " + SEEDED_SUBSCRIPTION_ID + ": "
                    + (test4ListNotNull ? bySubscription.size() : 0));

            // Test 5 - findByStatus: a matching status and a non-matching status
            List<Bill> byStatus = billingDAO.findByStatus(SEEDED_STATUS);
            boolean test5ListNotNull = byStatus != null;
            boolean test5AllMatchStatus = test5ListNotNull
                    && byStatus.stream().allMatch(b -> SEEDED_STATUS.equals(b.getBillStatus()));
            boolean test5Ordered = isChronologicallyOrdered(byStatus);

            List<Bill> byNonMatchingStatus = billingDAO.findByStatus(NON_MATCHING_STATUS);
            boolean test5EmptyForNonMatch = byNonMatchingStatus != null && byNonMatchingStatus.isEmpty();

            boolean test5Pass = test5ListNotNull && test5AllMatchStatus && test5Ordered && test5EmptyForNonMatch;
            System.out.println("Test 5 - findByStatus (seeded): " + (test5Pass ? "PASS" : "FAIL"));
            System.out.println("  Bills with status '" + SEEDED_STATUS + "': "
                    + (test5ListNotNull ? byStatus.size() : 0));
            System.out.println("  Bills with non-matching status '" + NON_MATCHING_STATUS + "': "
                    + (byNonMatchingStatus != null ? byNonMatchingStatus.size() : -1));

            // Test 6 - save a temporary bill for the seeded subscription
            Bill tempBill = new Bill(TEMP_BILL_NUMBER, SEEDED_SUBSCRIPTION_ID, TEMP_BILLING_MONTH,
                    TEMP_PLAN_RENTAL, TEMP_TOTAL_AMOUNT, TEMP_DUE_DATE, TEMP_BILL_STATUS);
            tempBill.setUsageCharges(TEMP_USAGE_CHARGES);
            tempBill.setTaxAmount(TEMP_TAX_AMOUNT);
            tempBill.setDiscount(TEMP_DISCOUNT);

            billingDAO.save(tempBill);
            tempBillId = tempBill.getBillId();
            final Long generatedBillId = tempBillId;

            boolean test6Pass = tempBillId != null;
            System.out.println("Test 6 - save (temporary bill): " + (test6Pass ? "PASS" : "FAIL"));
            if (test6Pass) {
                System.out.println("  Generated billId: " + tempBillId);
            }

            // Test 7 - findById for the newly created bill, verifying all important fields
            Bill newlyCreated = billingDAO.findById(tempBillId);
            boolean test7Pass = newlyCreated != null
                    && TEMP_BILL_NUMBER.equals(newlyCreated.getBillNumber())
                    && SEEDED_SUBSCRIPTION_ID.equals(newlyCreated.getSubscriptionId())
                    && TEMP_BILLING_MONTH.equals(newlyCreated.getBillingMonth())
                    && TEMP_PLAN_RENTAL.compareTo(newlyCreated.getPlanRental()) == 0
                    && TEMP_USAGE_CHARGES.compareTo(newlyCreated.getUsageCharges()) == 0
                    && TEMP_TAX_AMOUNT.compareTo(newlyCreated.getTaxAmount()) == 0
                    && TEMP_DISCOUNT.compareTo(newlyCreated.getDiscount()) == 0
                    && TEMP_TOTAL_AMOUNT.compareTo(newlyCreated.getTotalAmount()) == 0
                    && TEMP_DUE_DATE.equals(newlyCreated.getDueDate())
                    && TEMP_BILL_STATUS.equals(newlyCreated.getBillStatus())
                    && newlyCreated.getCreatedAt() != null
                    && newlyCreated.getUpdatedAt() != null;
            System.out.println("Test 7 - findById (newly created bill, field verification): "
                    + (test7Pass ? "PASS" : "FAIL"));

            // Test 8 - findByBillNumber for the newly created bill
            Bill foundByTempNumber = billingDAO.findByBillNumber(TEMP_BILL_NUMBER);
            boolean test8Pass = foundByTempNumber != null && generatedBillId.equals(foundByTempNumber.getBillId());
            System.out.println("Test 8 - findByBillNumber (newly created bill): " + (test8Pass ? "PASS" : "FAIL"));

            // Test 9 - findBySubscriptionIdAndBillingMonth for the newly created bill
            Bill foundByTempMonth = billingDAO.findBySubscriptionIdAndBillingMonth(
                    SEEDED_SUBSCRIPTION_ID, TEMP_BILLING_MONTH);
            boolean test9Pass = foundByTempMonth != null && generatedBillId.equals(foundByTempMonth.getBillId());
            System.out.println("Test 9 - findBySubscriptionIdAndBillingMonth (newly created bill): "
                    + (test9Pass ? "PASS" : "FAIL"));

            // Test 10 - findBySubscriptionId after save: the temporary bill must appear
            List<Bill> afterSaveBySubscription = billingDAO.findBySubscriptionId(SEEDED_SUBSCRIPTION_ID);
            boolean test10Pass = afterSaveBySubscription != null
                    && afterSaveBySubscription.stream().anyMatch(b -> generatedBillId.equals(b.getBillId()));
            System.out.println("Test 10 - findBySubscriptionId after save: " + (test10Pass ? "PASS" : "FAIL"));

            // Test 11 - findByStatus after save: the temporary bill must appear under TEMP_BILL_STATUS
            List<Bill> afterSaveByStatus = billingDAO.findByStatus(TEMP_BILL_STATUS);
            boolean test11Pass = afterSaveByStatus != null
                    && afterSaveByStatus.stream().anyMatch(b -> generatedBillId.equals(b.getBillId()));
            System.out.println("Test 11 - findByStatus after save: " + (test11Pass ? "PASS" : "FAIL"));

            // Test 12 - update the temporary bill's mutable fields
            newlyCreated.setPlanRental(UPDATED_PLAN_RENTAL);
            newlyCreated.setUsageCharges(UPDATED_USAGE_CHARGES);
            newlyCreated.setTaxAmount(UPDATED_TAX_AMOUNT);
            newlyCreated.setDiscount(UPDATED_DISCOUNT);
            newlyCreated.setTotalAmount(UPDATED_TOTAL_AMOUNT);
            newlyCreated.setDueDate(UPDATED_DUE_DATE);
            newlyCreated.setBillStatus(UPDATED_BILL_STATUS);

            billingDAO.update(newlyCreated);

            Bill afterUpdate = billingDAO.findById(tempBillId);
            boolean test12Pass = afterUpdate != null
                    && UPDATED_PLAN_RENTAL.compareTo(afterUpdate.getPlanRental()) == 0
                    && UPDATED_USAGE_CHARGES.compareTo(afterUpdate.getUsageCharges()) == 0
                    && UPDATED_TAX_AMOUNT.compareTo(afterUpdate.getTaxAmount()) == 0
                    && UPDATED_DISCOUNT.compareTo(afterUpdate.getDiscount()) == 0
                    && UPDATED_TOTAL_AMOUNT.compareTo(afterUpdate.getTotalAmount()) == 0
                    && UPDATED_DUE_DATE.equals(afterUpdate.getDueDate())
                    && UPDATED_BILL_STATUS.equals(afterUpdate.getBillStatus())
                    && generatedBillId.equals(afterUpdate.getBillId())
                    && newlyCreated.getCreatedAt().equals(afterUpdate.getCreatedAt())
                    && afterUpdate.getUpdatedAt() != null;
            System.out.println("Test 12 - update (temporary bill): " + (test12Pass ? "PASS" : "FAIL"));

            // Test 13 - findByStatus after update: present under NEW status, absent under OLD status
            List<Bill> byNewStatus = billingDAO.findByStatus(UPDATED_BILL_STATUS);
            boolean test13PresentInNew = byNewStatus != null
                    && byNewStatus.stream().anyMatch(b -> generatedBillId.equals(b.getBillId()));

            List<Bill> byOldStatus = billingDAO.findByStatus(TEMP_BILL_STATUS);
            boolean test13AbsentFromOld = byOldStatus != null
                    && byOldStatus.stream().noneMatch(b -> generatedBillId.equals(b.getBillId()));

            boolean test13Pass = test13PresentInNew && test13AbsentFromOld;
            System.out.println("Test 13 - findByStatus after update: " + (test13Pass ? "PASS" : "FAIL"));

        } catch (Exception e) {
            System.out.println("Test run failed with an exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Test 14 - cleanup: BillingDAO deliberately has no delete method, so the temporary
            // row is removed here with a plain JDBC statement, not through the DAO.
            if (tempBillId != null) {
                boolean cleaned = deleteTestBillRow(tempBillId);
                System.out.println("Test 14 - cleanup (direct JDBC DELETE): " + (cleaned ? "PASS" : "FAIL"));

                // Test 15 - cleanup verification
                BillingDAO verifyDAO = new BillingDAOImpl();
                boolean test15Pass = verifyDAO.findById(tempBillId) == null
                        && verifyDAO.findByBillNumber(TEMP_BILL_NUMBER) == null;
                System.out.println("Test 15 - cleanup verification (no temporary bill remains): "
                        + (test15Pass ? "PASS" : "FAIL"));
            }
        }
    }

    private static boolean isChronologicallyOrdered(List<Bill> bills) {
        if (bills == null) {
            return false;
        }
        for (int i = 1; i < bills.size(); i++) {
            if (bills.get(i - 1).getBillingMonth().isAfter(bills.get(i).getBillingMonth())) {
                return false;
            }
        }
        return true;
    }

    private static boolean deleteTestBillRow(Long billId) {
        String sql = "DELETE FROM bills WHERE bill_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, billId);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Cleanup FAILED for bill ID: " + billId + " - " + e.getMessage());
            return false;
        }
    }
}
