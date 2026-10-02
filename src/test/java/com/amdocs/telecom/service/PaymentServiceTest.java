package com.amdocs.telecom.service;

import com.amdocs.telecom.dao.AuditLogDAO;
import com.amdocs.telecom.dao.BillingDAO;
import com.amdocs.telecom.dao.PaymentDAO;
import com.amdocs.telecom.dao.impl.AuditLogDAOImpl;
import com.amdocs.telecom.dao.impl.BillingDAOImpl;
import com.amdocs.telecom.dao.impl.PaymentDAOImpl;
import com.amdocs.telecom.model.AuditLog;
import com.amdocs.telecom.model.Bill;
import com.amdocs.telecom.model.Payment;
import com.amdocs.telecom.service.impl.PaymentServiceImpl;
import com.amdocs.telecom.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Integration test for PaymentServiceImpl against the real MySQL database.
 *
 * Unlike the other Service tests, this one deliberately does NOT use a fake/in-memory DAO:
 * processPayment()'s whole point is a real JDBC transaction (commit/rollback across payment
 * creation, bill status update and audit record creation), which a fake DAO cannot meaningfully
 * exercise. Every bill used here is a TEMPORARY row created by this test for an existing seeded
 * subscription/customer (never a seeded bill), and is removed - along with any payment/audit rows
 * it produced - via direct JDBC DELETE in a finally block. No seeded row is ever modified or
 * deleted.
 */
public class PaymentServiceTest {

    // Reused seeded foreign keys from database/seed_data.sql (subscription -> its customer)
    private static final Long SUBSCRIPTION_1 = 1L; // customer 1
    private static final Long CUSTOMER_FOR_SUBSCRIPTION_1 = 1L;
    private static final Long SUBSCRIPTION_2 = 2L; // customer 1
    private static final Long CUSTOMER_FOR_SUBSCRIPTION_2 = 1L;
    private static final Long SUBSCRIPTION_3 = 3L; // customer 2
    private static final Long CUSTOMER_FOR_SUBSCRIPTION_3 = 2L;
    private static final Long SUBSCRIPTION_4 = 4L; // customer 3
    private static final Long CUSTOMER_FOR_SUBSCRIPTION_4 = 3L;

    private static int testsExecuted = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("PaymentServiceTest (real MySQL database)");
        System.out.println("========================================");

        PaymentDAO paymentDAO = new PaymentDAOImpl();
        BillingDAO billingDAO = new BillingDAOImpl();
        AuditLogDAO auditLogDAO = new AuditLogDAOImpl();
        PaymentService service = new PaymentServiceImpl(paymentDAO);

        List<Long> tempBillIds = new ArrayList<>();

