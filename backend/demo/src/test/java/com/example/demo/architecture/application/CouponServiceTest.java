package com.example.demo.architecture.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.demo.architecture.domain.Coupon;
import com.example.demo.architecture.domain.CouponEventPort;
import com.example.demo.architecture.domain.CouponIssue;
import com.example.demo.architecture.domain.CouponIssueResult;
import com.example.demo.architecture.domain.CouponIssueStatus;
import com.example.demo.architecture.domain.CouponRepository;
import com.example.demo.architecture.domain.CouponStockPort;

class CouponServiceTest {

    private FakeCouponRepository couponRepository;
    private FakeCouponStockPort couponStockPort;
    private FakeCouponEventPort couponEventPort;
    private CouponService couponService;

    @BeforeEach
    void setUp() {
        couponRepository = new FakeCouponRepository();
        couponStockPort = new FakeCouponStockPort();
        couponEventPort = new FakeCouponEventPort();
        couponService = new CouponService(couponRepository, couponStockPort, couponEventPort);
    }

    @Test
    void shouldIssueCouponWithinLimitAndBlockDuplicatesAndSoldOut() {
        couponService.createCoupon(100L, "오픈 기념 선착순 쿠폰", 2L);

        // 1번째 유저 발급 성공 (잔여 1)
        CouponIssueResult res1 = couponService.requestIssue(100L, 1L);
        assertEquals(CouponIssueStatus.SUCCESS, res1.getStatus());
        assertEquals(1L, res1.getRemainingQuantity());

        // 1번째 유저 중복 발급 시도 -> DUPLICATED 차단 (잔여 1 유지)
        CouponIssueResult dupRes = couponService.requestIssue(100L, 1L);
        assertEquals(CouponIssueStatus.DUPLICATED, dupRes.getStatus());
        assertEquals(1L, dupRes.getRemainingQuantity());

        // 2번째 유저 발급 성공 (잔여 0)
        CouponIssueResult res2 = couponService.requestIssue(100L, 2L);
        assertEquals(CouponIssueStatus.SUCCESS, res2.getStatus());
        assertEquals(0L, res2.getRemainingQuantity());

        // 3번째 유저 발급 시도 -> SOLD_OUT 차단
        CouponIssueResult soldOutRes = couponService.requestIssue(100L, 3L);
        assertEquals(CouponIssueStatus.SOLD_OUT, soldOutRes.getStatus());
        assertEquals(0L, soldOutRes.getRemainingQuantity());

        // Kafka 이벤트는 성공한 2건만 발행됨
        assertEquals(2, couponEventPort.publishedEvents.size());
    }

    @Test
    void shouldRollbackRedisStockWhenKafkaPublishFails() {
        couponService.createCoupon(200L, "장애 복구 테스트 쿠폰", 1L);
        couponEventPort.shouldFail = true;

        assertThrows(RuntimeException.class, () -> couponService.requestIssue(200L, 10L));

        // 롤백되어 잔여 수량이 다시 1이어야 하며, 동일 유저가 재시도 가능해야 함
        assertEquals(Optional.of(1L), couponStockPort.getRemainingStock(200L));

        couponEventPort.shouldFail = false;
        CouponIssueResult retryResult = couponService.requestIssue(200L, 10L);
        assertEquals(CouponIssueStatus.SUCCESS, retryResult.getStatus());
        assertEquals(0L, retryResult.getRemainingQuantity());
    }

    @Test
    void shouldPersistIdempotentlyWhenKafkaConsumerReceivesDuplicateMessage() {
        Instant now = Instant.now();
        couponService.confirmIssueFromEvent(100L, 1L, now);
        couponService.confirmIssueFromEvent(100L, 1L, now);

        List<CouponIssue> issues = couponService.getIssuedHistory(100L);
        assertEquals(1, issues.size());
        assertEquals(1L, issues.get(0).getUserId());
    }

    private static class FakeCouponRepository implements CouponRepository {
        private final Map<Long, Coupon> coupons = new HashMap<>();
        private final List<CouponIssue> issues = new ArrayList<>();

        @Override
        public Coupon saveCoupon(Coupon coupon) {
            coupons.put(coupon.getId(), coupon);
            return coupon;
        }

        @Override
        public Optional<Coupon> findCouponById(Long couponId) {
            return Optional.ofNullable(coupons.get(couponId));
        }

        @Override
        public List<Coupon> findAllCoupons() {
            return new ArrayList<>(coupons.values());
        }

        @Override
        public CouponIssue saveCouponIssue(CouponIssue couponIssue) {
            issues.add(couponIssue);
            return couponIssue;
        }

        @Override
        public boolean existsCouponIssue(Long couponId, Long userId) {
            return issues.stream()
                    .anyMatch(i -> i.getCouponId().equals(couponId) && i.getUserId().equals(userId));
        }

        @Override
        public List<CouponIssue> findIssuedByCouponId(Long couponId) {
            return issues.stream()
                    .filter(i -> i.getCouponId().equals(couponId))
                    .toList();
        }
    }

    private static class FakeCouponStockPort implements CouponStockPort {
        private final Map<Long, Long> stocks = new HashMap<>();
        private final Map<Long, Set<Long>> issuedUsers = new HashMap<>();

        @Override
        public synchronized void initializeStock(Long couponId, Long totalQuantity) {
            stocks.put(couponId, totalQuantity);
            issuedUsers.put(couponId, new HashSet<>());
        }

        @Override
        public synchronized CouponIssueResult tryIssueAtomic(Long couponId, Long userId) {
            if (!stocks.containsKey(couponId)) {
                return new CouponIssueResult(couponId, userId, CouponIssueStatus.COUPON_NOT_FOUND, 0L, "Not found");
            }
            Set<Long> users = issuedUsers.get(couponId);
            long currentStock = stocks.get(couponId);
            if (users.contains(userId)) {
                return new CouponIssueResult(couponId, userId, CouponIssueStatus.DUPLICATED, currentStock, "Duplicated");
            }
            if (currentStock <= 0) {
                return new CouponIssueResult(couponId, userId, CouponIssueStatus.SOLD_OUT, 0L, "Sold out");
            }
            long remaining = currentStock - 1;
            stocks.put(couponId, remaining);
            users.add(userId);
            return new CouponIssueResult(couponId, userId, CouponIssueStatus.SUCCESS, remaining, "Success");
        }

        @Override
        public synchronized void rollbackIssue(Long couponId, Long userId) {
            Set<Long> users = issuedUsers.get(couponId);
            if (users != null && users.remove(userId)) {
                stocks.computeIfPresent(couponId, (k, v) -> v + 1);
            }
        }

        @Override
        public synchronized Optional<Long> getRemainingStock(Long couponId) {
            return Optional.ofNullable(stocks.get(couponId));
        }
    }

    private static class FakeCouponEventPort implements CouponEventPort {
        private final List<String> publishedEvents = new ArrayList<>();
        private boolean shouldFail = false;

        @Override
        public void publishCouponIssued(Long couponId, Long userId, Instant issuedAt) {
            if (shouldFail) {
                throw new RuntimeException("Kafka broker unavailable");
            }
            publishedEvents.add(couponId + ":" + userId);
        }
    }
}

