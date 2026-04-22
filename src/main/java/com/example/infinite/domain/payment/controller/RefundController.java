package com.example.infinite.domain.payment.controller;

import com.example.infinite.domain.payment.service.RefundService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment/v1/refunds")
public class RefundController {

    private final RefundService refundService;

    // 수동결제 환불 — 결제일로부터 7일 이내, 젤리 미사용 조건 충족 시 PortOne 취소
    @PostMapping("/payments/{paymentId}")
    public ResponseEntity<ApiResponse<Void>> refundPayment(
            @AuthenticationPrincipal MemberDetailsImpl member,
            @PathVariable String paymentId) {
        refundService.refundPayment(member.getMemberId(), paymentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 자동충전 환불 — 충전일로부터 7일 이내, 젤리 미사용 조건 충족 시 PortOne 취소
    @PostMapping("/auto-charges/{historyId}")
    public ResponseEntity<ApiResponse<Void>> refundAutoCharge(
            @AuthenticationPrincipal MemberDetailsImpl member,
            @PathVariable Long historyId) {
        refundService.refundAutoCharge(member.getMemberId(), historyId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // DM 구독권 환불 — 구독일로부터 7일 이내, DM 미발송 조건 충족 시 젤리 반환
    @PostMapping("/dm-subscriptions/{subscriptionId}")
    public ResponseEntity<ApiResponse<Void>> refundDmSubscription(
            @AuthenticationPrincipal MemberDetailsImpl member,
            @PathVariable Long subscriptionId) {
        refundService.refundDmSubscription(member.getMemberId(), subscriptionId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
