package com.example.bai_tap_lon.model;

public class Seller extends User {
    private static final long serialVersionUID = 1L;

    public Seller(int id, String username, String password, String fullName) {
        super(id, username, password, fullName, UserRole.SELLER);
    }

    public Seller(int id, String username, String password, String fullName, boolean locked) {
        super(id, username, password, fullName, UserRole.SELLER, locked);
    }
}
