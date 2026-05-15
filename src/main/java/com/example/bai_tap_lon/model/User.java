package com.example.bai_tap_lon.model;

public class User extends Entity {
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String password;
    private final String fullName;
    private final UserRole role;
    private boolean locked;

    public User(int id, String username, String password, String fullName, UserRole role) {
        this(id, username, password, fullName, role, false);
    }

    public User(int id, String username, String password, String fullName, UserRole role, boolean locked) {
        super(id);
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.locked = locked;
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

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @Override
    public String toString() {
        return fullName + " (" + role.getDisplayName() + ")";
    }
}
