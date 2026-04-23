package com.example.infinite.domain.artistcontent.comment.service.cache;

import com.example.infinite.domain.artistcontent.comment.dto.response.CommentResponse;
import com.example.infinite.global.common.dto.CursorSliceResponse;

/**
 * 제네릭 댓글 슬라이스를 Spring Cache에 안전하게 넣고 꺼내기 위한 래퍼다.
 *
 * raw CursorSliceResponse.class 로 직접 캐스팅하면 내부 제네릭 정보가 흐려질 수 있어
 * 전용 entry record로 한 번 감싼다.
 */
public record CommentRootSliceCacheEntry(
        CursorSliceResponse<CommentResponse> value
) {
}
