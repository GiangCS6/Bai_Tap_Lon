package com.example.bai_tap_lon.model;


import java.util.ArrayList;
import java.util.List;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;

    private double balance; // Số dư tài khoản để đặt giá
    private List<String> bidHistory; // Danh sách ID các phiên đã tham gia

    public Bidder(String username, String password, String email, double initialBalance) {
        super(username, password, email);
        this.balance = initialBalance;
        this.bidHistory = new ArrayList<>();
    }

    // Các Getter và Setter
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public List<String> getBidHistory() { return bidHistory; }

    @Override
    public void displayRoleInfo() {
        System.out.println("Vai trò: Người đấu giá | Tài khoản: " + getUsername() + " | Số dư: " + balance);
    }

    // --- LOGIC NGHIỆP VỤ CỦA BIDDER ---

    /**
     * Kiểm tra xem người dùng có đủ tiền để thực hiện bid không
     */
    public boolean canAfford(double amount) {
        return this.balance >= amount;
    }

    /**
     * Ghi nhận một lần tham gia đấu giá mới
     */
    public void addBidToHistory(String auctionId) {
        if (!bidHistory.contains(auctionId)) {
            bidHistory.add(auctionId);
        }

public class Bidder extends User {
    public Bidder(int id, String username, String password, String fullName) {
        super(id, username, password, fullName, UserRole.BIDDER);
    }

    public Bidder(int id, String username, String password, String fullName, boolean locked) {
        super(id, username, password, fullName, UserRole.BIDDER, locked);
    }
}
