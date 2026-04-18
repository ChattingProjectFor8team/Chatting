package com.example.infinite.domain.dm.enums;

/**
 * DM 메시지 발송자 유형.
 * USER: 일반 회원이 보낸 메시지
 * ARTIST: 아티스트가 보낸 메시지 (fan-out 대상)
 */
public enum SenderType {
    USER,
    ARTIST
}