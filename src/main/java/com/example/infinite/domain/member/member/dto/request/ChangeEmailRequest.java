package com.example.infinite.domain.member.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 이메일은 JWT subject로 사용되므로 별도 요청으로 분리한다.
public record ChangeEmailRequest(
        @NotBlank
        @Email
        String newEmail
) {
}
