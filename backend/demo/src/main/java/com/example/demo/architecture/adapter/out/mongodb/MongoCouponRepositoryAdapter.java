package com.example.demo.architecture.adapter.out.mongodb;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.demo.architecture.domain.Coupon;
import com.example.demo.architecture.domain.CouponIssue;
import com.example.demo.architecture.domain.CouponRepository;

@Repository
public class MongoCouponRepositoryAdapter implements CouponRepository {

    private final CouponMongoRepository couponMongoRepository;
    private final CouponIssueMongoRepository couponIssueMongoRepository;

    public MongoCouponRepositoryAdapter(
            CouponMongoRepository couponMongoRepository,
            CouponIssueMongoRepository couponIssueMongoRepository
    ) {
        this.couponMongoRepository = couponMongoRepository;
        this.couponIssueMongoRepository = couponIssueMongoRepository;
    }

    @Override
    public Coupon saveCoupon(Coupon coupon) {
        CouponDocument saved = couponMongoRepository.save(toCouponDocument(coupon));
        return toCouponDomain(saved);
    }

    @Override
    public Optional<Coupon> findCouponById(Long couponId) {
        return couponMongoRepository.findById(couponId).map(this::toCouponDomain);
    }

    @Override
    public List<Coupon> findAllCoupons() {
        return couponMongoRepository.findAll().stream()
                .map(this::toCouponDomain)
                .toList();
    }

    @Override
    public CouponIssue saveCouponIssue(CouponIssue couponIssue) {
        CouponIssueDocument saved = couponIssueMongoRepository.save(toIssueDocument(couponIssue));
        return toIssueDomain(saved);
    }

    @Override
    public boolean existsCouponIssue(Long couponId, Long userId) {
        return couponIssueMongoRepository.existsByCouponIdAndUserId(couponId, userId);
    }

    @Override
    public List<CouponIssue> findIssuedByCouponId(Long couponId) {
        return couponIssueMongoRepository.findByCouponIdOrderByIssuedAtDesc(couponId).stream()
                .map(this::toIssueDomain)
                .toList();
    }

    private Coupon toCouponDomain(CouponDocument doc) {
        return new Coupon(
                doc.getId(),
                doc.getTitle(),
                doc.getTotalQuantity(),
                doc.getRemainingQuantity(),
                doc.getCreatedAt()
        );
    }

    private CouponDocument toCouponDocument(Coupon coupon) {
        CouponDocument doc = new CouponDocument();
        doc.setId(coupon.getId());
        doc.setTitle(coupon.getTitle());
        doc.setTotalQuantity(coupon.getTotalQuantity());
        doc.setRemainingQuantity(coupon.getRemainingQuantity());
        doc.setCreatedAt(coupon.getCreatedAt());
        return doc;
    }

    private CouponIssue toIssueDomain(CouponIssueDocument doc) {
        return new CouponIssue(
                doc.getId(),
                doc.getCouponId(),
                doc.getUserId(),
                doc.getIssuedAt()
        );
    }

    private CouponIssueDocument toIssueDocument(CouponIssue issue) {
        CouponIssueDocument doc = new CouponIssueDocument();
        doc.setId(issue.getId());
        doc.setCouponId(issue.getCouponId());
        doc.setUserId(issue.getUserId());
        doc.setIssuedAt(issue.getIssuedAt());
        return doc;
    }
}
