package com.example.demo.architecture.application;

import java.time.Instant;
import java.util.List;

import com.example.demo.architecture.domain.Coupon;
import com.example.demo.architecture.domain.CouponIssue;
import com.example.demo.architecture.domain.CouponIssueResult;

public interface CouponUseCase {

    Coupon createCoupon(Long couponId, String title, Long totalQuantity);

    CouponIssueResult requestIssue(Long couponId, Long userId);

    void confirmIssueFromEvent(Long couponId, Long userId, Instant issuedAt);

    List<Coupon> getAllCoupons();

    List<CouponIssue> getIssuedHistory(Long couponId);
}

