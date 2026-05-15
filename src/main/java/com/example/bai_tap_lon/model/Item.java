package com.example.bai_tap_lon.model;

public class Item extends Entity {
    private static final long serialVersionUID = 1L;

    private String seller;
    private String description;
    private double startPrice;
    private String name;
    private double currentHighestBid;
    private ItemCategory category;
    private String sellerId;

    public Item() {
        super(0);
    }

    public Item(String name, String description, double startingPrice, ItemCategory category, String sellerId) {
        super(0);
        this.name = name;
        this.description = description;
        this.startPrice = startingPrice;
        this.currentHighestBid = startingPrice;
        this.category = category;
        this.sellerId = sellerId;
        this.seller = sellerId;
    }

    public String getSeller() {
        return seller;
    }

    public void setSeller(String seller) {
        this.seller = seller;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(double startPrice) {
        this.startPrice = startPrice;
    }

    public String getName() {
        return name;
    }

    public double getStartingPrice() {
        return startPrice;
    }

    public double getCurrentHighestBid() {
        return currentHighestBid;
    }

    public void setCurrentHighestBid(double currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void displayItemDetails() {
        System.out.println(name + " | Current bid: " + currentHighestBid);
    }
}
