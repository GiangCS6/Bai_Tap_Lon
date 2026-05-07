package com.example.bai_tap_lon.model;

import java.time.LocalDateTime;

public abstract class Item extends Entity {
    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private double startingPrice;
    private double currentHighestBid;
    private ItemCategory category;
    private String sellerId; // Chứa ID của người bán (Seller)

    public Item(String name, String description, double startingPrice, ItemCategory category, String sellerId) {
        super(); // Khởi tạo ID từ Entity
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentHighestBid = startingPrice; // Lúc mới đăng, giá cao nhất bằng giá khởi điểm
        this.category = category;
        this.sellerId = sellerId;
    }

    // Getters and Setters (Đảm bảo tính Đóng gói)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getStartingPrice() { return startingPrice; }

    public double getCurrentHighestBid() { return currentHighestBid; }
    public void setCurrentHighestBid(double currentHighestBid) { this.currentHighestBid = currentHighestBid; }

    public ItemCategory getCategory() { return category; }
    public String getSellerId() { return sellerId; }

    // Phương thức trừu tượng (Tính Đa hình)
    public abstract void displayItemDetails();
}
