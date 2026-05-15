package com.example.bai_tap_lon.model;


import java.io.Serializable;
import java.util.UUID;

public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    // Đóng gói dữ liệu (Encapsulation)
    private String id;

    public Entity() {
        // Tự động sinh ID duy nhất khi khởi tạo
        this.id = UUID.randomUUID().toString();
    }

    public Entity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

public abstract class Entity {
    private final int id;

    protected Entity(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

}
