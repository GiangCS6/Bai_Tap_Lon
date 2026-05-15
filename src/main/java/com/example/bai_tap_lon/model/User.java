package com.example.bai_tap_lon.model;


public abstract class User extends Entity {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String email;

    public User(String username, String password, String email) {
        super(); // Gọi constructor của Entity để lấy ID
        this.username = username;
        this.password = password;
        this.email = email;
    }

    // Getters và Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // Phương thức trừu tượng thể hiện đa hình (Polymorphism)
    // Mỗi loại user sẽ in ra thông tin theo cách khác nhau
    public abstract void displayRoleInfo();

public class User extends Entity {
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
