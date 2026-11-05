package com.example.demo.architecture.application;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.architecture.domain.Coupon;
import com.example.demo.architecture.domain.CouponEventPort;
import com.example.demo.architecture.domain.CouponIssue;
import com.example.demo.architecture.domain.CouponIssueResult;
import com.example.demo.architecture.domain.CouponIssueStatus;
import com.example.demo.architecture.domain.CouponRepository;
import com.example.demo.architecture.domain.CouponStockPort;

@Service
public class CouponService implements CouponUseCase {

    private final CouponRepository couponRepository;
    private final CouponStockPort couponStockPort;
    private final CouponEventPort couponEventPort;

    public CouponService(
            CouponRepository couponRepository,
            CouponStockPort couponStockPort,
            CouponEventPort couponEventPort
    ) {
        this.couponRepository = couponRepository;
        this.couponStockPort = couponStockPort;
        this.couponEventPort = couponEventPort;
    }

    @Override
    public Coupon createCoupon(Long couponId, String title, Long totalQuantity) {
        Coupon coupon = new Coupon(
                couponId,
                title,
                totalQuantity,
                totalQuantity,
                Instant.now()
        );
        Coupon saved = couponRepository.saveCoupon(coupon);
        couponStockPort.initializeStock(couponId, totalQuantity);
        return saved;
    }

    @Override
    public CouponIssueResult requestIssue(Long couponId, Long userId) {
        CouponIssueResult result = couponStockPort.tryIssueAtomic(couponId, userId);

        if (result.getStatus() == CouponIssueStatus.SUCCESS) {
            try {
                couponEventPort.publishCouponIssued(couponId, userId, Instant.now());
            } catch (RuntimeException ex) {
                couponStockPort.rollbackIssue(couponId, userId);
                throw ex;
            }
        }

        return result;
    }

    @Override
    public void confirmIssueFromEvent(Long couponId, Long userId, Instant issuedAt) {
        if (couponRepository.existsCouponIssue(couponId, userId)) {
            return;
        }
        CouponIssue issue = CouponIssue.create(couponId, userId, issuedAt);
        couponRepository.saveCouponIssue(issue);
    }

    @Override
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAllCoupons().stream()
                .map(coupon -> couponStockPort.getRemainingStock(coupon.getId())
                        .map(coupon::withRemainingQuantity)
                        .orElse(coupon))
                .toList();
    }

    @Override
    public List<CouponIssue> getIssuedHistory(Long couponId) {
        return couponRepository.findIssuedByCouponId(couponId);
    }
}

