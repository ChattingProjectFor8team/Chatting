package com.example.infinite.domain.artistcontent.post.fanpost.dto.response;

import java.util.List;

// HOT 전용 base slice 는 일반 latest slice 와 달리
// 다음 페이지 계산에 필요한 score/id 커서를 함께 들고 다닌다.
public record FanPostHotBaseSlice(
        List<FanPostBaseResponse> content,
        Long nextScoreCursor,
        Long nextIdCursor,
        boolean hasNext,
        int size
) {
}
