package bai_tap_lon.common.model.entity;
import bai_tap_lon.common.network.TimeUtil;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class Entity {

    protected String id;
    protected LocalDateTime createdAt;

    public Entity() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = TimeUtil.now();
    }

    public String getId() { return this.id; }
    public LocalDateTime getCreatedAt() { return this.createdAt; }
    public void setId(String id){this.id=id;}
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public abstract void printInfo();

}