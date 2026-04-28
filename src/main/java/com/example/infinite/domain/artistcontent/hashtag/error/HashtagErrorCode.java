package com.example.infinite.domain.artistcontent.hashtag.error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HashtagErrorCode implements ErrorCodeType {

    HASHTAG_NOT_FOUND(HttpStatus.NOT_FOUND, "HT001", "해시태그를 찾을 수 없습니다."),
    INVALID_HASHTAG_TOKEN(HttpStatus.BAD_REQUEST, "HT002", "허용되지 않는 해시태그 형식입니다."),
    TOO_MANY_HASHTAGS(HttpStatus.BAD_REQUEST, "HT003", "게시물에 붙일 수 있는 해시태그 개수를 초과했습니다."),
    HASHTAG_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "HT004", "해시태그 이름이 너무 깁니다."),
    HASHTAG_SUGGESTION_LIMIT_INVALID(HttpStatus.BAD_REQUEST, "HT005", "해시태그 추천 조회 개수는 1 이상이어야 합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
