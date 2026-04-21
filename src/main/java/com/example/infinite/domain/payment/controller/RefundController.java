package com.example.infinite.domain.payment.controller;

import com.example.infinite.domain.payment.service.RefundService;
import com.example.infinite.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment/v1/refunds")
public class RefundController {

    private final RefundService refundService;

    // 수동결제 환불 — 결제일로부터 7일 이내, 젤리 미사용 조건 충족 시 PortOne 취소
    @PostMapping("/payments/{paymentId}")
    public ApiResponse<Void> refundPayment(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable String paymentId) {
        refundService.refundPayment(userId, paymentId);
        return ApiResponse.success(null);
    }

    // 자동충전 환불 — 충전일로부터 7일 이내, 젤리 미사용 조건 충족 시 PortOne 취소
    @PostMapping("/auto-charges/{historyId}")
    public ApiResponse<Void> refundAutoCharge(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long historyId) {
        refundService.refundAutoCharge(userId, historyId);
        return ApiResponse.success(null);
    }

    // DM 구독권 환불 — 구독일로부터 7일 이내, DM 미발송 조건 충족 시 젤리 반환
    @PostMapping("/dm-subscriptions/{subscriptionId}")
    public ApiResponse<Void> refundDmSubscription(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long subscriptionId) {
        refundService.refundDmSubscription(userId, subscriptionId);
        return ApiResponse.success(null);
    }

    // 팬 멤버십 환불 — 정책상 환불 불가, 클라이언트에 명확한 에러 반환
    @PostMapping("/fan-memberships/{membershipId}")
    public ApiResponse<Void> refundFanMembership(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long membershipId) {
        refundService.refundFanMembership();
        return ApiResponse.success(null);
    }
}
