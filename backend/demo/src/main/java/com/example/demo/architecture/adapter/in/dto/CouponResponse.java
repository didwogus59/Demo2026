package com.example.demo.architecture.adapter.in.dto;

import com.example.demo.architecture.domain.Coupon;

public record CouponResponse(
        Long id,
        String title,
        Long totalQuantity,
        Long remainingQuantity,
        String createdAt
) {
    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getTitle(),
                coupon.getTotalQuantity(),
                coupon.getRemainingQuantity(),
                coupon.getCreatedAt() != null ? coupon.getCreatedAt().toString() : null
        );
    }
}

