/*package bai_tap_lon.server.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() throws SQLException {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) throw new RuntimeException("Can't read db.properties");
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Can't find db.properties", e);
        }

        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");

        if (url == null || user == null) throw new RuntimeException("Can't find db information");

        String password = props.getProperty("db.password");
        this.connection = DriverManager.getConnection(url, user, password);
    }

    public static synchronized DatabaseConnection getInstance() throws SQLException {
        if (instance == null || instance.connection.isClosed()) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}*/

/*
package bai_tap_lon.server.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() throws SQLException {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new RuntimeException("Không tìm thấy file db.properties");
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi đọc db.properties", e);
        }

        String url = props.getProperty("db.url");
        if (url == null || url.trim().isEmpty()) {
            throw new RuntimeException("db.url không được để trống trong db.properties");
        }

        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");

        // Logic đặc biệt cho SQLite
        if (url.startsWith("jdbc:sqlite:")) {
            // SQLite không cần user/password
            this.connection = DriverManager.getConnection(url);
            System.out.println("✅ Kết nối SQLite thành công: " + url);
        } else {
            // Hỗ trợ fallback cho MySQL / các database khác
            if (user == null || user.trim().isEmpty()) {
                throw new RuntimeException("db.user không được để trống với database không phải SQLite");
            }
            this.connection = DriverManager.getConnection(url, user, password);
            System.out.println("✅ Kết nối " + url + " thành công với user: " + user);
        }
    }

    public static synchronized DatabaseConnection getInstance() throws SQLException {
        if (instance == null || instance.connection == null || instance.connection.isClosed()) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    */
/**
     * Đóng kết nối khi shutdown (tùy chọn)
     *//*

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }
}*/
package bai_tap_lon.server.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;

    private DatabaseConnection() throws SQLException {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new RuntimeException("Không tìm thấy file db.properties");
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi đọc db.properties", e);
        }

        String url = props.getProperty("db.url");
        if (url == null || url.trim().isEmpty()) {
            throw new RuntimeException("db.url không được để trống trong db.properties");
        }

        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password");

        // Logic đặc biệt cho SQLite
        if (url.startsWith("jdbc:sqlite:")) {
            this.connection = DriverManager.getConnection(url);
            System.out.println("Kết nối SQLite thành công: " + url);
        } else {
            this.connection = DriverManager.getConnection(url, user, password);
            System.out.println("Kết nối " + url + " thành công");
        }
    }

    public static synchronized DatabaseConnection getInstance() throws SQLException {
        if (instance == null || instance.connection == null || instance.connection.isClosed()) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }
}
