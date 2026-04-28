package com.example.infinite.domain.payment.controller;

import com.example.infinite.domain.payment.dto.response.JellyBalance;
import com.example.infinite.domain.payment.dto.response.JellyHistory;
import com.example.infinite.domain.payment.service.JellyService;
import com.example.infinite.global.common.dto.ApiResponse;
import com.example.infinite.global.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment/v1/jelly")
public class JellyController {

    private final JellyService jellyService;

    // 젤리 잔액 조회
    @GetMapping("/balance")
    public ApiResponse<JellyBalance> getBalance(
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(jellyService.getBalance(userId));
    }

    // 젤리 거래 이력 조회 (최신순, 기본 20개)
    @GetMapping("/histories")
    public ApiResponse<PageResponse<JellyHistory>> getHistories(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(
                new PageResponse<>(jellyService.getHistory(userId, pageable)));
    }
}