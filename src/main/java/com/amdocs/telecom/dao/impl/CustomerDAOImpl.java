package com.amdocs.telecom.dao.impl;

import com.amdocs.telecom.dao.CustomerDAO;
import com.amdocs.telecom.model.Customer;
import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JDBC implementation of CustomerDAO.
 * Only columns that have a matching field in Customer.java are selected/written;
 * columns such as failed_login_attempts, locked_until, created_at and updated_at
 * are not part of Customer.java and are therefore left untouched here.
 */
public class CustomerDAOImpl implements CustomerDAO {

    private static final String SELECT_COLUMNS =
            "customer_id, customer_number, first_name, last_name, date_of_birth, email, " +
            "mobile_number, address, city, country, username, password_hash, registration_date, account_status";

    @Override
    public Customer findById(Long customerId) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM customers WHERE customer_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToCustomer(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find customer by ID: " + customerId, e);
        }
    }

    @Override
    public Customer findByUsername(String username) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM customers WHERE username = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, username);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToCustomer(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find customer by username: " + username, e);
        }
    }

    @Override
    public Customer findByEmail(String email) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM customers WHERE email = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapRowToCustomer(resultSet);
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find customer by email: " + email, e);
        }
    }

    @Override
    public boolean existsByMobileNumber(String mobileNumber) {
        // "SELECT 1" avoids pulling back a whole row just to check existence.
        String sql = "SELECT 1 FROM customers WHERE mobile_number = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, mobileNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check existence of mobile number: " + mobileNumber, e);
        }
    }

    @Override
    public void save(Customer customer) {
        // customer_id is AUTO_INCREMENT and registration_date defaults to CURRENT_TIMESTAMP,
        // so neither is included in the INSERT.
        String sql = "INSERT INTO customers (customer_number, first_name, last_name, date_of_birth, email, " +
                "mobile_number, address, city, country, username, password_hash, account_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, customer.getCustomerNumber());
            statement.setString(2, customer.getFirstName());
            statement.setString(3, customer.getLastName());
            statement.setDate(4, java.sql.Date.valueOf(customer.getDateOfBirth()));
            statement.setString(5, customer.getEmail());
            statement.setString(6, customer.getMobileNumber());
            statement.setString(7, customer.getAddress());
            statement.setString(8, customer.getCity());
            statement.setString(9, customer.getCountry());
            statement.setString(10, customer.getUsername());
            statement.setString(11, customer.getPasswordHash());
            statement.setString(12, customer.getAccountStatus());

            statement.executeUpdate();

            // Read back the auto-generated customer_id and set it on the passed-in object.
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    customer.setCustomerId(generatedKeys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save customer with username: " + customer.getUsername(), e);
        }
    }

    @Override
    public void update(Customer customer) {
        // customer_id is the WHERE key and is never updated.
        // password_hash is intentionally excluded: the general update operation must not
        // touch the stored password hash (that belongs to a dedicated password-change flow).
        String sql = "UPDATE customers SET customer_number = ?, first_name = ?, last_name = ?, " +
                "date_of_birth = ?, email = ?, mobile_number = ?, address = ?, city = ?, country = ?, " +
                "username = ?, account_status = ?, updated_at = CURRENT_TIMESTAMP WHERE customer_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, customer.getCustomerNumber());
            statement.setString(2, customer.getFirstName());
            statement.setString(3, customer.getLastName());
            statement.setDate(4, java.sql.Date.valueOf(customer.getDateOfBirth()));
            statement.setString(5, customer.getEmail());
            statement.setString(6, customer.getMobileNumber());
            statement.setString(7, customer.getAddress());
            statement.setString(8, customer.getCity());
            statement.setString(9, customer.getCountry());
            statement.setString(10, customer.getUsername());
            statement.setString(11, customer.getAccountStatus());
            statement.setLong(12, customer.getCustomerId());

            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update customer with ID: " + customer.getCustomerId(), e);
        }
    }

    @Override
    public void deleteById(Long customerId) {
        String sql = "DELETE FROM customers WHERE customer_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(1, customerId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete customer with ID: " + customerId, e);
        }
    }

    private Customer mapRowToCustomer(ResultSet resultSet) throws SQLException {
        Customer customer = new Customer();
        customer.setCustomerId(resultSet.getLong("customer_id"));
        customer.setCustomerNumber(resultSet.getString("customer_number"));
        customer.setFirstName(resultSet.getString("first_name"));
        customer.setLastName(resultSet.getString("last_name"));
        customer.setDateOfBirth(resultSet.getDate("date_of_birth").toLocalDate());
        customer.setEmail(resultSet.getString("email"));
        customer.setMobileNumber(resultSet.getString("mobile_number"));
        customer.setAddress(resultSet.getString("address"));
        customer.setCity(resultSet.getString("city"));
        customer.setCountry(resultSet.getString("country"));
        customer.setUsername(resultSet.getString("username"));
        customer.setPasswordHash(resultSet.getString("password_hash"));
        customer.setRegistrationDate(resultSet.getTimestamp("registration_date").toLocalDateTime());
        customer.setAccountStatus(resultSet.getString("account_status"));
        return customer;
    }
}
