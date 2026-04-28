package com.example.infinite.domain.artistcontent.media.enums;

// 현재 게시글 첨부 정책은 "이미지 묶음" 또는 "비디오 1개" 두 종류만 지원한다.
// 혼합 업로드 금지 정책과 최대 개수 제한은 이 enum 분류를 기준으로 동작한다.
public enum MediaType {
    IMAGE,
    VIDEO
}
