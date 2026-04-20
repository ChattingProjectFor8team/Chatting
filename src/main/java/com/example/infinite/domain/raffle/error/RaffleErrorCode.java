package com.example.infinite.domain.raffle.error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RaffleErrorCode implements ErrorCodeType {

    RAFFLE_NOT_FOUND(HttpStatus.NOT_FOUND, "RF001", "해당 래플을 찾을 수 없습니다."),
    RAFFLE_ARTIST_MISMATCH(HttpStatus.FORBIDDEN, "RF002", "해당 아티스트의 래플이 아닙니다."),
    RAFFLE_NOT_PENDING(HttpStatus.BAD_REQUEST, "RF003", "대기 상태의 래플만 시작할 수 있습니다."),
    RAFFLE_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "RF004", "현재 진행 중인 래플이 아닙니다."),
    RAFFLE_ALREADY_ENTERED(HttpStatus.BAD_REQUEST, "RF005", "이미 응모를 완료한 래플입니다."),
    RAFFLE_SLOT_CLOSED(HttpStatus.BAD_REQUEST, "RF006", "현재 슬롯이 마감되었습니다."),
    RAFFLE_MEMBERSHIP_REQUIRED(HttpStatus.FORBIDDEN, "RF007", "멤버십 회원만 응모할 수 있는 래플입니다."),
    RAFFLE_INVALID_REWARD_TYPE(HttpStatus.BAD_REQUEST, "RF008", "현재 지원하지 않는 보상 유형입니다."),
    RAFFLE_CANCEL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "RF009", "완료되었거나 이미 취소된 래플입니다."),
    RAFFLE_ENTRY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "RF010", "응모 처리 중 오류가 발생했습니다."),
    RAFFLE_WINNER_NOT_FOUND(HttpStatus.NOT_FOUND, "RF011", "해당 당첨 정보를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}