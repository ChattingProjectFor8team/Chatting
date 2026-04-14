package com.example.infinite.domain.ArtistContent.Post.error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ArtistContentErrorCode implements ErrorCodeType {

    POST_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "B001", "해당 게시글 작성 권한이 없습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "B002", "존재하지 않거나 삭제된 게시물입니다."),
    POST_PROHIBITED_WORD(HttpStatus.BAD_REQUEST, "B005", "게시글 또는 댓글에 금칙어가 포함되어 있습니다."),
    MEDIA_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "M001", "파일 업로드 중 서버 오류가 발생했습니다."),
    MEDIA_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "M002", "지원하지 않는 파일 형식입니다."),
    MEDIA_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "M003", "업로드 가능한 최대 용량을 초과했습니다."),
    MEDIA_RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "M004", "요청한 미디어 파일을 찾을 수 없습니다."),
    MEDIA_THUMBNAIL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "M005", "썸네일 생성 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
