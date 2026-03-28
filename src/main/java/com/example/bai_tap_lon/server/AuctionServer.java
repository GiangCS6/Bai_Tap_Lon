package com.example.bai_tap_lon.server;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionServer {

    private static final int PORT = 8080;
    private static final String DB_URL = "jdbc:sqlite:auction.db";

    // Dữ liệu trong RAM
    private static final Map<String, String> users = new ConcurrentHashMap<>();
    private static final Map<String, AuctionItem> auctionItems = new ConcurrentHashMap<>();
    private static final Map<String, PrintWriter> onlineClients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("🚀 Auction Server khởi động tại port " + PORT);
        initDatabase();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ Server đang lắng nghe kết nối từ client...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("📡 Client mới kết nối từ: " + clientSocket.getInetAddress());
                new Thread(new AuctionClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("❌ Lỗi khởi tạo Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ====================== DATABASE ======================
    private static void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("✅ SQLite JDBC Driver đã load thành công.");

            try (Connection conn = DriverManager.getConnection(DB_URL);
                 Statement stmt = conn.createStatement()) {

                createTables(stmt);
                insertSampleData(stmt);
                loadDataToMemory(conn);

                System.out.println("✅ Database SQLite sẵn sàng! File: auction.db");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("❌ KHÔNG TÌM THẤY SQLite JDBC Driver! Kiểm tra dependency và Reload Maven.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("❌ Lỗi kết nối Database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createTables(Statement stmt) throws SQLException {
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS users (
                username TEXT PRIMARY KEY,
                password TEXT NOT NULL
            )
        """);

        stmt.execute("""
            CREATE TABLE IF NOT EXISTS auction_items (
                name TEXT PRIMARY KEY,
                current_price REAL,
                highest_bidder TEXT
            )
        """);
    }

    private static void insertSampleData(Statement stmt) throws SQLException {
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
        if (rs.next() && rs.getInt(1) == 0) {
            stmt.execute("""
                INSERT INTO users (username, password) VALUES 
                ('Admin', 'Admin'), ('user1', '123456'), ('user2', '123456')
            """);
        }

        rs = stmt.executeQuery("SELECT COUNT(*) FROM auction_items");
        if (rs.next() && rs.getInt(1) == 0) {
            stmt.execute("""
                INSERT INTO auction_items (name, current_price, highest_bidder) VALUES 
                ('iPhone 16 Pro', 20000000, 'Chưa có'),
                ('Laptop Dell XPS 15', 35000000, 'Chưa có'),
                ('Tai nghe Sony WH-1000XM5', 8000000, 'Chưa có')
            """);
        }
    }

    private static void loadDataToMemory(Connection conn) throws SQLException {
        loadUsersFromDB(conn);
        loadItemsFromDB(conn);
    }

    private static void loadUsersFromDB(Connection conn) throws SQLException {
        users.clear();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT username, password FROM users")) {
            while (rs.next()) {
                users.put(rs.getString("username"), rs.getString("password"));
            }
        }
    }

    private static void loadItemsFromDB(Connection conn) throws SQLException {
        auctionItems.clear();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name, current_price, highest_bidder FROM auction_items")) {
            while (rs.next()) {
                String name = rs.getString("name");
                double price = rs.getDouble("current_price");
                String bidder = rs.getString("highest_bidder");

                AuctionItem item = new AuctionItem(name, price);
                item.highestBidder = (bidder != null && !bidder.isEmpty()) ? bidder : "Chưa có";
                auctionItems.put(name, item);
            }
        }
    }

    private static void updateItemInDB(String name, double price, String bidder) {
        String sql = "UPDATE auction_items SET current_price = ?, highest_bidder = ? WHERE name = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, price);
            pstmt.setString(2, bidder);
            pstmt.setString(3, name);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void broadcast(String message) {
        onlineClients.values().forEach(writer -> {
            try {
                writer.println(message);
            } catch (Exception ignored) {}
        });
    }

    // ====================== CLIENT HANDLER ======================
    private static class AuctionClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter writer;
        private BufferedReader reader;
        private String username;

        public AuctionClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);

                String line;
                while ((line = reader.readLine()) != null) {
                    handleCommand(line);
                }
            } catch (IOException e) {
                System.out.println("Client ngắt kết nối: " + username);
            } finally {
                disconnect();   // ← Đã sửa lỗi ở đây
            }
        }

        private void handleCommand(String command) {
            String[] parts = command.split(":", 3);

            switch (parts[0].toUpperCase()) {
                case "LOGIN"  -> handleLogin(parts);
                case "BID"    -> handleBid(parts);
                case "LIST"   -> handleListItems();
                case "LOGOUT" -> disconnect();
                default       -> writer.println("ERROR:Lệnh không hợp lệ");
            }
        }

        private void handleLogin(String[] parts) {
            if (parts.length < 3) {
                writer.println("LOGIN_FAILED:Thiếu thông tin");
                return;
            }
            String user = parts[1];
            String pass = parts[2];

            if (users.containsKey(user) && users.get(user).equals(pass)) {
                this.username = user;
                onlineClients.put(user, writer);
                writer.println("LOGIN_SUCCESS:" + user);
                System.out.println("✅ " + user + " đã đăng nhập");
                broadcast("SYSTEM:" + user + " đã tham gia đấu giá!");
            } else {
                writer.println("LOGIN_FAILED:Sai tài khoản hoặc mật khẩu");
            }
        }

        private void handleBid(String[] parts) {
            if (username == null) {
                writer.println("ERROR:Bạn chưa đăng nhập");
                return;
            }
            if (parts.length < 3) {
                writer.println("BID_FAILED:Thiếu thông tin");
                return;
            }

            String itemName = parts[1];
            double bidAmount;
            try {
                bidAmount = Double.parseDouble(parts[2]);
            } catch (NumberFormatException e) {
                writer.println("BID_FAILED:Số tiền không hợp lệ");
                return;
            }

            AuctionItem item = auctionItems.get(itemName);
            if (item == null) {
                writer.println("BID_FAILED:Món đấu giá không tồn tại");
                return;
            }

            if (bidAmount <= item.currentPrice) {
                writer.println("BID_FAILED:Giá phải cao hơn giá hiện tại (" + item.currentPrice + ")");
                return;
            }

            // Cập nhật
            item.currentPrice = bidAmount;
            item.highestBidder = username;
            updateItemInDB(itemName, bidAmount, username);

            writer.println("BID_SUCCESS:" + itemName + ":" + bidAmount);
            broadcast("NEW_BID:" + itemName + ":" + bidAmount + ":" + username);
            System.out.println(username + " đấu giá " + bidAmount + " cho " + itemName);
        }

        private void handleListItems() {
            StringBuilder sb = new StringBuilder("ITEM_LIST:");
            for (Map.Entry<String, AuctionItem> entry : auctionItems.entrySet()) {
                AuctionItem item = entry.getValue();
                sb.append(entry.getKey())
                        .append("|")
                        .append(item.currentPrice)
                        .append("|")
                        .append(item.highestBidder)
                        .append(";");
            }
            writer.println(sb);
        }

        /** Phương thức đóng kết nối - ĐÃ ĐƯỢC KHAI BÁO ĐẦY ĐỦ */
        private void disconnect() {
            if (username != null) {
                onlineClients.remove(username);
                broadcast("SYSTEM:" + username + " đã rời khỏi đấu giá.");
                System.out.println("🔴 " + username + " đã ngắt kết nối.");
            }
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ignored) {}
        }
    }

    // ====================== MÔN ĐẤU GIÁ ======================
    private static class AuctionItem {
        String name;
        double currentPrice;
        String highestBidder = "Chưa có";

        public AuctionItem(String name, double startPrice) {
            this.name = name;
            this.currentPrice = startPrice;
        }
    }
}