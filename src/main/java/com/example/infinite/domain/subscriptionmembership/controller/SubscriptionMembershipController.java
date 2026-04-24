package com.example.infinite.domain.subscriptionmembership.controller;

import com.example.infinite.domain.subscriptionmembership.dto.response.SubscriptionHistoryResponse;
import com.example.infinite.domain.subscriptionmembership.dto.response.SubscriptionStatusResponse;
import com.example.infinite.domain.subscriptionmembership.service.SubscriptionMembershipService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.dto.ApiResponse;
import com.example.infinite.global.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionMembershipController {

    private final SubscriptionMembershipService subscriptionMembershipService;

    @PostMapping("/dm/{artistId}")
    public ApiResponse<Void> purchaseDmSubscription(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId) {
        subscriptionMembershipService.purchaseDmSubscription(memberDetails.getMemberId(), artistId);
        return ApiResponse.success(null);
    }

    @PostMapping("/membership/{artistId}")
    public ApiResponse<Void> purchaseFanMembership(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId) {
        subscriptionMembershipService.purchaseFanMembership(memberDetails.getMemberId(), artistId);
        return ApiResponse.success(null);
    }

    @GetMapping("/dm/{artistId}/status")
    public ApiResponse<SubscriptionStatusResponse> getDmSubscriptionStatus(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId) {
        return ApiResponse.success(subscriptionMembershipService.getDmSubscriptionStatus(memberDetails.getMemberId(), artistId));
    }

    @GetMapping("/membership/{artistId}/status")
    public ApiResponse<SubscriptionStatusResponse> getFanMembershipStatus(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId) {
        return ApiResponse.success(subscriptionMembershipService.getFanMembershipStatus(memberDetails.getMemberId(), artistId));
    }

    @GetMapping("/dm/history")
    public ApiResponse<PageResponse<SubscriptionHistoryResponse>> getDmSubscriptionHistory(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(subscriptionMembershipService.getDmSubscriptionHistory(memberDetails.getMemberId(), pageable));
    }

    @GetMapping("/membership/history")
    public ApiResponse<PageResponse<SubscriptionHistoryResponse>> getFanMembershipHistory(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(subscriptionMembershipService.getFanMembershipHistory(memberDetails.getMemberId(), pageable));
    }
}
