package com.example.bai_tap_lon.model;

import java.io.Serializable;

public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;

    protected Entity(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
