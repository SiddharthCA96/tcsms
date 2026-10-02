package com.amdocs.telecom.dao;

// import com.amdocs.telecom.dao.PaymentDAO;
import com.amdocs.telecom.dao.impl.PaymentDAOImpl;
import com.amdocs.telecom.model.Payment;
import com.amdocs.telecom.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Manual integration test for PaymentDAO / PaymentDAOImpl against the real MySQL database.
 *
 * This test covers ONLY the persistence/retrieval methods PaymentDAO currently exposes
 * (findById, findByTransactionReference, findByBillId, findByCustomerId,
 * existsByTransactionReference, save). It deliberately does NOT exercise the full payment
 * transaction workflow (bill validation, amount validation, bill status update, audit record
 * creation, commit/rollback) - that belongs to a future Service-layer class using a shared JDBC
 * Connection. No seeded bill status or seeded payment is modified by this test.
 *
 * database/seed_data.sql already contains payments for bill_id 1 (customer 1) and bill_id 3
 * (customer 2), so findById/findByTransactionReference/existsByTransactionReference/findByBillId/
 * findByCustomerId are first exercised against that seeded data. A temporary Payment is then
 * created via save() - reusing seeded bill_id 1 and customer_id 1 - to exercise the write path,
 * the re-reads, and the UNIQUE constraint on transaction_reference. PaymentDAO intentionally
 * exposes no delete method, so the temporary row is removed at the end with a direct, test-only
 * JDBC statement - this cleanup logic is NOT part of the DAO or its implementation.
 */
public class PaymentDAOTest {

    // Reused seeded values from database/seed_data.sql
    private static final Long SEEDED_PAYMENT_ID = 1L;
    private static final String SEEDED_TRANSACTION_REFERENCE = "TXN-202608-0001";
    private static final Long SEEDED_BILL_ID = 1L;
    private static final Long SEEDED_CUSTOMER_ID = 1L;
    private static final String NON_EXISTENT_TRANSACTION_REFERENCE = "TXN-DAO-TEST-DOES-NOT-EXIST";

    // Temporary payment used to test save() and the re-reads
    private static final String TEMP_TRANSACTION_REFERENCE = "TXN-DAO-TEST-0001";
    private static final BigDecimal TEMP_AMOUNT = new BigDecimal("500.00");
    private static final String TEMP_PAYMENT_MODE = "UPI";
    private static final String TEMP_PAYMENT_STATUS = "SUCCESS";
    private static final LocalDateTime TEMP_PAYMENT_DATE = LocalDateTime.of(2026, 9, 15, 10, 0, 0);

