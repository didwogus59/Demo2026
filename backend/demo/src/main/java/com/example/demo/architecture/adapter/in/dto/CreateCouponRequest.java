package com.example.demo.architecture.adapter.in.dto;

public record CreateCouponRequest(
        Long id,
        String title,
        Long totalQuantity
) {
}

