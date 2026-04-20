package com.example.infinite.domain.artistcontent.post.fanpost.dto.request;

import jakarta.validation.constraints.Size;

public record FanPostUpdateRequest(
        // 수정은 부분 변경이므로 null이면 기존 본문 유지로 해석한다.
        @Size(max = 5000)
        String content
) {
}
