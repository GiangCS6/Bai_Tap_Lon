package com.example.bai_tap_lon.service;

import com.example.bai_tap_lon.controller.AdminController;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LoginService {

    private static final String DB_URL = "jdbc:sqlite:auction.db";

    public LoginService() {
        createUserTable();
        migrateRoleColumn();
        //insertSampleUsers();
    }

    private void createUserTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                username TEXT PRIMARY KEY,
                password TEXT NOT NULL,
                role TEXT
            )
        """;

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void migrateRoleColumn() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("PRAGMA table_info(users)");
            boolean hasRole = false;
            while (rs.next()) {
                if ("role".equalsIgnoreCase(rs.getString("name"))) {
                    hasRole = true;
                    break;
                }
            }

            if (!hasRole) {
                stmt.execute("ALTER TABLE users ADD COLUMN role TEXT DEFAULT 'Bidder'");
                System.out.println("✅ Đã thêm cột 'role' vào bảng users.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*public boolean register(String username, String password) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false; // username đã tồn tại
        }
    }*/
    public boolean register(String username, String password, String role) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role);

            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            return false;
        }
    }

    public boolean authenticate(String username, String password) {
        String sql = "SELECT password, role FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String dbPassword = rs.getString("password");
                String dbRole = rs.getString("role");
                return dbPassword.equals(password);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String authenticateAndGetRole(String username, String password) {
        String sql = "SELECT password, role FROM users WHERE username = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String dbPassword = rs.getString("password");
                String dbRole = rs.getString("role");

                if (dbPassword.equals(password)) {
                    return dbRole != null ? dbRole : "Bidder"; // fallback nếu role null
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // sai username hoặc password
    }
    // tài khoản mẫuseller
//    public void insertSampleUsers() {
//        register("admin", "admin123", "Admin");
//        register("seller1", "123456", "Seller");
//        register("bidder1", "123456", "Bidder");
//    }

    private boolean isTableEmpty() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) == 0;
        } catch (SQLException e) {
            return true;
        }
    }

    // Lấy tất cả user
    public List<AdminController.User> getAllUsers() {
        List<AdminController.User> list = new ArrayList<>();
        String sql = "SELECT username, password, role FROM users";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new AdminController.User(
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Xóa user
    public boolean deleteUser(String username) {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}