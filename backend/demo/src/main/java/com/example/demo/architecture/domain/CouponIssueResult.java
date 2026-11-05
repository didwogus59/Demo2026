package com.example.demo.architecture.domain;

public class CouponIssueResult {

    private final Long couponId;
    private final Long userId;
    private final CouponIssueStatus status;
    private final Long remainingQuantity;
    private final String message;

    public CouponIssueResult(
            Long couponId,
            Long userId,
            CouponIssueStatus status,
            Long remainingQuantity,
            String message
    ) {
        this.couponId = couponId;
        this.userId = userId;
        this.status = status;
        this.remainingQuantity = remainingQuantity;
        this.message = message;
    }

    public Long getCouponId() {
        return couponId;
    }

    public Long getUserId() {
        return userId;
    }

    public CouponIssueStatus getStatus() {
        return status;
    }

    public Long getRemainingQuantity() {
        return remainingQuantity;
    }

    public String getMessage() {
        return message;
    }
}

