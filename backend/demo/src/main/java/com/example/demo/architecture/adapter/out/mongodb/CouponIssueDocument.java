package com.example.demo.architecture.adapter.out.mongodb;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "coupon_issues")
@CompoundIndex(name = "idx_coupon_user_unique", def = "{'couponId': 1, 'userId': 1}", unique = true)
public class CouponIssueDocument {

    @Id
    private String id;

    private Long couponId;

    private Long userId;

    private Instant issuedAt;

    public CouponIssueDocument() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }
}
