package com.example.bai_tap_lon.model;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    private final UserRole originalRole;

    public Admin(int id, String username, String password, String fullName) {
        this(id, username, password, fullName, false, UserRole.BIDDER);
    }

    public Admin(int id, String username, String password, String fullName, boolean locked) {
        this(id, username, password, fullName, locked, UserRole.BIDDER);
    }

    public Admin(int id, String username, String password, String fullName, boolean locked, UserRole originalRole) {
        super(id, username, password, fullName, UserRole.ADMIN, locked);
        this.originalRole = normalizeOriginalRole(originalRole);
    }

    public UserRole getOriginalRole() {
        return originalRole;
    }

    private UserRole normalizeOriginalRole(UserRole role) {
        if (role == null || role == UserRole.ADMIN) {
            return UserRole.BIDDER;
        }
        return role;
    }
}