    public static void main(String[] args) {
        PaymentDAO paymentDAO = new PaymentDAOImpl();

        System.out.println("=== PaymentDAO Test ===");

        Long tempPaymentId = null;

        try {
            // Test 1 - findById using an existing seeded payment
            Payment seededPayment = paymentDAO.findById(SEEDED_PAYMENT_ID);
            boolean test1Pass = seededPayment != null;
            System.out.println("Test 1 - findById (seeded payment): " + (test1Pass ? "PASS" : "FAIL"));
            if (test1Pass) {
                System.out.println("  paymentId: " + seededPayment.getPaymentId());
                System.out.println("  transactionReference: " + seededPayment.getTransactionReference());
                System.out.println("  billId: " + seededPayment.getBillId());
                System.out.println("  customerId: " + seededPayment.getCustomerId());
                System.out.println("  amount: " + seededPayment.getAmount());
                System.out.println("  paymentMode: " + seededPayment.getPaymentMode());
                System.out.println("  paymentDate: " + seededPayment.getPaymentDate());
                System.out.println("  paymentStatus: " + seededPayment.getPaymentStatus());
                System.out.println("  createdAt: " + seededPayment.getCreatedAt());
                System.out.println("  updatedAt: " + seededPayment.getUpdatedAt());
            }

            // Test 2 - findByTransactionReference using an existing seeded transaction reference
            Payment byTransactionReference = paymentDAO.findByTransactionReference(SEEDED_TRANSACTION_REFERENCE);
            boolean test2Pass = byTransactionReference != null
                    && SEEDED_PAYMENT_ID.equals(byTransactionReference.getPaymentId());
            System.out.println("Test 2 - findByTransactionReference (seeded payment): "
                    + (test2Pass ? "PASS" : "FAIL"));

            // Test 3 - existsByTransactionReference: existing reference and a non-existing one
            boolean existsForSeeded = paymentDAO.existsByTransactionReference(SEEDED_TRANSACTION_REFERENCE);
            boolean existsForNonExistent = paymentDAO.existsByTransactionReference(
                    NON_EXISTENT_TRANSACTION_REFERENCE);
            boolean test3Pass = existsForSeeded && !existsForNonExistent;
            System.out.println("Test 3 - existsByTransactionReference (seeded true / unknown false): "
                    + (test3Pass ? "PASS" : "FAIL"));

            // Test 4 - findByBillId using a seeded bill that has payments
            List<Payment> byBillId = paymentDAO.findByBillId(SEEDED_BILL_ID);
            boolean test4ListNotNull = byBillId != null;
            boolean test4HasExpectedPayment = test4ListNotNull && byBillId.stream()
                    .anyMatch(p -> SEEDED_PAYMENT_ID.equals(p.getPaymentId()));
            boolean test4Ordered = isChronologicallyOrdered(byBillId);
            boolean test4Pass = test4ListNotNull && test4HasExpectedPayment && test4Ordered;
            System.out.println("Test 4 - findByBillId (seeded): " + (test4Pass ? "PASS" : "FAIL"));
            System.out.println("  Payment count for bill " + SEEDED_BILL_ID + ": "
                    + (test4ListNotNull ? byBillId.size() : 0));

            // Test 5 - findByCustomerId using a seeded customer that has payments
            List<Payment> byCustomerId = paymentDAO.findByCustomerId(SEEDED_CUSTOMER_ID);
            boolean test5ListNotNull = byCustomerId != null;
            boolean test5HasExpectedPayment = test5ListNotNull && byCustomerId.stream()
                    .anyMatch(p -> SEEDED_PAYMENT_ID.equals(p.getPaymentId()));
            boolean test5Ordered = isChronologicallyOrdered(byCustomerId);
            boolean test5Pass = test5ListNotNull && test5HasExpectedPayment && test5Ordered;
            System.out.println("Test 5 - findByCustomerId (seeded): " + (test5Pass ? "PASS" : "FAIL"));
            System.out.println("  Payment count for customer " + SEEDED_CUSTOMER_ID + ": "
                    + (test5ListNotNull ? byCustomerId.size() : 0));

            // Test 6 - save a temporary payment, reusing seeded bill_id and customer_id
            Payment tempPayment = new Payment(TEMP_TRANSACTION_REFERENCE, SEEDED_BILL_ID, SEEDED_CUSTOMER_ID,
                    TEMP_AMOUNT, TEMP_PAYMENT_MODE, TEMP_PAYMENT_STATUS);
            tempPayment.setPaymentDate(TEMP_PAYMENT_DATE);

            paymentDAO.save(tempPayment);
            tempPaymentId = tempPayment.getPaymentId();
            final Long generatedPaymentId = tempPaymentId;

            boolean test6Pass = tempPaymentId != null;
            System.out.println("Test 6 - save (temporary payment): " + (test6Pass ? "PASS" : "FAIL"));
            if (test6Pass) {
                System.out.println("  Generated paymentId: " + tempPaymentId);
            }

            // Test 7 - findById for the newly created payment, verifying all important fields
            Payment newlyCreated = paymentDAO.findById(tempPaymentId);
            boolean test7Pass = newlyCreated != null
                    && TEMP_TRANSACTION_REFERENCE.equals(newlyCreated.getTransactionReference())
                    && SEEDED_BILL_ID.equals(newlyCreated.getBillId())
                    && SEEDED_CUSTOMER_ID.equals(newlyCreated.getCustomerId())
                    && TEMP_AMOUNT.compareTo(newlyCreated.getAmount()) == 0
                    && TEMP_PAYMENT_MODE.equals(newlyCreated.getPaymentMode())
                    && TEMP_PAYMENT_DATE.equals(newlyCreated.getPaymentDate())
                    && TEMP_PAYMENT_STATUS.equals(newlyCreated.getPaymentStatus())
                    && newlyCreated.getCreatedAt() != null
                    && newlyCreated.getUpdatedAt() != null;
            System.out.println("Test 7 - findById (newly created payment, field verification): "
                    + (test7Pass ? "PASS" : "FAIL"));

            // Test 8 - findByTransactionReference for the newly created payment
            Payment foundByTempReference = paymentDAO.findByTransactionReference(TEMP_TRANSACTION_REFERENCE);
            boolean test8Pass = foundByTempReference != null
                    && generatedPaymentId.equals(foundByTempReference.getPaymentId());
            System.out.println("Test 8 - findByTransactionReference (newly created payment): "
                    + (test8Pass ? "PASS" : "FAIL"));

            // Test 9 - existsByTransactionReference after save
            boolean test9Pass = paymentDAO.existsByTransactionReference(TEMP_TRANSACTION_REFERENCE);
            System.out.println("Test 9 - existsByTransactionReference after save: " + (test9Pass ? "PASS" : "FAIL"));

            // Test 10 - findByBillId after save: the temporary payment must appear
            List<Payment> afterSaveByBillId = paymentDAO.findByBillId(SEEDED_BILL_ID);
            boolean test10Pass = afterSaveByBillId != null
                    && afterSaveByBillId.stream().anyMatch(p -> generatedPaymentId.equals(p.getPaymentId()));
            System.out.println("Test 10 - findByBillId after save: " + (test10Pass ? "PASS" : "FAIL"));

            // Test 11 - findByCustomerId after save: the temporary payment must appear
            List<Payment> afterSaveByCustomerId = paymentDAO.findByCustomerId(SEEDED_CUSTOMER_ID);
            boolean test11Pass = afterSaveByCustomerId != null
                    && afterSaveByCustomerId.stream().anyMatch(p -> generatedPaymentId.equals(p.getPaymentId()));
            System.out.println("Test 11 - findByCustomerId after save: " + (test11Pass ? "PASS" : "FAIL"));

            // Test 12 - duplicate transaction reference protection
            boolean existsBeforeDuplicate = paymentDAO.existsByTransactionReference(TEMP_TRANSACTION_REFERENCE);
            boolean duplicateRejected = false;
            if (existsBeforeDuplicate) {
                Payment duplicatePayment = new Payment(TEMP_TRANSACTION_REFERENCE, SEEDED_BILL_ID,
                        SEEDED_CUSTOMER_ID, TEMP_AMOUNT, TEMP_PAYMENT_MODE, TEMP_PAYMENT_STATUS);
                try {
                    paymentDAO.save(duplicatePayment);
                } catch (RuntimeException e) {
                    duplicateRejected = true;
                }
            }
            boolean test12Pass = existsBeforeDuplicate && duplicateRejected;
            System.out.println("Test 12 - duplicate transaction reference protection: "
                    + (test12Pass ? "PASS" : "FAIL"));

        } catch (Exception e) {
            System.out.println("Test run failed with an exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup: PaymentDAO deliberately has no delete method, so the temporary row is
            // removed here with a plain JDBC statement, not through the DAO.
            if (tempPaymentId != null) {
                boolean cleaned = deleteTestPaymentRow(tempPaymentId);
                System.out.println("Cleanup (direct JDBC DELETE): " + (cleaned ? "PASS" : "FAIL"));

                // Test 13 - cleanup verification
                PaymentDAO verifyDAO = new PaymentDAOImpl();
                boolean test13Pass = verifyDAO.findById(tempPaymentId) == null
                        && verifyDAO.findByTransactionReference(TEMP_TRANSACTION_REFERENCE) == null
                        && !verifyDAO.existsByTransactionReference(TEMP_TRANSACTION_REFERENCE);
                System.out.println("Test 13 - cleanup verification (no temporary payment remains): "
                        + (test13Pass ? "PASS" : "FAIL"));
            }
        }
    }

    private static boolean isChronologicallyOrdered(List<Payment> payments) {
        if (payments == null) {
            return false;
        }
        for (int i = 1; i < payments.size(); i++) {
            if (payments.get(i - 1).getPaymentDate().isAfter(payments.get(i).getPaymentDate())) {
                return false;
            }
        }
        return true;
    }

    private static boolean deleteTestPaymentRow(Long paymentId) {
        String sql = "DELETE FROM payments WHERE payment_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, paymentId);
            statement.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Cleanup FAILED for payment ID: " + paymentId + " - " + e.getMessage());
            return false;
        }
    }
}
