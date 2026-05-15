package com.example.bai_tap_lon.model;

public class Electronics extends Item {
    private static final long serialVersionUID = 1L;
    private int warrantyMonths;

    public Electronics(String name, String description, double startingPrice, String sellerId, int warrantyMonths) {
        super(name, description, startingPrice, ItemCategory.ELECTRONICS, sellerId);
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public void displayItemDetails() {
        System.out.println("[Điện tử] " + getName() + " | Bảo hành: " + warrantyMonths + " tháng | Giá hiện tại: " + getCurrentHighestBid());
    }
}
