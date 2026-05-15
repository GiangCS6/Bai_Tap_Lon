package com.example.bai_tap_lon.model;

public class Vehicle extends Item{
    private static final long serialVersionUID = 1L;
    private int productionYear;

    public Vehicle(String name, String description, double startingPrice, String sellerId, int productionYear) {
        super(name, description, startingPrice, ItemCategory.VEHICLE, sellerId);
        this.productionYear = productionYear;
    }

    @Override
    public void displayItemDetails() {
        System.out.println("[Xe cộ] " + getName() + " | Năm SX: " + productionYear + " | Giá hiện tại: " + getCurrentHighestBid());
    }
}
