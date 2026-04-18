package com.example.infinite.domain.member.support;

import com.example.infinite.domain.member.member.error.MemberErrorCode;
import com.example.infinite.domain.member.member.error.MemberException;
import com.example.infinite.global.auth.MemberDetailsImpl;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

public final class MemberInputSupport {

    private MemberInputSupport() {
    }

    // 멤버 계열 서비스에서 인증 subject(email) 추출 규칙을 한 곳으로 모은다.
    public static String extractEmail(MemberDetailsImpl memberDetails) {
        if (memberDetails == null || !StringUtils.hasText(memberDetails.getEmail())) {
            throw new MemberException(MemberErrorCode.STATE_NOT_LOGIN);
        }
        return memberDetails.getEmail().trim();
    }

    // blank 입력은 null로 통일해 선택 필드 부분 수정 로직을 단순화한다.
    public static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    // 필수 문자열은 trim 후 검증까지 함께 처리해 멤버 계열 서비스의 중복 코드를 줄인다.
    public static String requireTrimmed(String value, Supplier<? extends RuntimeException> exceptionSupplier) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw exceptionSupplier.get();
        }
        return normalized;
    }
}
