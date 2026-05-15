package com.example.bai_tap_lon.model;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin(int id, String username, String password, String fullName) {
        super(id, username, password, fullName, UserRole.ADMIN);
    }

    public Admin(int id, String username, String password, String fullName, boolean locked) {
        super(id, username, password, fullName, UserRole.ADMIN, locked);
    }
}
