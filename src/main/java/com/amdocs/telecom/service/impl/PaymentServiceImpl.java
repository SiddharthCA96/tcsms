package com.amdocs.telecom.service.impl;

import com.amdocs.telecom.dao.PaymentDAO;
import com.amdocs.telecom.model.Payment;
import com.amdocs.telecom.service.PaymentService;
import com.amdocs.telecom.util.DBConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * Business logic implementation of PaymentService.
 *
 * ARCHITECTURAL NOTE - why processPayment() does not call PaymentDAO.save() / BillingDAO.update()
 * / AuditLogDAO.save():
 *
 * Each of those methods calls DBConnection.getConnection() itself, and DBConnection never sets
 * autoCommit(false) - every DAO call runs on its own connection and commits immediately and
 * independently. There is no way to compose three such calls into one atomic unit: if the third
 * call failed after the first two had already committed on their own connections, there would be
 * nothing left to roll back. The case study requires exactly that atomicity (create payment,
 * update bill status, create audit record, commit/rollback together), so this one operation opens
 * a single Connection, disables autoCommit, performs the equivalent INSERT/UPDATE/INSERT itself
 * via PreparedStatement against that one connection, and commits or rolls back as a unit. This
 * mirrors the general project rule that a service may use PreparedStatement directly "when the
 * case study explicitly requires a service-level transaction that cannot be delegated" - this is
 * that case. No DAO interface or implementation was modified to make this possible.
 *
 * Every other PaymentService method is a simple read and delegates to PaymentDAO as normal.
 *
 * ASSUMPTION: the schema has no partial-payment/balance column, and bill_status has no
 * "PARTIALLY_PAID" value - a payment is therefore required to exactly match the bill's
 * totalAmount (full payment only).
 */
public class PaymentServiceImpl implements PaymentService {

    private static final String PAYMENT_STATUS_SUCCESS = "SUCCESS";
    private static final String BILL_STATUS_PAID = "PAID";
    private static final String AUDIT_ACTOR_TYPE_CUSTOMER = "CUSTOMER";
    private static final String AUDIT_ACTION_PAYMENT_PROCESSED = "PAYMENT_PROCESSED";
    private static final String AUDIT_ENTITY_TYPE_BILL = "BILL";

    // BillingDAO/AuditLogDAO are deliberately NOT injected: the transactional bill-status update
    // and audit-record creation below run as raw SQL on processPayment()'s own shared Connection
    // (see the class Javadoc), not through those DAOs, which each open their own connection.
    private final PaymentDAO paymentDAO;

    public PaymentServiceImpl(PaymentDAO paymentDAO) {
        this.paymentDAO = paymentDAO;
    }

    @Override
    public Payment processPayment(Long billId, Long customerId, BigDecimal amount, String paymentMode,
                                   String transactionReference) {
        if (billId == null) {
            throw new RuntimeException("Failed to process payment: billId must not be null.");
        }
        if (customerId == null) {
            throw new RuntimeException("Failed to process payment: customerId must not be null.");
        }
        if (paymentMode == null || paymentMode.trim().isEmpty()) {
            throw new RuntimeException("Failed to process payment: paymentMode must not be null or empty.");
        }
        if (transactionReference == null || transactionReference.trim().isEmpty()) {
            throw new RuntimeException("Failed to process payment: transactionReference must not be null or empty.");
        }

        // Fast duplicate check up front (the transaction_reference UNIQUE constraint is the real
        // safety net against a race, enforced below when the INSERT runs inside the transaction).
        if (paymentDAO.existsByTransactionReference(transactionReference)) {
            throw new RuntimeException("Failed to process payment: transaction reference '"
                    + transactionReference + "' has already been used.");
        }

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                // 1. Validate bill (locks the row for the duration of this transaction).
                BillSnapshot bill = findBillForUpdate(connection, billId);
                if (BILL_STATUS_PAID.equals(bill.billStatus)) {
                    throw new RuntimeException("Bill ID " + billId + " is already PAID.");
                }

                // 2. Validate amount.
                if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("Payment amount must be greater than zero.");
                }
                if (amount.compareTo(bill.totalAmount) != 0) {
                    throw new RuntimeException("Payment amount " + amount
                            + " does not match bill total amount " + bill.totalAmount + ".");
                }

                // 3. Create payment.
                Long paymentId = insertPayment(connection, transactionReference, billId, customerId, amount,
                        paymentMode);

