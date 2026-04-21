package com.example.infinite.domain.realtimelive.error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LiveErrorCode implements ErrorCodeType {

    LIVE_NOT_FOUND(HttpStatus.NOT_FOUND, "RL001", "라이브를 찾을 수 없습니다."),
    CHAT_MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "RL002", "채팅 메시지를 찾을 수 없습니다."),
    LIVE_NOT_SCHEDULED(HttpStatus.CONFLICT, "RL003", "예정 상태의 라이브만 시작할 수 있습니다."),
    LIVE_NOT_LIVE(HttpStatus.CONFLICT, "RL004", "진행 중인 라이브만 종료할 수 있습니다."),
    NOT_LIVE_OWNER(HttpStatus.FORBIDDEN, "RL005", "해당 라이브의 소유자가 아닙니다."),
    MESSAGE_ALREADY_DELETED(HttpStatus.CONFLICT, "RL006", "이미 삭제된 메시지입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
