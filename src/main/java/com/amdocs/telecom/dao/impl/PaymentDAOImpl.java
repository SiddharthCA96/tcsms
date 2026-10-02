package com.amdocs.telecom.dao.impl;

import com.amdocs.telecom.dao.PaymentDAO;
import com.amdocs.telecom.model.Payment;
import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of PaymentDAO.
 *
 * PaymentDAO only exposes persistence/retrieval operations for the payments table (findById,
 * findByTransactionReference, findByBillId, findByCustomerId, existsByTransactionReference,
 * save) - it has no methods for reading or updating a bill, or for writing an audit record. This
 * class therefore implements only payment persistence/retrieval. The full payment workflow
 * described by the case study (validate bill, validate amount, create payment, update bill
 * status, create audit record, commit/rollback as one transaction) touches bills and audit_logs
 * as well as payments, so it must be coordinated at the Service layer - e.g. a PaymentService that
 * opens one Connection, sets autoCommit(false), and calls BillingDAO/PaymentDAO (and an audit log
 * DAO, once one exists) against that same connection before committing or rolling back. Nothing
 * in this DAO performs that coordination.
 */
public class PaymentDAOImpl implements PaymentDAO {

    private static final String SELECT_COLUMNS =
            "payment_id, transaction_reference, bill_id, customer_id, amount, payment_mode, " +
            "payment_date, payment_status, created_at, updated_at";

    @Override
    public Payment findById(Long paymentId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM payments WHERE payment_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, paymentId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToPayment(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payment by ID: " + paymentId, e);
        }
    }

    @Override
    public Payment findByTransactionReference(String transactionReference) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM payments WHERE transaction_reference = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, transactionReference);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToPayment(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payment by transaction reference: "
                    + transactionReference, e);
        }
    }

    @Override
    public List<Payment> findByBillId(Long billId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM payments WHERE bill_id = ? " +
                "ORDER BY payment_date, payment_id";

        List<Payment> payments = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, billId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    payments.add(mapRowToPayment(resultSet));
                }
            }
            return payments;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payments for bill ID: " + billId, e);
        }
    }

    @Override
    public List<Payment> findByCustomerId(Long customerId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM payments WHERE customer_id = ? " +
                "ORDER BY payment_date, payment_id";

        List<Payment> payments = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    payments.add(mapRowToPayment(resultSet));
                }
            }
            return payments;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payments for customer ID: " + customerId, e);
        }
    }

    @Override
    public boolean existsByTransactionReference(String transactionReference) {
        // "SELECT 1 ... LIMIT 1" avoids pulling back a whole row just to check existence.
        String sql = "SELECT 1 FROM payments WHERE transaction_reference = ? LIMIT 1";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, transactionReference);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check existence of transaction reference: "
                    + transactionReference, e);
        }
    }

    @Override
    public void save(Payment payment) {
        // payment_id is AUTO_INCREMENT. payment_date and created_at/updated_at default to
        // CURRENT_TIMESTAMP in the schema, but if the caller already supplied a payment_date,
        // that value is preserved instead of relying on the database default.
        boolean paymentDateSupplied = payment.getPaymentDate() != null;

        String sql = paymentDateSupplied
                ? "INSERT INTO payments (transaction_reference, bill_id, customer_id, amount, " +
                        "payment_mode, payment_date, payment_status) VALUES (?, ?, ?, ?, ?, ?, ?)"
                : "INSERT INTO payments (transaction_reference, bill_id, customer_id, amount, " +
                        "payment_mode, payment_status) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, payment.getTransactionReference());
            statement.setLong(2, payment.getBillId());
            statement.setLong(3, payment.getCustomerId());
            statement.setBigDecimal(4, payment.getAmount());
            statement.setString(5, payment.getPaymentMode());

            if (paymentDateSupplied) {
                statement.setTimestamp(6, Timestamp.valueOf(payment.getPaymentDate()));
                statement.setString(7, payment.getPaymentStatus());
            } else {
                statement.setString(6, payment.getPaymentStatus());
            }

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    payment.setPaymentId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save payment with transaction reference: "
                    + payment.getTransactionReference(), e);
        }
    }

    private Payment mapRowToPayment(ResultSet resultSet) throws SQLException {
        Payment payment = new Payment();
        payment.setPaymentId(resultSet.getLong("payment_id"));
        payment.setTransactionReference(resultSet.getString("transaction_reference"));
        payment.setBillId(resultSet.getLong("bill_id"));
        payment.setCustomerId(resultSet.getLong("customer_id"));
        payment.setAmount(resultSet.getBigDecimal("amount"));
        payment.setPaymentMode(resultSet.getString("payment_mode"));
        payment.setPaymentDate(resultSet.getTimestamp("payment_date").toLocalDateTime());
        payment.setPaymentStatus(resultSet.getString("payment_status"));
        payment.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        payment.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
        return payment;
    }
}
