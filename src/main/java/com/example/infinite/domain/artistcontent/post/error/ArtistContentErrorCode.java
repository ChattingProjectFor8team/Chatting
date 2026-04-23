package com.example.infinite.domain.artistcontent.post.error;

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
    POST_CONTENT_OR_MEDIA_REQUIRED(HttpStatus.BAD_REQUEST, "B006", "게시글은 본문 또는 첨부파일 중 하나가 필요합니다."),
    MEDIA_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "M001", "파일 업로드 중 서버 오류가 발생했습니다."),
    MEDIA_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "M002", "지원하지 않는 파일 형식입니다."),
    MEDIA_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "M003", "업로드 가능한 최대 용량을 초과했습니다."),
    MEDIA_RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "M004", "요청한 미디어 파일을 찾을 수 없습니다."),
    MEDIA_THUMBNAIL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "M005", "썸네일 생성 중 오류가 발생했습니다."),
    MEDIA_MAX_IMAGE_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "M006", "이미지는 최대 10장까지 업로드할 수 있습니다."),
    MEDIA_MAX_VIDEO_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "M007", "동영상은 한 개만 업로드할 수 있습니다."),
    MEDIA_MIXED_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "M008", "이미지와 동영상을 함께 업로드할 수 없습니다."),
    MEDIA_STORAGE_NOT_CONFIGURED(HttpStatus.INTERNAL_SERVER_ERROR, "M009", "미디어 저장소 설정이 준비되지 않았습니다."),
    MEDIA_FAN_LETTER_IMAGE_ONLY(HttpStatus.BAD_REQUEST, "M010", "팬레터는 이미지 한 장만 업로드할 수 있습니다."),
    MEDIA_YOUTUBE_API_KEY_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "M011", "유튜브 메타데이터 API 키가 설정되지 않았습니다."),
    MEDIA_YOUTUBE_METADATA_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "M012", "유튜브 메타데이터를 가져오지 못했습니다."),
    MEDIA_YOUTUBE_URL_INVALID(HttpStatus.BAD_REQUEST, "M013", "유효한 유튜브 링크 형식이 아닙니다."),
    MEDIA_YOUTUBE_VIDEO_NOT_FOUND(HttpStatus.NOT_FOUND, "M014", "유튜브 영상을 찾을 수 없습니다."),
    MEDIA_YOUTUBE_VIDEO_DUPLICATED(HttpStatus.BAD_REQUEST, "M015", "이미 등록된 유튜브 영상입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
