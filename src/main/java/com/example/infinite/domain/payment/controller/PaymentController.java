package com.example.infinite.domain.payment.controller;

import com.example.infinite.domain.payment.dto.request.PaymentPrepareRequest;
import com.example.infinite.domain.payment.dto.request.PortOneWebhookRequest;
import com.example.infinite.domain.payment.dto.response.PaymentPrepareResponse;
import com.example.infinite.domain.payment.service.PaymentService;
import com.example.infinite.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    // 결제 준비 — paymentId 발급
    // 클라이언트는 응답받은 paymentId와 amount를 PortOne SDK에 전달해 결제 진행
    @PostMapping("/prepare")
    public ApiResponse<PaymentPrepareResponse> prepare(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PaymentPrepareRequest request) {
        return ApiResponse.success(paymentService.prepare(userId, request));
    }

    // PortOne 웹훅 수신 — 결제 완료 시 PortOne 서버에서 직접 호출
    // 인증 헤더 불필요, 내부에서 PortOne API 재검증으로 위변조 방지
    @PostMapping("/webhook")
    public ApiResponse<Void> webhook(@RequestBody PortOneWebhookRequest request) {
        paymentService.handleWebhook(request);
        return ApiResponse.success(null);
    }
}
