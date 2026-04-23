package com.example.infinite.domain.realtimelive.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 실제 인코딩/저장 완료 후 공개 가능한 다시보기 URL만 받아 VOD 공개 상태로 전환한다.
public record LiveReplayPublishRequest(
        @NotBlank
        @Size(max = 500)
        String replayUrl
) {
}
