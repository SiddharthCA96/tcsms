package com.amdocs.telecom.dao.impl;

import com.amdocs.telecom.dao.BillingDAO;
import com.amdocs.telecom.model.Bill;
import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of BillingDAO.
 * This DAO is responsible only for bill persistence and retrieval; the transactional payment
 * workflow (applying payments, updating bill_status accordingly) belongs to PaymentService /
 * PaymentDAO, not here.
 */
public class BillingDAOImpl implements BillingDAO {

    private static final String SELECT_COLUMNS =
            "bill_id, bill_number, subscription_id, billing_month, plan_rental, usage_charges, " +
            "tax_amount, discount, total_amount, due_date, bill_status, created_at, updated_at";

    @Override
    public Bill findById(Long billId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM bills WHERE bill_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, billId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToBill(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find bill by ID: " + billId, e);
        }
    }

    @Override
    public Bill findByBillNumber(String billNumber) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM bills WHERE bill_number = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, billNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToBill(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find bill by bill number: " + billNumber, e);
        }
    }

    @Override
    public Bill findBySubscriptionIdAndBillingMonth(Long subscriptionId, LocalDate billingMonth) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM bills WHERE subscription_id = ? AND billing_month = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, subscriptionId);
            statement.setDate(2, Date.valueOf(billingMonth));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToBill(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find bill for subscription ID: " + subscriptionId
                    + " and billing month: " + billingMonth, e);
        }
    }

    @Override
    public List<Bill> findBySubscriptionId(Long subscriptionId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM bills WHERE subscription_id = ? " +
                "ORDER BY billing_month";

        List<Bill> bills = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, subscriptionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    bills.add(mapRowToBill(resultSet));
                }
            }
            return bills;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find bills for subscription ID: " + subscriptionId, e);
        }
    }

    @Override
    public List<Bill> findByStatus(String billStatus) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM bills WHERE bill_status = ? " +
                "ORDER BY billing_month";

        List<Bill> bills = new ArrayList<>();

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, billStatus);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    bills.add(mapRowToBill(resultSet));
                }
            }
            return bills;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find bills with status: " + billStatus, e);
        }
    }

    @Override
    public void save(Bill bill) {
        // bill_id is AUTO_INCREMENT and created_at/updated_at rely on their database defaults.
        String sql = "INSERT INTO bills (bill_number, subscription_id, billing_month, plan_rental, " +
                "usage_charges, tax_amount, discount, total_amount, due_date, bill_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, bill.getBillNumber());
            statement.setLong(2, bill.getSubscriptionId());
            statement.setDate(3, Date.valueOf(bill.getBillingMonth()));
            statement.setBigDecimal(4, bill.getPlanRental());
            statement.setBigDecimal(5, bill.getUsageCharges());
            statement.setBigDecimal(6, bill.getTaxAmount());
            statement.setBigDecimal(7, bill.getDiscount());
            statement.setBigDecimal(8, bill.getTotalAmount());
            statement.setDate(9, Date.valueOf(bill.getDueDate()));
            statement.setString(10, bill.getBillStatus());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    bill.setBillId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save bill with bill number: " + bill.getBillNumber(), e);
        }
    }

    @Override
    public void update(Bill bill) {
        // bill_id is the WHERE key and is never updated. created_at is never touched.
        String sql = "UPDATE bills SET bill_number = ?, subscription_id = ?, billing_month = ?, " +
                "plan_rental = ?, usage_charges = ?, tax_amount = ?, discount = ?, total_amount = ?, " +
                "due_date = ?, bill_status = ?, updated_at = CURRENT_TIMESTAMP WHERE bill_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, bill.getBillNumber());
            statement.setLong(2, bill.getSubscriptionId());
            statement.setDate(3, Date.valueOf(bill.getBillingMonth()));
            statement.setBigDecimal(4, bill.getPlanRental());
            statement.setBigDecimal(5, bill.getUsageCharges());
            statement.setBigDecimal(6, bill.getTaxAmount());
            statement.setBigDecimal(7, bill.getDiscount());
            statement.setBigDecimal(8, bill.getTotalAmount());
            statement.setDate(9, Date.valueOf(bill.getDueDate()));
            statement.setString(10, bill.getBillStatus());
            statement.setLong(11, bill.getBillId());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update bill with ID: " + bill.getBillId(), e);
        }
    }

    private Bill mapRowToBill(ResultSet resultSet) throws SQLException {
        Bill bill = new Bill();
        bill.setBillId(resultSet.getLong("bill_id"));
        bill.setBillNumber(resultSet.getString("bill_number"));
        bill.setSubscriptionId(resultSet.getLong("subscription_id"));
        bill.setBillingMonth(resultSet.getDate("billing_month").toLocalDate());
        bill.setPlanRental(resultSet.getBigDecimal("plan_rental"));
        bill.setUsageCharges(resultSet.getBigDecimal("usage_charges"));
        bill.setTaxAmount(resultSet.getBigDecimal("tax_amount"));
        bill.setDiscount(resultSet.getBigDecimal("discount"));
        bill.setTotalAmount(resultSet.getBigDecimal("total_amount"));
        bill.setDueDate(resultSet.getDate("due_date").toLocalDate());
        bill.setBillStatus(resultSet.getString("bill_status"));
        bill.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());
        bill.setUpdatedAt(resultSet.getTimestamp("updated_at").toLocalDateTime());
        return bill;
    }
}
