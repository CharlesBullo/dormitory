package com.example.dormitory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    // =====================================================
    // DATABASE CONNECTION SETTINGS
    // =====================================================

    // For XAMPP (default configuration)
    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DATABASE_NAME = "dormitory_db";
    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE_NAME;

    // Database credentials
    private static final String USERNAME = "root";  // Default XAMPP username
    private static final String PASSWORD = "";      // Default XAMPP password (empty)

    // Additional connection properties
    private static final String SSL_DISABLED = "?useSSL=false";
    private static final String TIMEZONE = "&serverTimezone=UTC";
    private static final String ALLOW_PUBLIC_KEY_RETRIEVAL = "&allowPublicKeyRetrieval=true";
    private static final String CHARACTER_ENCODING = "&characterEncoding=utf8";

    // Full URL with additional properties (uncomment if needed)
    // private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE_NAME +
    //                                   SSL_DISABLED + TIMEZONE + ALLOW_PUBLIC_KEY_RETRIEVAL + CHARACTER_ENCODING;

    static {
        try {
            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("MySQL JDBC Driver loaded successfully!");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found!");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get database connection
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        try {
            Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Database connected successfully!");
            return connection;
        } catch (SQLException e) {
            System.err.println("Database connection failed!");
            System.err.println("URL: " + URL);
            System.err.println("Username: " + USERNAME);
            System.err.println("Error Code: " + e.getErrorCode());
            System.err.println("Error Message: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Test database connection
     * @return true if connection successful, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            boolean isValid = conn.isValid(5); // Timeout of 5 seconds
            if (isValid) {
                System.out.println("✓ Database connection test: SUCCESS");
                System.out.println("  Database: " + DATABASE_NAME);
                System.out.println("  Host: " + HOST + ":" + PORT);
                System.out.println("  User: " + USERNAME);
            }
            return isValid;
        } catch (SQLException e) {
            System.err.println("✗ Database connection test: FAILED");
            System.err.println("  Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Close database connection
     * @param conn Connection to close
     */
    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    /**
     * Get database URL (for debugging)
     * @return database URL
     */
    public static String getDatabaseURL() {
        return URL;
    }

    /**
     * Get database name
     * @return database name
     */
    public static String getDatabaseName() {
        return DATABASE_NAME;
    }
}