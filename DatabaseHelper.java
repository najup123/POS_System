package com.pujanmapase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseHelper {
    // Database connection parameters
    private static final String URL = "jdbc:mysql://localhost:3306/mapasedb";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "password";

    public static void main(String[] args) {
        try {
            initializeDB();
            System.out.println("Database initialized successfully!");
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }

    public static void initializeDB() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create Products table
            String sql = "CREATE TABLE IF NOT EXISTS products (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "name VARCHAR(100) NOT NULL," +
                        "category VARCHAR(50) NOT NULL," +
                        "price DECIMAL(10,2) NOT NULL," +
                        "stock INT NOT NULL," +
                        "alcohol_content DECIMAL(5,2)," +
                        "volume_ml INT)";
            stmt.execute(sql);
            
            // Create Sales table
            sql = "CREATE TABLE IF NOT EXISTS sales (" +
                  "id INT AUTO_INCREMENT PRIMARY KEY," +
                  "transaction_date DATETIME NOT NULL," +
                  "total_amount DECIMAL(10,2) NOT NULL," +
                  "payment_method VARCHAR(50) NOT NULL)";
            stmt.execute(sql);
            
            // Create SaleItems table with foreign keys
            sql = "CREATE TABLE IF NOT EXISTS sale_items (" +
                  "id INT AUTO_INCREMENT PRIMARY KEY," +
                  "sale_id INT NOT NULL," +
                  "product_id INT NOT NULL," +
                  "quantity INT NOT NULL," +
                  "price DECIMAL(10,2) NOT NULL," +
                  "CONSTRAINT fk_sale FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE," +
                  "CONSTRAINT fk_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE)";
            stmt.execute(sql);
            
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}