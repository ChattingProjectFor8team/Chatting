package com.example.infinite.domain.artistcontent.comment.service.cache;

import com.example.infinite.domain.artistcontent.comment.dto.response.CommentResponse;
import com.example.infinite.global.common.dto.CursorSliceResponse;

/**
 * 대댓글 목록 캐시 래퍼다.
 * reply list도 제네릭 CursorSliceResponse<CommentResponse>를 직접 Cache.get(..., Class)로 읽기 어렵기 때문에 감싼다.
 */
public record CommentReplyListCacheEntry(
        CursorSliceResponse<CommentResponse> value
) {
}
