package com.example.demo.architecture.adapter.out.kafka;

import java.time.Instant;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.example.demo.architecture.domain.CouponEventPort;

@Component
public class KafkaCouponEventAdapter implements CouponEventPort {

    public static final String TOPIC_NAME = "coupon-issue-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaCouponEventAdapter(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishCouponIssued(Long couponId, Long userId, Instant issuedAt) {
        String key = String.valueOf(couponId);
        String payload = couponId + ":" + userId + ":" + issuedAt.toEpochMilli();
        kafkaTemplate.send(TOPIC_NAME, key, payload);
    }
}

