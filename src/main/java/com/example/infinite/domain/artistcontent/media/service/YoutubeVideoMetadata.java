package com.example.infinite.domain.artistcontent.media.service;

import java.time.LocalDateTime;

// 유튜브 API 응답 전체를 서비스가 알 필요는 없으므로 카드 렌더링에 필요한 값만 압축한다.
public record YoutubeVideoMetadata(
        String youtubeVideoId,
        String youtubeUrl,
        String title,
        String thumbnailUrl,
        long durationSeconds,
        LocalDateTime publishedAt
) {
}
