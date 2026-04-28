package com.example.infinite.domain.member.member.dto.request;

import jakarta.validation.constraints.NotBlank;

// 비밀번호 변경은 현재 비밀번호와 새 비밀번호를 함께 받는다.
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank String newPassword
) {
}
