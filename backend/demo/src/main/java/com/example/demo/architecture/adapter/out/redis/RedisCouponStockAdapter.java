package com.example.demo.architecture.adapter.out.redis;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import com.example.demo.architecture.domain.CouponIssueResult;
import com.example.demo.architecture.domain.CouponIssueStatus;
import com.example.demo.architecture.domain.CouponStockPort;

@Repository
public class RedisCouponStockAdapter implements CouponStockPort {

    /**
     * 원자적(Atomic) 선착순 쿠폰 발급 Lua Script
     * KEYS[1] : 쿠폰 잔여 수량 키 (coupon:{couponId}:stock)
     * KEYS[2] : 발급 유저 Set 키 (coupon:{couponId}:users)
     * ARGV[1] : 요청 userId
     *
     * 반환값: { statusCode, remainingStock }
     *  1 : 발급 성공 (수량 차감 및 유저 Set 등록 완료)
     * -1 : 중복 발급 요청
     * -2 : 수량 소진 (Sold Out)
     * -3 : 쿠폰 재고 키 없음
     */
    private static final String ISSUE_LUA_SCRIPT = """
            if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
                local currentStock = tonumber(redis.call('GET', KEYS[1]) or '0')
                return { -1, currentStock }
            end
            local stock = tonumber(redis.call('GET', KEYS[1]))
            if stock == nil then
                return { -3, 0 }
            end
            if stock <= 0 then
                return { -2, 0 }
            end
            local remaining = redis.call('DECR', KEYS[1])
            redis.call('SADD', KEYS[2], ARGV[1])
            return { 1, remaining }
            """;

    private static final String ROLLBACK_LUA_SCRIPT = """
            if redis.call('SREM', KEYS[2], ARGV[1]) == 1 then
                return redis.call('INCR', KEYS[1])
            end
            return tonumber(redis.call('GET', KEYS[1]) or '0')
            """;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> issueScript;
    private final DefaultRedisScript<Long> rollbackScript;

    public RedisCouponStockAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        this.issueScript = new DefaultRedisScript<>();
        this.issueScript.setScriptText(ISSUE_LUA_SCRIPT);
        this.issueScript.setResultType(List.class);

        this.rollbackScript = new DefaultRedisScript<>();
        this.rollbackScript.setScriptText(ROLLBACK_LUA_SCRIPT);
        this.rollbackScript.setResultType(Long.class);
    }

    @Override
    public void initializeStock(Long couponId, Long totalQuantity) {
        String stockKey = stockKey(couponId);
        String usersKey = usersKey(couponId);
        redisTemplate.delete(Arrays.asList(stockKey, usersKey));
        redisTemplate.opsForValue().set(stockKey, String.valueOf(totalQuantity));
    }

    @Override
    @SuppressWarnings("unchecked")
    public CouponIssueResult tryIssueAtomic(Long couponId, Long userId) {
        List<Object> rawResult = redisTemplate.execute(
                issueScript,
                Arrays.asList(stockKey(couponId), usersKey(couponId)),
                String.valueOf(userId)
        );

        if (rawResult == null || rawResult.size() < 2) {
            return new CouponIssueResult(
                    couponId,
                    userId,
                    CouponIssueStatus.COUPON_NOT_FOUND,
                    0L,
                    "Redis 응답을 확인할 수 없습니다."
            );
        }

        long code = ((Number) rawResult.get(0)).longValue();
        long remaining = ((Number) rawResult.get(1)).longValue();

        if (code == 1L) {
            return new CouponIssueResult(
                    couponId,
                    userId,
                    CouponIssueStatus.SUCCESS,
                    remaining,
                    "선착순 쿠폰 발급 요청이 접수되었습니다. (비동기 저장 진행 중)"
            );
        } else if (code == -1L) {
            return new CouponIssueResult(
                    couponId,
                    userId,
                    CouponIssueStatus.DUPLICATED,
                    remaining,
                    "이미 발급받은 유저입니다."
            );
        } else if (code == -2L) {
            return new CouponIssueResult(
                    couponId,
                    userId,
                    CouponIssueStatus.SOLD_OUT,
                    0L,
                    "선착순 쿠폰 수량이 모두 소진되었습니다."
            );
        } else {
            return new CouponIssueResult(
                    couponId,
                    userId,
                    CouponIssueStatus.COUPON_NOT_FOUND,
                    0L,
                    "존재하지 않는 쿠폰입니다."
            );
        }
    }

    @Override
    public void rollbackIssue(Long couponId, Long userId) {
        redisTemplate.execute(
                rollbackScript,
                Arrays.asList(stockKey(couponId), usersKey(couponId)),
                String.valueOf(userId)
        );
    }

    @Override
    public Optional<Long> getRemainingStock(Long couponId) {
        String value = redisTemplate.opsForValue().get(stockKey(couponId));
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private String stockKey(Long couponId) {
        return "coupon:" + couponId + ":stock";
    }

    private String usersKey(Long couponId) {
        return "coupon:" + couponId + ":users";
    }
}