                // 4. Update bill status.
                updateBillStatusToPaid(connection, billId);

                // 5. Create audit record.
                insertAuditLog(connection, customerId, billId, "Payment " + transactionReference
                        + " of " + amount + " applied to bill ID " + billId + ".");

                // 6. Commit.
                connection.commit();

                Payment payment = new Payment(transactionReference, billId, customerId, amount, paymentMode,
                        PAYMENT_STATUS_SUCCESS);
                payment.setPaymentId(paymentId);
                return payment;

            } catch (Exception e) {
                rollbackQuietly(connection);
                throw new RuntimeException("Failed to process payment for bill ID " + billId
                        + ": " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to process payment for bill ID " + billId
                    + ": could not open a database transaction.", e);
        }
    }

    @Override
    public Payment getPaymentById(Long paymentId) {
        if (paymentId == null) {
            throw new RuntimeException("Failed to get payment: paymentId must not be null.");
        }

        Payment payment = paymentDAO.findById(paymentId);
        if (payment == null) {
            throw new RuntimeException("Failed to get payment: no payment found with ID: " + paymentId);
        }
        return payment;
    }

    @Override
    public Optional<Payment> findPaymentByTransactionReference(String transactionReference) {
        if (transactionReference == null || transactionReference.trim().isEmpty()) {
            throw new RuntimeException(
                    "Failed to find payment: transactionReference must not be null or empty.");
        }
        return Optional.ofNullable(paymentDAO.findByTransactionReference(transactionReference));
    }

    @Override
    public List<Payment> getBillPayments(Long billId) {
        if (billId == null) {
            throw new RuntimeException("Failed to get bill payments: billId must not be null.");
        }
        return paymentDAO.findByBillId(billId);
    }

    @Override
    public List<Payment> getCustomerPayments(Long customerId) {
        if (customerId == null) {
            throw new RuntimeException("Failed to get customer payments: customerId must not be null.");
        }
        return paymentDAO.findByCustomerId(customerId);
    }

    // ---- transactional helpers (bound to the caller-supplied Connection; not routed through DAOs) ----

    private static final class BillSnapshot {
        final BigDecimal totalAmount;
        final String billStatus;

        BillSnapshot(BigDecimal totalAmount, String billStatus) {
            this.totalAmount = totalAmount;
            this.billStatus = billStatus;
        }
    }

    private BillSnapshot findBillForUpdate(Connection connection, Long billId) throws SQLException {
        String sql = "SELECT total_amount, bill_status FROM bills WHERE bill_id = ? FOR UPDATE";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, billId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new RuntimeException("No bill found with ID: " + billId);
                }
                return new BillSnapshot(resultSet.getBigDecimal("total_amount"),
                        resultSet.getString("bill_status"));
            }
        }
    }

    private Long insertPayment(Connection connection, String transactionReference, Long billId, Long customerId,
                                BigDecimal amount, String paymentMode) throws SQLException {
        String sql = "INSERT INTO payments (transaction_reference, bill_id, customer_id, amount, "
                + "payment_mode, payment_status) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, transactionReference);
            statement.setLong(2, billId);
            statement.setLong(3, customerId);
            statement.setBigDecimal(4, amount);
            statement.setString(5, paymentMode);
            statement.setString(6, PAYMENT_STATUS_SUCCESS);

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
                throw new RuntimeException("Payment insert did not return a generated ID.");
            }
        }
    }

    private void updateBillStatusToPaid(Connection connection, Long billId) throws SQLException {
        String sql = "UPDATE bills SET bill_status = ?, updated_at = CURRENT_TIMESTAMP WHERE bill_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, BILL_STATUS_PAID);
            statement.setLong(2, billId);
            statement.executeUpdate();
        }
    }

    private void insertAuditLog(Connection connection, Long customerId, Long billId, String description)
            throws SQLException {
        String sql = "INSERT INTO audit_logs (actor_type, actor_id, action, entity_type, entity_id, description) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, AUDIT_ACTOR_TYPE_CUSTOMER);
            statement.setLong(2, customerId);
            statement.setString(3, AUDIT_ACTION_PAYMENT_PROCESSED);
            statement.setString(4, AUDIT_ENTITY_TYPE_BILL);
            statement.setLong(5, billId);
            statement.setString(6, description);
            statement.executeUpdate();
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            // The original failure is what gets reported to the caller; a failed rollback on an
            // already-broken connection must not replace or hide it.
        }
    }
}
