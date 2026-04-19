package com.example.infinite.domain.artistcontent.post.fanpost.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FanPostCreateRequest(
        // 본문은 사용자 입력 원문을 보존하되 길이 제한만 DTO 레벨에서 먼저 검증한다.
        @NotBlank
        @Size(max = 5000)
        String content
) {
}
