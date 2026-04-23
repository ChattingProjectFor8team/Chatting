package com.example.infinite.domain.artistcontent.media.service;

import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class YoutubeUrlParser {

    private static final String YOUTUBE_WATCH_PATH = "/watch";
    private static final String YOUTUBE_MAIN_HOST = "youtube.com";
    private static final String YOUTUBE_SHORT_HOST = "youtu.be";

    private YoutubeUrlParser() {
    }

    public static String extractVideoId(String rawUrl) {
        if (!StringUtils.hasText(rawUrl)) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_YOUTUBE_URL_INVALID);
        }

        URI uri = parseUri(rawUrl.trim());
        String host = Optional.ofNullable(uri.getHost())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse("");

        if (isYoutubeShortHost(host)) {
            return extractPathSegment(uri.getPath());
        }

        if (!isYoutubeMainHost(host)) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_YOUTUBE_URL_INVALID);
        }

        if (YOUTUBE_WATCH_PATH.equals(uri.getPath())) {
            return extractWatchVideoId(uri.getQuery());
        }

        String[] segments = Arrays.stream(Optional.ofNullable(uri.getPath()).orElse("")
                        .split("/"))
                .filter(StringUtils::hasText)
                .toArray(String[]::new);

        if (segments.length >= 2 && ("shorts".equals(segments[0]) || "embed".equals(segments[0]) || "live".equals(segments[0]))) {
            return validateVideoId(segments[1]);
        }

        throw new ArtistContentException(ArtistContentErrorCode.MEDIA_YOUTUBE_URL_INVALID);
    }

    public static String canonicalUrl(String youtubeVideoId) {
        return "https://www.youtube.com/watch?v=" + youtubeVideoId;
    }

    private static boolean isYoutubeMainHost(String host) {
        // endsWith("youtube.com")만 쓰면 notyoutube.com 같은 가짜 호스트도 통과한다.
        // 그래서 정확히 youtube.com 이거나, 그 하위 서브도메인만 허용한다.
        return YOUTUBE_MAIN_HOST.equals(host) || host.endsWith("." + YOUTUBE_MAIN_HOST);
    }

    private static boolean isYoutubeShortHost(String host) {
        return YOUTUBE_SHORT_HOST.equals(host) || host.endsWith("." + YOUTUBE_SHORT_HOST);
    }

    private static URI parseUri(String rawUrl) {
        try {
            return new URI(rawUrl);
        } catch (URISyntaxException firstException) {
            try {
                // 사용자가 스킴 없이 붙여 넣는 경우를 허용해 프론트 입력 부담을 줄인다.
                return new URI("https://" + rawUrl);
            } catch (URISyntaxException ignored) {
                throw new ArtistContentException(ArtistContentErrorCode.MEDIA_YOUTUBE_URL_INVALID);
            }
        }
    }

    private static String extractWatchVideoId(String query) {
        if (!StringUtils.hasText(query)) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_YOUTUBE_URL_INVALID);
        }

        Map<String, String> queryMap = Arrays.stream(query.split("&"))
                .map(token -> token.split("=", 2))
                .filter(parts -> parts.length == 2 && StringUtils.hasText(parts[0]) && StringUtils.hasText(parts[1]))
                .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1], (left, right) -> left));

        return validateVideoId(queryMap.get("v"));
    }

    private static String extractPathSegment(String path) {
        String[] segments = Arrays.stream(Optional.ofNullable(path).orElse("")
                        .split("/"))
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
        if (segments.length == 0) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_YOUTUBE_URL_INVALID);
        }
        return validateVideoId(segments[0]);
    }

    private static String validateVideoId(String videoId) {
        if (!StringUtils.hasText(videoId)) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_YOUTUBE_URL_INVALID);
        }

        String normalized = videoId.trim();
        if (normalized.length() < 6 || normalized.length() > 20) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_YOUTUBE_URL_INVALID);
        }
        return normalized;
    }
}
