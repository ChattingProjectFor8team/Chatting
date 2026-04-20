package com.example.infinite.domain.member.member.service;

import com.example.infinite.domain.member.member.dto.request.ChangeEmailRequest;
import com.example.infinite.domain.member.member.dto.request.ChangePasswordRequest;
import com.example.infinite.domain.member.member.dto.request.UpdateMemberRequest;
import com.example.infinite.domain.member.member.dto.request.UpdateMemberRoleRequest;
import com.example.infinite.domain.member.member.dto.request.UpdateMemberStatusRequest;
import com.example.infinite.domain.member.member.dto.response.AdminMemberResponse;
import com.example.infinite.domain.member.member.dto.response.MyInfoResponse;
import com.example.infinite.domain.member.member.dto.response.TokenResponse;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.error.MemberErrorCode;
import com.example.infinite.domain.member.member.error.MemberException;
import com.example.infinite.domain.member.member.enums.MemberRole;
import com.example.infinite.domain.member.member.repository.MemberRepository;
import com.example.infinite.domain.member.member.support.MemberReader;
import com.example.infinite.domain.member.member.support.MemberInputSupport;
import com.example.infinite.global.auth.JwtTokenProvider;
import com.example.infinite.global.auth.MemberDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberReader memberReader;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public MyInfoResponse getMyInfo(MemberDetailsImpl memberDetails) {
        // 토큰의 subject(email)로 현재 로그인 멤버를 식별한다.
        return MyInfoResponse.from(memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails)));
    }

    public MyInfoResponse updateMyInfo(MemberDetailsImpl memberDetails, UpdateMemberRequest request) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));

        // null/blank 입력은 기존 값 유지로 해석해 부분 수정처럼 동작시킨다.
        String nextNickname = MemberInputSupport.trimToNull(request.nickname());
        String nextPhoneNumber = MemberInputSupport.trimToNull(request.phoneNumber());
        String nextProfileImageUrl = MemberInputSupport.trimToNull(request.profileImageUrl());

        // 본인 번호를 제외한 다른 회원의 전화번호와 충돌하면 수정하지 못하게 막는다.
        validatePhoneNumberDuplication(member.getId(), nextPhoneNumber);
        member.updateProfile(
                nextNickname != null ? nextNickname : member.getNickname(),
                nextPhoneNumber != null ? nextPhoneNumber : member.getPhoneNumber(),
                nextProfileImageUrl != null ? nextProfileImageUrl : member.getProfileImageUrl()
        );

        return MyInfoResponse.from(member);
    }

    public void changePassword(MemberDetailsImpl memberDetails, ChangePasswordRequest request) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        // 현재 비밀번호 불일치 시 변경을 막아 계정 탈취성 변경을 방지한다.
        if (!passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
            throw new MemberException(MemberErrorCode.MEMBER_PASSWORD_MISMATCH);
        }

        member.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    public TokenResponse changeEmail(MemberDetailsImpl memberDetails, ChangeEmailRequest request) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        String nextEmail = MemberInputSupport.requireTrimmed(
                request.newEmail(),
                () -> new IllegalArgumentException("변경할 이메일은 필수입니다.")
        );

        validateEmailChange(member.getEmail(), nextEmail);
        member.changeEmail(nextEmail);

        // JWT subject가 email이므로 이메일 변경 직후 새 토큰을 재발급한다.
        String accessToken = jwtTokenProvider.createToken(member.getEmail(), member.getRole().getSecurityName());
        return new TokenResponse(accessToken, "Bearer");
    }

    public AdminMemberResponse changeRole(Long memberId, UpdateMemberRoleRequest request) {
        Member member = memberReader.findByIdOrThrow(memberId);
        // 역할 승격은 SUPER_ADMIN 전용 admin 경로에서만 호출되며,
        // 현재 정책상 USER -> ARTIST_ADMIN / USER -> SUPER_ADMIN 두 경우만 허용한다.
        validateRoleChange(member.getRole(), request.role());
        member.changeRole(request.role());
        return AdminMemberResponse.from(member);
    }

    public AdminMemberResponse changeStatus(Long memberId, UpdateMemberStatusRequest request) {
        Member member = memberReader.findByIdOrThrow(memberId);
        member.changeStatus(request.status());
        return AdminMemberResponse.from(member);
    }

    // 이메일 변경 API는 "실제 변경"만 허용하므로 현재 이메일과 같으면 바로 막는다.
    private void validateEmailChange(String currentEmail, String nextEmail) {
        if (currentEmail.equals(nextEmail)) {
            throw new MemberException(MemberErrorCode.MEMBER_SAME_EMAIL_NOT_ALLOWED);
        }

        // 현재 이메일과 다를 때만 전체 중복 검사를 해 로그인 식별자 충돌을 막는다.
        if (memberRepository.existsByEmail(nextEmail)) {
            throw new MemberException(MemberErrorCode.AUTH_DUPLICATE_EMAIL);
        }
    }


    private void validateRoleChange(MemberRole currentRole, MemberRole nextRole) {
        if (currentRole != MemberRole.USER) {
            throw new MemberException(MemberErrorCode.MEMBER_INVALID_ROLE_CHANGE);
        }

        if (nextRole != MemberRole.ARTIST_ADMIN && nextRole != MemberRole.SUPER_ADMIN) {
            throw new MemberException(MemberErrorCode.MEMBER_INVALID_ROLE_CHANGE);
        }
    }

    // 프로필 수정에서는 본인 번호는 허용하고, 다른 회원과의 충돌만 막는다.
    private void validatePhoneNumberDuplication(Long memberId, String phoneNumber) {
        if (phoneNumber != null && memberRepository.existsByPhoneNumberAndIdNot(phoneNumber, memberId)) {
            throw new MemberException(MemberErrorCode.MEMBER_DUPLICATE_PHONE_NUMBER);
        }
    }
}
