package com.example.infinite.domain.member.member.controller;

import com.example.infinite.domain.member.member.dto.request.ChangeEmailRequest;
import com.example.infinite.domain.member.member.dto.request.ChangePasswordRequest;
import com.example.infinite.domain.member.member.dto.request.UpdateMemberRequest;
import com.example.infinite.domain.member.member.dto.request.UpdateMemberMultipartRequest;
import com.example.infinite.domain.member.member.dto.response.MyInfoResponse;
import com.example.infinite.domain.member.member.dto.response.TokenResponse;
import com.example.infinite.domain.member.member.service.MemberService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 현재 로그인한 멤버의 기본 프로필 정보를 조회한다.
    @GetMapping("v1/me")
    public ResponseEntity<ApiResponse<MyInfoResponse>> getMyInfo(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberService.getMyInfo(memberDetails)));
    }

    // 닉네임/전화번호/프로필 이미지를 한 번에 수정한다.
    @PatchMapping(value = "v1/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<MyInfoResponse>> updateMyInfo(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @Valid @RequestBody UpdateMemberRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberService.updateMyInfo(memberDetails, request)));
    }

    // 같은 경로에 multipart 버전을 추가해
    // "문자열 필드 + 실제 파일 업로드"를 한 요청에서 같이 받을 수 있게 한다.
    @PatchMapping(value = "v1/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MyInfoResponse>> updateMyInfoMultipart(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @Valid @ModelAttribute UpdateMemberMultipartRequest request
    ) {
        // 서비스는 기존 UpdateMemberRequest 를 재사용하고,
        // 파일만 별도 파라미터로 넘겨 JSON/multipart 두 흐름의 비즈니스 로직을 통합한다.
        return ResponseEntity.ok(ApiResponse.success(memberService.updateMyInfo(
                memberDetails,
                new UpdateMemberRequest(
                        request.getNickname(),
                        request.getPhoneNumber(),
                        request.getProfileImageUrl(),
                        request.getCoverImageUrl()
                ),
                request.getProfileImageFile(),
                request.getCoverImageFile()
        )));
    }

    // 비밀번호 변경은 현재 비밀번호 검증을 통과해야만 가능하다.
    @PatchMapping("v1/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        memberService.changePassword(memberDetails, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // 이메일 변경 후에는 새 subject 기준 토큰을 다시 발급한다.
    @PatchMapping("v1/me/email")
    public ResponseEntity<ApiResponse<TokenResponse>> changeEmail(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @Valid @RequestBody ChangeEmailRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(memberService.changeEmail(memberDetails, request)));
    }
}
