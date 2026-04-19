package com.example.infinite.domain.artistcontent.post.fanpost.dto.request;

import jakarta.validation.constraints.Size;

public record FanPostCreateRequest(
        // 본문은 선택값으로 두고, 나중에 media write path가 붙으면 "본문 또는 미디어 중 하나 필수"로 검증한다.
        @Size(max = 5000)
        String content
) {
}
