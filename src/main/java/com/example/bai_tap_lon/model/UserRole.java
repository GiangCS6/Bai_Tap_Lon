package com.example.bai_tap_lon.model;

public enum UserRole {
    BIDDER("Bidder"),
    SELLER("Seller"),
    ADMIN("Admin");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
