package com.example.infinite.domain.member.artist.error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ArtistErrorCode implements ErrorCodeType {

    ARTIST_NOT_FOUND(HttpStatus.NOT_FOUND, "AR001", "아티스트 정보를 찾을 수 없습니다."),
    ARTIST_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "AR002", "아티스트 멤버 정보를 찾을 수 없습니다."),
    MEDIA_PROFILE_REQUIRED(HttpStatus.BAD_REQUEST, "M006", "아티스트 가입을 위해 프로필 사진 등록이 필수입니다."),
    ARTIST_CREATE_ROLE_REQUIRED(HttpStatus.FORBIDDEN, "AR003", "아티스트 권한을 가진 회원만 아티스트를 생성할 수 있습니다."),
    ARTIST_MEMBER_ALREADY_LINKED(HttpStatus.BAD_REQUEST, "AR004", "이미 아티스트 멤버에 소속된 회원입니다."),
    ARTIST_SLUG_DUPLICATED(HttpStatus.BAD_REQUEST, "AR005", "이미 사용 중인 아티스트 슬러그입니다."),
    ARTIST_MANAGE_DENIED(HttpStatus.FORBIDDEN, "AR006", "해당 아티스트를 수정하거나 삭제할 권한이 없습니다."),
    ARTIST_MEMBER_ROLE_REQUIRED(HttpStatus.FORBIDDEN, "AR007", "아티스트 권한을 가진 회원만 아티스트 멤버로 추가할 수 있습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
