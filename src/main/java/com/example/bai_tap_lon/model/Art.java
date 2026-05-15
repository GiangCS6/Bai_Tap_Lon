package com.example.bai_tap_lon.model;

public class Art extends Item{
    private static final long serialVersionUID = 1L;
    private String artistName;

    public Art(String name, String description, double startingPrice, String sellerId, String artistName) {
        super(name, description, startingPrice, ItemCategory.ART, sellerId);
        this.artistName = artistName;
    }

    @Override
    public void displayItemDetails() {
        System.out.println("[Nghệ thuật] " + getName() + " | Tác giả: " + artistName + " | Giá hiện tại: " + getCurrentHighestBid());
    }
}
