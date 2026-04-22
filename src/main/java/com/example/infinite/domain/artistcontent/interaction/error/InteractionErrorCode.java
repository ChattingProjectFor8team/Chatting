package com.example.infinite.domain.artistcontent.interaction.error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InteractionErrorCode implements ErrorCodeType {

    COMMENT_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "B003", "댓글 작성 권한이 없는 유저입니다."),
    REACTION_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "B004", "이미 리액션을 표시한 게시물입니다."),
    LIKE_REQUEST_IN_PROGRESS(HttpStatus.CONFLICT, "B005", "좋아요 처리 중입니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
