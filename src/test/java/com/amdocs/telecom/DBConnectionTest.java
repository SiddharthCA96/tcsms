package com.amdocs.telecom;

import com.amdocs.telecom.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;

public class DBConnectionTest {

    public static void main(String[] args) {
        try (Connection connection = DBConnection.getConnection()) {
            System.out.println("Database connection successful!");
        } catch (SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
        }
    }
}
