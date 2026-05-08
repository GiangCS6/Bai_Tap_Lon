package com.example.bai_tap_lon.model;

public class Bidder extends User {
    public Bidder(int id, String username, String password, String fullName) {
        super(id, username, password, fullName, UserRole.BIDDER);
    }
}
