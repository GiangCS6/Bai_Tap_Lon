package com.example.bai_tap_lon.model;

import java.time.LocalDateTime;

public class ProductReview {
    private final User reviewer;
    private final int rating;
    private final String comment;
    private final LocalDateTime createdAt;

    public ProductReview(User reviewer, int rating, String comment, LocalDateTime createdAt) {
        this.reviewer = reviewer;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public User getReviewer() {
        return reviewer;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
