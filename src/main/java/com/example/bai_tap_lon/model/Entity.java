package com.example.bai_tap_lon.model;

public abstract class Entity {
    private final int id;

    protected Entity(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
