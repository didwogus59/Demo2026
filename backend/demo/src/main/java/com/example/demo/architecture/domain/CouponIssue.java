package com.example.demo.architecture.domain;

import java.time.Instant;

public class CouponIssue {

    private final String id;
    private final Long couponId;
    private final Long userId;
    private final Instant issuedAt;

    public CouponIssue(String id, Long couponId, Long userId, Instant issuedAt) {
        this.id = id;
        this.couponId = couponId;
        this.userId = userId;
        this.issuedAt = issuedAt;
    }

    public static CouponIssue create(Long couponId, Long userId, Instant issuedAt) {
        return new CouponIssue(couponId + ":" + userId, couponId, userId, issuedAt);
    }

    public String getId() {
        return id;
    }

    public Long getCouponId() {
        return couponId;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}

