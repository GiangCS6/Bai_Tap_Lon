package bai_tap_lon.common.model.user;

import bai_tap_lon.common.model.entity.Entity;

import java.time.LocalDateTime;

public abstract class User extends Entity {

    private String username;
    private String password;
    private String email;
    private boolean isActive;

    public User(String username, String password, String email) {
        super();
        this.username = username;
        this.password = password;
        this.email = email;
        this.isActive = true;
    }

    public User(String username) {
        this.username=username;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
    public void setPassword(String password) { this.password = password; }
    public abstract void printInfo();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public abstract String getRole();
}