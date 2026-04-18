package com.example.infinite.domain.member.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 회원가입 시 닉네임, 전화번호, 로그인 식별자 형식을 함께 검증한다.
public record SignUpRequest(
        @NotBlank
        @Email
        @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password,

        @NotBlank
        @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "전화번호는 010-1234-5678 형식이어야 합니다.")
        @Size(min = 13, max = 13)
        String phoneNumber,

        @NotBlank
        @Size(min = 2, max = 50)
        String nickname
) {
}
