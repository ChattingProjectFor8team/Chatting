package com.example.infinite.domain.member.error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCodeType {

    AUTH_DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "A001", "이미 가입된 이메일 주소입니다."),
    AUTH_PENDING_APPROVAL(HttpStatus.FORBIDDEN, "A002", "관리자(Admin)의 승인이 대기 중인 계정입니다."),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A003", "이메일 또는 비밀번호가 일치하지 않습니다."),
    AUTH_BANNED_USER(HttpStatus.FORBIDDEN, "A004", "운영 정책 위반으로 제재를 받은 계정입니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "A005", "회원을 찾을 수 없습니다."),
    STATE_NOT_LOGIN(HttpStatus.UNAUTHORIZED, "A006", "로그인되어 있지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
