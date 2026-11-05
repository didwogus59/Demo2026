package com.example.demo.architecture.adapter.in.dto;

import com.example.demo.architecture.domain.CouponIssue;

public record CouponIssueHistoryResponse(
        String id,
        Long couponId,
        Long userId,
        String issuedAt
) {
    public static CouponIssueHistoryResponse from(CouponIssue issue) {
        return new CouponIssueHistoryResponse(
                issue.getId(),
                issue.getCouponId(),
                issue.getUserId(),
                issue.getIssuedAt() != null ? issue.getIssuedAt().toString() : null
        );
    }
}

