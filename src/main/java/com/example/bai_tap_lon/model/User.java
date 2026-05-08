package com.example.bai_tap_lon.model;

public class User extends Entity {
    private final String username;
    private final String password;
    private final String fullName;
    private final UserRole role;

    public User(int id, String username, String password, String fullName, UserRole role) {
        super(id);
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public boolean hasPassword(String rawPassword) {
        return password.equals(rawPassword);
    }

    public String getPassword() {
        return password;
    }

    public String getFullName() {
        return fullName;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public String toString() {
        return fullName + " (" + role.getDisplayName() + ")";
    }
}
