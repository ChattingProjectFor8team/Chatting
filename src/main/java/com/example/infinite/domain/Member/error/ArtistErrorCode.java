package com.example.infinite.domain.Member.error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ArtistErrorCode implements ErrorCodeType {

    ARTIST_NOT_FOUND(HttpStatus.NOT_FOUND, "AR001", "아티스트 정보를 찾을 수 없습니다."),
    ARTIST_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "AR002", "아티스트 멤버 정보를 찾을 수 없습니다."),
    MEDIA_PROFILE_REQUIRED(HttpStatus.BAD_REQUEST, "M006", "아티스트 가입을 위해 프로필 사진 등록이 필수입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
