package com.example.infinite.domain.member.member.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 내 정보 수정에서 사용하는 프로필성 필드 묶음이다.
public record UpdateMemberRequest(
        // 부분 수정이므로 null은 허용하고, 값이 오면 닉네임 길이를 검증한다.
        @Size(min = 2, max = 50)
        String nickname,

        // 전화번호는 null 허용, 값이 오면 010-1234-5678 형식만 받는다.
        @Pattern(regexp = "^$|^010-\\d{4}-\\d{4}$", message = "전화번호는 010-1234-5678 형식이어야 합니다.")
        @Size(max = 13)
        String phoneNumber,

        String profileImageUrl,

        String coverImageUrl
) {
}
