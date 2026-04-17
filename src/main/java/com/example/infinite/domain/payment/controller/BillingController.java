package com.example.infinite.domain.payment.controller;

import com.example.infinite.domain.payment.dto.request.BillingKeyRequest;
import com.example.infinite.domain.payment.dto.response.BillingKeyResponse;
import com.example.infinite.domain.payment.service.BillingService;
import com.example.infinite.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment/v1/billings")
public class BillingController {

    private final BillingService billingService;

    // 카드 등록
    @PostMapping
    public ApiResponse<BillingKeyResponse> register(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid BillingKeyRequest request) {
        return ApiResponse.success(billingService.register(userId, request));
    }

    // 카드 목록 조회
    @GetMapping
    public ApiResponse<List<BillingKeyResponse>> getList(
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(billingService.getList(userId));
    }

    // 카드 삭제
    @DeleteMapping("/{billingId}")
    public ApiResponse<Void> delete(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long billingId) {
        billingService.delete(userId, billingId);
        return ApiResponse.success(null);
    }
}