package com.example.demo.architecture.adapter.in;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.architecture.adapter.in.dto.CouponIssueHistoryResponse;
import com.example.demo.architecture.adapter.in.dto.CouponIssueResultResponse;
import com.example.demo.architecture.adapter.in.dto.CouponResponse;
import com.example.demo.architecture.adapter.in.dto.CreateCouponRequest;
import com.example.demo.architecture.adapter.in.dto.IssueCouponRequest;
import com.example.demo.architecture.application.CouponUseCase;
import com.example.demo.architecture.domain.Coupon;
import com.example.demo.architecture.domain.CouponIssueResult;
import com.example.demo.architecture.domain.CouponIssueStatus;

@RestController
@RequestMapping("/coupons")
public class CouponController {

    private final CouponUseCase couponUseCase;

    public CouponController(CouponUseCase couponUseCase) {
        this.couponUseCase = couponUseCase;
    }

    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(@RequestBody CreateCouponRequest request) {
        Coupon created = couponUseCase.createCoupon(
                request.id(),
                request.title(),
                request.totalQuantity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(CouponResponse.from(created));
    }

    @GetMapping
    public List<CouponResponse> getAllCoupons() {
        return couponUseCase.getAllCoupons().stream()
                .map(CouponResponse::from)
                .toList();
    }

    @PostMapping("/{couponId}/issue")
    public ResponseEntity<CouponIssueResultResponse> issueCoupon(
            @PathVariable Long couponId,
            @RequestBody IssueCouponRequest request
    ) {
        CouponIssueResult result = couponUseCase.requestIssue(couponId, request.userId());

        HttpStatus status = switch (result.getStatus()) {
            case SUCCESS -> HttpStatus.ACCEPTED;
            case DUPLICATED, SOLD_OUT -> HttpStatus.CONFLICT;
            case COUPON_NOT_FOUND -> HttpStatus.NOT_FOUND;
        };

        return ResponseEntity.status(status).body(CouponIssueResultResponse.from(result));
    }

    @GetMapping("/{couponId}/issues")
    public List<CouponIssueHistoryResponse> getIssuedHistory(@PathVariable Long couponId) {
        return couponUseCase.getIssuedHistory(couponId).stream()
                .map(CouponIssueHistoryResponse::from)
                .toList();
    }
}

