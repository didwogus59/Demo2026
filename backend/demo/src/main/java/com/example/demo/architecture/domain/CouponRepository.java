package com.example.demo.architecture.domain;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {

    Coupon saveCoupon(Coupon coupon);

    Optional<Coupon> findCouponById(Long couponId);

    List<Coupon> findAllCoupons();

    CouponIssue saveCouponIssue(CouponIssue couponIssue);

    boolean existsCouponIssue(Long couponId, Long userId);

    List<CouponIssue> findIssuedByCouponId(Long couponId);
}

