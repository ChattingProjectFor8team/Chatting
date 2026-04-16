package com.example.infinite.domain.payment.controller;

import com.example.infinite.domain.payment.dto.request.ChargeSettingRequest;
import com.example.infinite.domain.payment.dto.response.AutoChargeHistoryResponse;
import com.example.infinite.domain.payment.dto.response.ChargeSettingResponse;
import com.example.infinite.domain.payment.service.ChargeService;
import com.example.infinite.global.common.dto.ApiResponse;
import com.example.infinite.global.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment/v1/charge")
public class ChargeController {

    private final ChargeService chargeService;

    // 자동충전 설정 등록/수정
    @PostMapping("/settings")
    public ApiResponse<ChargeSettingResponse> saveSetting(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody @Valid ChargeSettingRequest request) {
        return ApiResponse.success(chargeService.save(userId, request));
    }

    // 자동충전 설정 조회
    @GetMapping("/settings")
    public ApiResponse<ChargeSettingResponse> getSetting(
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(chargeService.getSetting(userId));
    }

    // 자동충전 설정 해제
    @DeleteMapping("/settings")
    public ApiResponse<Void> disableSetting(
            @RequestHeader("X-User-Id") Long userId) {
        chargeService.disable(userId);
        return ApiResponse.success(null);
    }

    // 자동충전 이력 조회 (최신순, 기본 20개)
    @GetMapping("/histories")
    public ApiResponse<PageResponse<AutoChargeHistoryResponse>> getHistories(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(
                new PageResponse<>(chargeService.getHistories(userId, pageable)));
    }
}