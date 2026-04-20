package com.example.infinite.domain.member.member.controller;

import com.example.infinite.domain.member.member.dto.request.UpdateMemberRoleRequest;
import com.example.infinite.domain.member.member.dto.request.UpdateMemberStatusRequest;
import com.example.infinite.domain.member.member.dto.response.AdminMemberResponse;
import com.example.infinite.domain.member.member.service.MemberService;
import com.example.infinite.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member/admin")
@RequiredArgsConstructor
public class AdminMemberController {

    private final MemberService memberService;

    // admin 경로는 SecurityConfig에서 hasRole("ADMIN")으로 보호되므로 SUPER_ADMIN만 접근한다.
    // 현재 과제 규칙상 USER를 ARTIST_ADMIN 또는 SUPER_ADMIN으로만 승격시킨다.
    @PatchMapping("v1/members/{memberId}/role")
    public ResponseEntity<ApiResponse<AdminMemberResponse>> changeRole(
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberRoleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberService.changeRole(memberId, request)));
    }

    // 상태 변경은 관리 목적의 별도 엔드포인트로 분리한다.
    @PatchMapping("v1/members/{memberId}/status")
    public ResponseEntity<ApiResponse<AdminMemberResponse>> changeStatus(
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberService.changeStatus(memberId, request)));
    }
}
