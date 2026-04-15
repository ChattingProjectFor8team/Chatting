package com.example.infinite.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 서비스 권한 체계
 * 1. SUPER_ADMIN: 시스템 전체 관리
 * 2. ARTIST_ADMIN: 아티스트 페이지 관리 및 아티스트 본인
 * 3. SUBSCRIBER: 유료 구독 팬 (팬레터, DM 가능)
 * 4. USER: 일반 팬 (멤버십 미가입자 포함)
 */
@Getter
@RequiredArgsConstructor
public enum UserRole {

    ROLE_SUPER_ADMIN("ROLE_SUPER_ADMIN", "시스템 관리자"),
    ROLE_ARTIST_ADMIN("ROLE_ARTIST_ADMIN", "아티스트 및 멤버"),
    ROLE_SUBSCRIBER("ROLE_SUBSCRIBER", "유료 구독 팬"),
    ROLE_USER("ROLE_USER", "일반 팬");

    private final String authority; // 시큐리티 인증용 (ROLE_ 접두사 포함)
    private final String description; // 설명용
}
