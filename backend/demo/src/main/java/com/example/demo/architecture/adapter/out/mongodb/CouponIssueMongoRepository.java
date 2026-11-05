package com.example.demo.architecture.adapter.out.mongodb;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponIssueMongoRepository extends MongoRepository<CouponIssueDocument, String> {

    boolean existsByCouponIdAndUserId(Long couponId, Long userId);

    List<CouponIssueDocument> findByCouponIdOrderByIssuedAtDesc(Long couponId);
}

