package com.example.infinite.domain.subscriptionmembership.controller;

import com.example.infinite.domain.subscriptionmembership.dto.response.SubscriptionHistoryResponse;
import com.example.infinite.domain.subscriptionmembership.dto.response.SubscriptionStatusResponse;
import com.example.infinite.domain.subscriptionmembership.service.SubscriptionMembershipService;
import com.example.infinite.global.common.dto.ApiResponse;
import com.example.infinite.global.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionMembershipController {

    private final SubscriptionMembershipService subscriptionMembershipService;

    @PostMapping("/dm/{artistId}")
    public ApiResponse<Void> purchaseDmSubscription(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long artistId) {
        subscriptionMembershipService.purchaseDmSubscription(userId, artistId);
        return ApiResponse.success(null);
    }

    @PostMapping("/membership/{artistId}")
    public ApiResponse<Void> purchaseFanMembership(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long artistId) {
        subscriptionMembershipService.purchaseFanMembership(userId, artistId);
        return ApiResponse.success(null);
    }

    @GetMapping("/dm/{artistId}/status")
    public ApiResponse<SubscriptionStatusResponse> getDmSubscriptionStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long artistId) {
        return ApiResponse.success(subscriptionMembershipService.getDmSubscriptionStatus(userId, artistId));
    }

    @GetMapping("/membership/{artistId}/status")
    public ApiResponse<SubscriptionStatusResponse> getFanMembershipStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long artistId) {
        return ApiResponse.success(subscriptionMembershipService.getFanMembershipStatus(userId, artistId));
    }

    @GetMapping("/dm/history")
    public ApiResponse<PageResponse<SubscriptionHistoryResponse>> getDmSubscriptionHistory(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(subscriptionMembershipService.getDmSubscriptionHistory(userId, pageable));
    }

    @GetMapping("/membership/history")
    public ApiResponse<PageResponse<SubscriptionHistoryResponse>> getFanMembershipHistory(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(subscriptionMembershipService.getFanMembershipHistory(userId, pageable));
    }
}
