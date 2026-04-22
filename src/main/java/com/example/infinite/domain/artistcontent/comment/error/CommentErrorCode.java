package com.example.infinite.domain.artistcontent.comment.error;

import com.example.infinite.global.error.ErrorCodeType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements ErrorCodeType {

    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "C001", "존재하지 않거나 삭제된 댓글입니다."),
    COMMENT_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "C002", "해당 댓글을 수정 또는 삭제할 권한이 없습니다."),
    COMMENT_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "C003", "댓글은 2뎁스까지만 작성할 수 있습니다."),
    INVALID_PARENT_COMMENT(HttpStatus.BAD_REQUEST, "C004", "현재 게시글에 속한 원댓글만 부모로 지정할 수 있습니다."),
    INVALID_MENTION_SCOPE(HttpStatus.BAD_REQUEST, "C005", "멘션은 부모 댓글과 같은 스레드의 닉네임만 사용할 수 있습니다."),
    COMMENT_REQUEST_IN_PROGRESS(HttpStatus.CONFLICT, "C006", "댓글 처리 중입니다. 잠시 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
