package com.example.demo.architecture.domain;

import java.time.Instant;

public class Coupon {

    private final Long id;
    private final String title;
    private final Long totalQuantity;
    private final Long remainingQuantity;
    private final Instant createdAt;

    public Coupon(Long id, String title, Long totalQuantity, Long remainingQuantity, Instant createdAt) {
        this.id = id;
        this.title = title;
        this.totalQuantity = totalQuantity;
        this.remainingQuantity = remainingQuantity;
        this.createdAt = createdAt;
    }

    public Coupon withRemainingQuantity(Long updatedRemainingQuantity) {
        return new Coupon(this.id, this.title, this.totalQuantity, updatedRemainingQuantity, this.createdAt);
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Long getTotalQuantity() {
        return totalQuantity;
    }

    public Long getRemainingQuantity() {
        return remainingQuantity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

