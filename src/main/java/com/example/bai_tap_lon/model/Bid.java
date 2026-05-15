package com.example.bai_tap_lon.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Bid {
    private final User bidder;
    private final BigDecimal amount;
    private final LocalDateTime createdAt;

    public Bid(User bidder, BigDecimal amount, LocalDateTime createdAt) {
        this.bidder = bidder;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public User getBidder() {
        return bidder;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
