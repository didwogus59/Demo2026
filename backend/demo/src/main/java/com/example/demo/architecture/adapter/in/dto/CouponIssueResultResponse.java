package com.example.demo.architecture.adapter.in.dto;

import com.example.demo.architecture.domain.CouponIssueResult;

public record CouponIssueResultResponse(
        Long couponId,
        Long userId,
        String status,
        Long remainingQuantity,
        String message
) {
    public static CouponIssueResultResponse from(CouponIssueResult result) {
        return new CouponIssueResultResponse(
                result.getCouponId(),
                result.getUserId(),
                result.getStatus().name(),
                result.getRemainingQuantity(),
                result.getMessage()
        );
    }
}

