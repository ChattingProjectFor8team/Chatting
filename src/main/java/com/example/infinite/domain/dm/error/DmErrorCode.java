package com.example.infinite.domain.dm.error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DmErrorCode implements ErrorCodeType {

    DM_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "DM001", "DM 룸을 찾을 수 없습니다."),
    DM_SUBSCRIPTION_REQUIRED(HttpStatus.FORBIDDEN, "DM002", "DM 구독 상태인 유저만 이용 가능합니다."),
    DM_REPLY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "DM003", "아티스트 답장 전 최대 3건까지만 보낼 수 있습니다."),
    DM_ROOM_ALREADY_EXISTS(HttpStatus.CONFLICT, "DM004", "이미 해당 아티스트와의 DM 룸이 존재합니다."),
    DM_NOT_ROOM_MEMBER(HttpStatus.FORBIDDEN, "DM005", "해당 DM 룸의 참여자가 아닙니다."),
    DM_ARTIST_MISMATCH(HttpStatus.FORBIDDEN, "DM006", "해당 아티스트의 DM이 아닙니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
