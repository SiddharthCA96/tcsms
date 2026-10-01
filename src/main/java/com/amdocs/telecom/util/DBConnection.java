
package com.amdocs.telecom.util;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utility class that creates JDBC connections to the MySQL database.
 *
 * Connection details are read from these environment variables:
 *   DB_URL      e.g. jdbc:mysql://localhost:3306/telecom_management
 *   DB_USERNAME e.g. root
 *   DB_PASSWORD your MySQL password
 */
public class DBConnection {

    private static final String DB_URL = System.getenv("DB_URL");
    private static final String DB_USERNAME = System.getenv("DB_USERNAME");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    // Private constructor: this class only has static methods, so it should not be instantiated.
    private DBConnection() {
    }

    /**
     * Opens and returns a new connection to the database.
     * The caller is responsible for closing it (preferably with try-with-resources).
     *
     * @return a new java.sql.Connection
     * @throws SQLException if the environment variables are missing or the connection fails
     */
    public static Connection getConnection() throws SQLException {
        if (isBlank(DB_URL)) {
            throw new SQLException("Environment variable DB_URL is not set.");
        }
        if (isBlank(DB_USERNAME)) {
            throw new SQLException("Environment variable DB_USERNAME is not set.");
        }
        // An empty password is allowed (some local MySQL setups use one), but it must be defined.
        if (DB_PASSWORD == null) {
            throw new SQLException("Environment variable DB_PASSWORD is not set.");
        }

        try {
            return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
        } catch (SQLException e) {
            throw new SQLException("Failed to connect to the database at " + DB_URL
                    + ". Check that MySQL is running and that the URL, username and password are correct. Cause: "
                    + e.getMessage(), e.getSQLState(), e.getErrorCode(), e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
