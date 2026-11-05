package com.example.demo.architecture.adapter.out.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponMongoRepository extends MongoRepository<CouponDocument, Long> {
}

