package com.example.demo.architecture.domain;

import java.util.Optional;

public interface CouponStockPort {

    void initializeStock(Long couponId, Long totalQuantity);

    CouponIssueResult tryIssueAtomic(Long couponId, Long userId);

    void rollbackIssue(Long couponId, Long userId);

    Optional<Long> getRemainingStock(Long couponId);
}

