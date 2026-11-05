package com.example.demo.architecture.adapter.in;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.demo.architecture.adapter.out.kafka.KafkaCouponEventAdapter;
import com.example.demo.architecture.application.CouponUseCase;

@Component
public class CouponKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(CouponKafkaConsumer.class);

    private final CouponUseCase couponUseCase;

    public CouponKafkaConsumer(CouponUseCase couponUseCase) {
        this.couponUseCase = couponUseCase;
    }

    @KafkaListener(
            topics = KafkaCouponEventAdapter.TOPIC_NAME,
            groupId = "${spring.kafka.consumer.group-id:coupon-issue-group}"
    )
    public void consumeCouponIssuedEvent(String message) {
        try {
            String[] parts = message.split(":");
            if (parts.length < 3) {
                log.warn("Invalid coupon issue event message format: {}", message);
                return;
            }
            Long couponId = Long.parseLong(parts[0]);
            Long userId = Long.parseLong(parts[1]);
            Instant issuedAt = Instant.ofEpochMilli(Long.parseLong(parts[2]));

            couponUseCase.confirmIssueFromEvent(couponId, userId, issuedAt);
            log.info("Persisted coupon issue to MongoDB: couponId={}, userId={}", couponId, userId);
        } catch (Exception ex) {
            log.error("Failed to process coupon issue event: {}", message, ex);
            throw ex;
        }
    }
}
