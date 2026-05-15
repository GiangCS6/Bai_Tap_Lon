package com.example.bai_tap_lon.model;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;

    public Bidder(int id, String username, String password, String fullName) {
        super(id, username, password, fullName, UserRole.BIDDER);
    }

    public Bidder(int id, String username, String password, String fullName, boolean locked) {
        super(id, username, password, fullName, UserRole.BIDDER, locked);
    }
}
