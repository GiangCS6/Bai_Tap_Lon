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
}
