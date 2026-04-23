package com.example.infinite.domain.artistcontent.media.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Component
// 유튜브 링크 import 시 필요한 최소 카드 메타데이터만 외부 API에서 읽어오는 클라이언트다.
// 여기서는 "영상 카드 조립용 값"만 가져오고, 등록 권한/중복 검증은 서비스가 담당한다.
public class YoutubeMetadataClient {

    private final RestClient restClient;
    private final String apiKey;

    public YoutubeMetadataClient(@Value("${media.youtube.api-key:}") String apiKey) {
        this.apiKey = apiKey;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);

        this.restClient = RestClient.builder()
                .baseUrl("https://www.googleapis.com")
                .requestFactory(factory)
                .build();
    }

    public YoutubeVideoMetadata fetchVideoMetadata(String youtubeVideoId) {
        if (!StringUtils.hasText(apiKey)) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_YOUTUBE_API_KEY_MISSING);
        }

        try {
            // videos.list의 snippet + contentDetails 조합이면
            // 제목, 업로드 시각, 썸네일, ISO-8601 duration을 한 번에 가져올 수 있다.
            YoutubeVideosResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/youtube/v3/videos")
                            .queryParam("part", "snippet,contentDetails")
                            .queryParam("id", youtubeVideoId)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .body(YoutubeVideosResponse.class);

            if (response == null || response.items() == null || response.items().isEmpty()) {
                throw new ArtistContentException(ArtistContentErrorCode.MEDIA_YOUTUBE_VIDEO_NOT_FOUND);
            }

            YoutubeVideoItem item = response.items().getFirst();
            if (item.snippet() == null || item.contentDetails() == null) {
                throw new ArtistContentException(ArtistContentErrorCode.MEDIA_YOUTUBE_METADATA_FETCH_FAILED);
            }

            if (!StringUtils.hasText(item.snippet().title())
                    || !StringUtils.hasText(item.snippet().publishedAt())
                    || !StringUtils.hasText(item.contentDetails().duration())) {
                throw new ArtistContentException(ArtistContentErrorCode.MEDIA_YOUTUBE_METADATA_FETCH_FAILED);
            }

            return new YoutubeVideoMetadata(
                    youtubeVideoId,
                    YoutubeUrlParser.canonicalUrl(youtubeVideoId),
                    item.snippet().title(),
                    resolveThumbnailUrl(youtubeVideoId, item.snippet().thumbnails()),
                    Duration.parse(item.contentDetails().duration()).getSeconds(),
                    OffsetDateTime.parse(item.snippet().publishedAt()).toLocalDateTime()
            );
        } catch (ArtistContentException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("유튜브 메타데이터 조회 실패: videoId={}, error={}", youtubeVideoId, exception.getMessage());
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_YOUTUBE_METADATA_FETCH_FAILED);
        }
    }

    private String resolveThumbnailUrl(String youtubeVideoId, YoutubeThumbnailGroup thumbnails) {
        if (thumbnails == null) {
            return buildFallbackThumbnailUrl(youtubeVideoId);
        }

        // 썸네일은 가장 큰 해상도부터 내려가며 선택한다.
        // 이렇게 하면 프론트가 따로 우선순위 분기를 하지 않아도 된다.
        if (hasUrl(thumbnails.maxres())) {
            return thumbnails.maxres().url();
        }
        if (hasUrl(thumbnails.standard())) {
            return thumbnails.standard().url();
        }
        if (hasUrl(thumbnails.high())) {
            return thumbnails.high().url();
        }
        if (hasUrl(thumbnails.medium())) {
            return thumbnails.medium().url();
        }
        if (hasUrl(thumbnails.defaultThumbnail())) {
            return thumbnails.defaultThumbnail().url();
        }
        return buildFallbackThumbnailUrl(youtubeVideoId);
    }

    private boolean hasUrl(YoutubeThumbnail thumbnail) {
        return thumbnail != null && StringUtils.hasText(thumbnail.url());
    }

    private String buildFallbackThumbnailUrl(String youtubeVideoId) {
        // API 응답에 썸네일이 비정상적으로 비면 유튜브 기본 hqdefault 경로로 fallback 한다.
        return "https://i.ytimg.com/vi/" + youtubeVideoId + "/hqdefault.jpg";
    }

    private record YoutubeVideosResponse(List<YoutubeVideoItem> items) {
    }

    private record YoutubeVideoItem(
            YoutubeSnippet snippet,
            YoutubeContentDetails contentDetails
    ) {
    }

    private record YoutubeSnippet(
            String title,
            String publishedAt,
            YoutubeThumbnailGroup thumbnails
    ) {
    }

    private record YoutubeContentDetails(String duration) {
    }

    private record YoutubeThumbnailGroup(
            @JsonProperty("default")
            YoutubeThumbnail defaultThumbnail,
            YoutubeThumbnail medium,
            YoutubeThumbnail high,
            YoutubeThumbnail standard,
            YoutubeThumbnail maxres
    ) {
    }

    private record YoutubeThumbnail(String url) {
    }
}