        try {
            // ---- Tests 1-3: successful payment, bill status update, audit record creation ----
            Bill bill1 = createTempBill(billingDAO, SUBSCRIPTION_1, LocalDate.of(2027, 1, 1),
                    new BigDecimal("500.00"));
            tempBillIds.add(bill1.getBillId());

            String txnRef1 = "TEST-TXN-SUCCESS-" + System.nanoTime();
            Payment payment1 = service.processPayment(bill1.getBillId(), CUSTOMER_FOR_SUBSCRIPTION_1,
                    new BigDecimal("500.00"), "UPI", txnRef1);

            boolean test1Pass = payment1 != null && payment1.getPaymentId() != null
                    && "SUCCESS".equals(payment1.getPaymentStatus())
                    && txnRef1.equals(payment1.getTransactionReference())
                    && new BigDecimal("500.00").compareTo(payment1.getAmount()) == 0;
            check("Test 1 - processPayment() successful payment created", test1Pass);

            Bill billAfterPayment = billingDAO.findById(bill1.getBillId());
            check("Test 2 - processPayment() updates bill status to PAID",
                    billAfterPayment != null && "PAID".equals(billAfterPayment.getBillStatus()));

            List<AuditLog> auditForBill1 = auditLogDAO.findByEntity("BILL", bill1.getBillId());
            boolean auditCreated = auditForBill1.stream().anyMatch(a ->
                    "PAYMENT_PROCESSED".equals(a.getAction()) && CUSTOMER_FOR_SUBSCRIPTION_1.equals(a.getActorId()));
            check("Test 3 - processPayment() creates an audit record for the bill", auditCreated);

            // ---- Test 4: invalid (nonexistent) bill ----
            String txnRef4 = "TEST-TXN-INVALIDBILL-" + System.nanoTime();
            RuntimeException invalidBillException = expectRuntimeException(() ->
                    service.processPayment(999_999L, CUSTOMER_FOR_SUBSCRIPTION_1, new BigDecimal("100.00"),
                            "UPI", txnRef4));
            check("Test 4 - processPayment() rejects a nonexistent bill", invalidBillException != null);

            // ---- Test 5: invalid amount (does not match bill total) ----
            Bill bill5 = createTempBill(billingDAO, SUBSCRIPTION_2, LocalDate.of(2027, 2, 1),
                    new BigDecimal("300.00"));
            tempBillIds.add(bill5.getBillId());

            String txnRef5 = "TEST-TXN-BADAMOUNT-" + System.nanoTime();
            RuntimeException invalidAmountException = expectRuntimeException(() ->
                    service.processPayment(bill5.getBillId(), CUSTOMER_FOR_SUBSCRIPTION_2, new BigDecimal("999.99"),
                            "UPI", txnRef5));

            Bill bill5AfterRejectedPayment = billingDAO.findById(bill5.getBillId());
            check("Test 5 - processPayment() rejects a mismatched amount and leaves bill UNPAID",
                    invalidAmountException != null
                            && "UNPAID".equals(bill5AfterRejectedPayment.getBillStatus()));

            // ---- Test 6: duplicate transaction reference ----
            Bill bill6a = createTempBill(billingDAO, SUBSCRIPTION_3, LocalDate.of(2027, 3, 1),
                    new BigDecimal("250.00"));
            tempBillIds.add(bill6a.getBillId());
            Bill bill6b = createTempBill(billingDAO, SUBSCRIPTION_4, LocalDate.of(2027, 4, 1),
                    new BigDecimal("250.00"));
            tempBillIds.add(bill6b.getBillId());

            String sharedTxnRef = "TEST-TXN-DUPLICATE-" + System.nanoTime();
            service.processPayment(bill6a.getBillId(), CUSTOMER_FOR_SUBSCRIPTION_3, new BigDecimal("250.00"),
                    "CARD", sharedTxnRef);

            RuntimeException duplicateException = expectRuntimeException(() ->
                    service.processPayment(bill6b.getBillId(), CUSTOMER_FOR_SUBSCRIPTION_4, new BigDecimal("250.00"),
                            "CARD", sharedTxnRef));

            Bill bill6bAfterRejectedPayment = billingDAO.findById(bill6b.getBillId());
            check("Test 6 - processPayment() rejects a duplicate transaction reference and leaves "
                            + "the second bill UNPAID",
                    duplicateException != null && "UNPAID".equals(bill6bAfterRejectedPayment.getBillStatus()));

            // ---- Test 7: rollback behavior ----
            Bill bill7 = createTempBill(billingDAO, SUBSCRIPTION_1, LocalDate.of(2027, 5, 1),
                    new BigDecimal("400.00"));
            tempBillIds.add(bill7.getBillId());

            String txnRef7Failed = "TEST-TXN-ROLLBACK-FAIL-" + System.nanoTime();
            // customerId 999_999 does not exist -> violates payments.fk_payment_customer mid-transaction.
            RuntimeException rollbackException = expectRuntimeException(() ->
                    service.processPayment(bill7.getBillId(), 999_999L, new BigDecimal("400.00"), "UPI",
                            txnRef7Failed));

            Bill bill7AfterFailedPayment = billingDAO.findById(bill7.getBillId());
            boolean billUntouchedAfterFailure = bill7AfterFailedPayment != null
                    && "UNPAID".equals(bill7AfterFailedPayment.getBillStatus());
            boolean noOrphanPayment = paymentDAO.findByBillId(bill7.getBillId()).isEmpty();

            // A follow-up valid payment on the same bill must still succeed, proving the failed
            // attempt left no partial/corrupted state behind.
            String txnRef7Valid = "TEST-TXN-ROLLBACK-RETRY-" + System.nanoTime();
            Payment retryPayment = service.processPayment(bill7.getBillId(), CUSTOMER_FOR_SUBSCRIPTION_1,
                    new BigDecimal("400.00"), "UPI", txnRef7Valid);
            Bill bill7AfterRetry = billingDAO.findById(bill7.getBillId());

            check("Test 7 - rollback behavior: failed mid-transaction payment leaves bill/payments "
                            + "untouched, and the bill remains payable afterward",
                    rollbackException != null && billUntouchedAfterFailure && noOrphanPayment
                            && retryPayment.getPaymentId() != null
                            && "PAID".equals(bill7AfterRetry.getBillStatus()));

        } catch (Exception e) {
            System.out.println("Test run failed with an exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            boolean cleaned = true;
            for (Long billId : tempBillIds) {
                cleaned &= cleanupBill(billId);
            }
            System.out.println("Cleanup (temporary bills, payments and audit logs removed): "
                    + (cleaned ? "PASS" : "FAIL"));

            System.out.println("========================================");
            System.out.println("Tests executed: " + testsExecuted);
            System.out.println("Tests passed: " + testsPassed);
            System.out.println("Tests failed: " + (testsExecuted - testsPassed));
            System.out.println("========================================");
        }
    }

    private static void check(String testName, boolean condition) {
        testsExecuted++;
        if (condition) {
            testsPassed++;
            System.out.println(testName + ": PASS");
        } else {
            System.out.println(testName + ": FAIL");
        }
    }

    private interface ThrowingAction {
        void run();
    }

    private static RuntimeException expectRuntimeException(ThrowingAction action) {
        try {
            action.run();
            return null;
        } catch (RuntimeException e) {
            return e;
        }
    }

    private static Bill createTempBill(BillingDAO billingDAO, Long subscriptionId, LocalDate billingMonth,
                                        BigDecimal totalAmount) {
        Bill bill = new Bill("DAO-TEST-BILL-" + subscriptionId + "-" + billingMonth, subscriptionId, billingMonth,
                totalAmount, totalAmount, billingMonth.plusDays(19), "UNPAID");
        bill.setUsageCharges(BigDecimal.ZERO);
        bill.setTaxAmount(BigDecimal.ZERO);
        bill.setDiscount(BigDecimal.ZERO);
        billingDAO.save(bill);
        return bill;
    }

    /** Deletes a temporary bill's payments, audit records and the bill row itself. Never touches seeded data. */
    private static boolean cleanupBill(Long billId) {
        try (Connection connection = DBConnection.getConnection()) {
            executeDelete(connection, "DELETE FROM audit_logs WHERE entity_type = 'BILL' AND entity_id = ?", billId);
            executeDelete(connection, "DELETE FROM payments WHERE bill_id = ?", billId);
            executeDelete(connection, "DELETE FROM bills WHERE bill_id = ?", billId);
            return true;
        } catch (SQLException e) {
            System.out.println("Cleanup FAILED for bill ID: " + billId + " - " + e.getMessage());
            return false;
        }
    }

    private static void executeDelete(Connection connection, String sql, Long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }
}
