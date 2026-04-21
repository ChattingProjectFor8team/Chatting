package com.example.infinite.domain.member.member.service;

import com.example.infinite.domain.artistcontent.media.service.AssetImageService;
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
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberReader memberReader;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AssetImageService assetImageService;

    @Transactional(readOnly = true)
    public MyInfoResponse getMyInfo(MemberDetailsImpl memberDetails) {
        // 토큰의 subject(email)로 현재 로그인 멤버를 식별한다.
        return MyInfoResponse.from(memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails)));
    }

    public MyInfoResponse updateMyInfo(MemberDetailsImpl memberDetails, UpdateMemberRequest request) {
        // 기존 JSON API 와의 호환을 위해 파일 없는 경로는 그대로 유지하고,
        // 내부적으로만 "파일 포함 버전" 오버로드로 위임한다.
        return updateMyInfo(memberDetails, request, null, null);
    }

    public MyInfoResponse updateMyInfo(
            MemberDetailsImpl memberDetails,
            UpdateMemberRequest request,
            MultipartFile profileImageFile,
            MultipartFile coverImageFile
    ) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));

        // 문자열 URL 과 실제 업로드 파일을 동시에 받을 수 있으므로
        // "파일이 오면 새 업로드 URL 우선, 아니면 문자열 URL, 그것도 없으면 기존 값 유지" 규칙으로 해석한다.
        String nextNickname = MemberInputSupport.trimToNull(request.nickname());
        String nextPhoneNumber = MemberInputSupport.trimToNull(request.phoneNumber());
        String nextProfileImageUrl = resolveNextProfileImageUrl(member, request.profileImageUrl(), profileImageFile);
        String nextCoverImageUrl = resolveNextCoverImageUrl(member, request.coverImageUrl(), coverImageFile);

        // 본인 번호를 제외한 다른 회원의 전화번호와 충돌하면 수정하지 못하게 막는다.
        validateNicknameDuplication(member.getId(), nextNickname);
        validatePhoneNumberDuplication(member.getId(), nextPhoneNumber);
        member.updateProfile(
                nextNickname != null ? nextNickname : member.getNickname(),
                nextPhoneNumber != null ? nextPhoneNumber : member.getPhoneNumber(),
                nextProfileImageUrl,
                nextCoverImageUrl
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
        String accessToken = jwtTokenProvider.createToken(member.getEmail(), member.getRole().name(), member.getId());
        return new TokenResponse(accessToken, "Bearer");
    }

    public AdminMemberResponse changeRole(Long memberId, UpdateMemberRoleRequest request) {
        Member member = memberReader.findByIdOrThrow(memberId);
        // 역할 승격은 SUPER_ADMIN 전용 admin 경로에서만 호출되며,
        // 현재 정책상 MEMBER -> ARTIST / MEMBER -> SUPER_ADMIN 두 경우만 허용한다.
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
        if (currentRole != MemberRole.MEMBER) {
            throw new MemberException(MemberErrorCode.MEMBER_INVALID_ROLE_CHANGE);
        }

        if (nextRole != MemberRole.ARTIST && nextRole != MemberRole.SUPER_ADMIN) {
            throw new MemberException(MemberErrorCode.MEMBER_INVALID_ROLE_CHANGE);
        }
    }

    private void validateNicknameDuplication(Long memberId, String nickname) {
        if (nickname != null && memberRepository.existsByNicknameAndIdNot(nickname, memberId)) {
            throw new MemberException(MemberErrorCode.MEMBER_DUPLICATE_NICKNAME);
        }
    }

    // 프로필 수정에서는 본인 번호는 허용하고, 다른 회원과의 충돌만 막는다.
    private void validatePhoneNumberDuplication(Long memberId, String phoneNumber) {
        if (phoneNumber != null && memberRepository.existsByPhoneNumberAndIdNot(phoneNumber, memberId)) {
            throw new MemberException(MemberErrorCode.MEMBER_DUPLICATE_PHONE_NUMBER);
        }
    }

    private String resolveNextProfileImageUrl(Member member, String requestedProfileImageUrl, MultipartFile profileImageFile) {
        if (profileImageFile != null && !profileImageFile.isEmpty()) {
            // 실제 파일이 오면 새 파일을 storage 에 올리고,
            // 이전 대표 이미지는 best-effort 로 정리한다.
            String uploadedImageUrl = assetImageService.uploadMemberProfileImage(member.getId(), profileImageFile);
            assetImageService.deleteByUrlQuietly(member.getProfileImageUrl());
            return uploadedImageUrl;
        }

        // 파일이 없을 때만 기존의 "URL 문자열 직접 저장" 경로를 허용한다.
        String normalizedRequestedUrl = MemberInputSupport.trimToNull(requestedProfileImageUrl);
        return normalizedRequestedUrl != null ? normalizedRequestedUrl : member.getProfileImageUrl();
    }

    private String resolveNextCoverImageUrl(Member member, String requestedCoverImageUrl, MultipartFile coverImageFile) {
        if (coverImageFile != null && !coverImageFile.isEmpty()) {
            // cover 역시 profile 과 동일한 규칙으로 처리한다.
            String uploadedImageUrl = assetImageService.uploadMemberCoverImage(member.getId(), coverImageFile);
            assetImageService.deleteByUrlQuietly(member.getCoverImageUrl());
            return uploadedImageUrl;
        }

        String normalizedRequestedUrl = MemberInputSupport.trimToNull(requestedCoverImageUrl);
        return normalizedRequestedUrl != null ? normalizedRequestedUrl : member.getCoverImageUrl();
    }
}
