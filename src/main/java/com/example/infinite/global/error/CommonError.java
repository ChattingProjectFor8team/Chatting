package com.example.infinite.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonError {

    // 1. Common (공통)
    COMMON_INVALID_AUTHENTICATION(HttpStatus.UNAUTHORIZED, "C001", "인증이 필요합니다. (로그인 만료 포함)"),
    COMMON_ACCESS_DENIED(HttpStatus.FORBIDDEN, "C002", "해당 기능에 대한 접근 권한이 없습니다."),
    COMMON_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "C003", "잘못된 요청 형식 또는 파라미터입니다."),
    COMMON_RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "요청한 리소스를 찾을 수 없습니다."),
    COMMON_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C005", "서버 내부 오류가 발생했습니다."),
    COMMON_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "C006", "도배 방지를 위해 요청 횟수가 제한되었습니다."),

    // 2. Auth (인증/인가)
    AUTH_DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST, "A001", "이미 가입된 이메일 주소입니다."),
    AUTH_PENDING_APPROVAL(HttpStatus.FORBIDDEN, "A002", "관리자(Admin)의 승인이 대기 중인 계정입니다."),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A003", "이메일 또는 비밀번호가 일치하지 않습니다."),
    AUTH_BANNED_USER(HttpStatus.FORBIDDEN, "A004", "운영 정책 위반으로 제재를 받은 계정입니다."),

    // 3. Payment / Jelly (결제 및 재화)
    JELLY_INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "P001", "보유한 젤리 잔액이 부족합니다."),
    PAYMENT_VERIFICATION_FAILED(HttpStatus.BAD_REQUEST, "P002", "실체 결제 금액과 요청 금액이 일치하지 않습니다."),
    PAYMENT_AUTO_CHARGING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "P003", "월 단위 정기 결제 처리 중 오류가 발생했습니다."),
    JELLY_WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "P004", "사용자의 젤리 지갑 정보를 찾을 수 없습니다."),
    REFUND_NOT_ELIGIBLE(HttpStatus.BAD_REQUEST, "P005", "환불 정책 조건을 충족하지 않습니다."),

    // 4. DM (Direct Message)
    DM_SUBSCRIPTION_REQUIRED(HttpStatus.FORBIDDEN, "D001", "DM 구독 상태인 유저만 이용 가능합니다."),
    DM_ROOM_DUPLICATE(HttpStatus.BAD_REQUEST, "D002", "이미 해당 유저와 아티스트 사이의 방이 존재합니다."),
    DM_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "D003", "유효하지 않거나 개설되지 않은 DM 방입니다."),
    DM_SENDER_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "D004", "잘못된 발송자 타입입니다."),

    // 5. Fan Membership / DM Subscription (구독)
    SUB_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "S001", "이미 활성화된 멤버십 또는 구독권을 보유하고 있습니다."),
    SUB_ARTIST_NOT_FOUND(HttpStatus.NOT_FOUND, "S002", "구독하려는 아티스트 정보를 찾을 수 없습니다."),
    SUB_EXPIRED(HttpStatus.FORBIDDEN, "S003", "구독 기간이 만료되어 권한이 회수되었습니다."),

    // 6. Refund (환불)
    REFUND_PERIOD_EXPIRED(HttpStatus.BAD_REQUEST, "R001", "환불 가능 기간이 초과되었습니다."),
    REFUND_ALREADY_USED(HttpStatus.BAD_REQUEST, "R002", "서비스를 이미 사용하여 환불이 불가능합니다."),
    REFUND_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "R003", "이미 환불 처리가 완료된 주문입니다."),
    REFUND_NOT_FOUND(HttpStatus.NOT_FOUND, "R004", "환불 대상 주문 내역을 찾을 수 없습니다."),
    REFUND_PROCESSING_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "R005", "환불 처리 중 내부 시스템 오류가 발생했습니다."),
    REFUND_PARTIAL_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "R006", "부분 환불은 지원하지 않는 상품입니다."),

    // 7. Raffle (이벤트/래플)
    RAFFLE_NOT_FOUND(HttpStatus.NOT_FOUND, "E001", "해당 래플 이벤트를 찾을 수 없습니다."),
    RAFFLE_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "E002", "아티스트 관리자만 래플을 운영할 수 있습니다."),
    RAFFLE_ALREADY_ENTERED(HttpStatus.BAD_REQUEST, "E003", "이미 응모를 완료한 이벤트입니다."),
    RAFFLE_DRAW_NOT_STARTED(HttpStatus.BAD_REQUEST, "E004", "아직 추첨 기간이 아닙니다."),

    // 8. Post / Interaction (게시글/댓글)
    POST_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "B001", "해당 게시글 작성 권한이 없습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "B002", "존재하지 않거나 삭제된 게시물입니다."),
    COMMENT_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "B003", "댓글 작성 권한이 없는 유저입니다."),
    REACTION_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "B004", "이미 리액션을 표시한 게시물입니다."),
    POST_PROHIBITED_WORD(HttpStatus.BAD_REQUEST, "B005", "게시글 또는 댓글에 금칙어가 포함되어 있습니다."),

    // 9. LiveStreaming (라이브)
    LIVE_STREAM_NOT_FOUND(HttpStatus.NOT_FOUND, "L001", "진행 중인 라이브 방송 정보를 찾을 수 없습니다."),
    LIVE_THUMBNAIL_REQUIRED(HttpStatus.BAD_REQUEST, "L002", "라이브 송출을 위해 썸네일 등록이 필수입니다."),
    LIVE_CHAT_NOT_READY(HttpStatus.INTERNAL_SERVER_ERROR, "L003", "라이브 채팅 서버와의 연결에 실패했습니다."),

    // 10. Media (미디어/파일)
    MEDIA_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "M001", "파일 업로드 중 서버 오류가 발생했습니다."),
    MEDIA_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "M002", "지원하지 않는 파일 형식입니다."),
    MEDIA_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "M003", "업로드 가능한 최대 용량을 초과했습니다."),
    MEDIA_RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "M004", "요청한 미디어 파일을 찾을 수 없습니다."),
    MEDIA_THUMBNAIL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "M005", "썸네일 생성 중 오류가 발생했습니다."),
    MEDIA_PROFILE_REQUIRED(HttpStatus.BAD_REQUEST, "M006", "아티스트 가입을 위해 프로필 사진 등록이 필수입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
