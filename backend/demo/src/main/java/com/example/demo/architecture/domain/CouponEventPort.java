package com.example.demo.architecture.domain;

import java.time.Instant;

public interface CouponEventPort {

    void publishCouponIssued(Long couponId, Long userId, Instant issuedAt);
}

