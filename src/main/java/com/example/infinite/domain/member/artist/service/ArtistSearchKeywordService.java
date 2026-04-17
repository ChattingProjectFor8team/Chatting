package com.example.infinite.domain.member.artist.service;

import com.example.infinite.domain.member.artist.dto.response.ArtistPopularSearchResponse;
import com.example.infinite.domain.member.artist.repository.ArtistSearchKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistSearchKeywordService {

    private static final int DEFAULT_POPULAR_KEYWORD_LIMIT = 10;
    private static final int MAX_POPULAR_KEYWORD_LIMIT = 50;

    private final ArtistSearchKeywordRepository artistSearchKeywordRepository;

    @Transactional
    public void recordSearchKeyword(String userKey, String keyword) {
        // 인기 검색어는 검색 결과 캐시와 별개로 사용자 기준 중복을 제거해 집계한다.
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedUserKey = normalizeUserKey(userKey);
        if (normalizedKeyword == null || normalizedUserKey == null) {
            return;
        }

        artistSearchKeywordRepository.incrementScoreIfFirstSearch(normalizedUserKey, normalizedKeyword);
    }

    public List<ArtistPopularSearchResponse> getPopularKeywords(Integer limit) {
        // 비정상적으로 큰 limit 요청이 Redis 대량 조회로 이어지지 않도록 상한을 둔다.
        int resolvedLimit = (limit == null || limit <= 0)
                ? DEFAULT_POPULAR_KEYWORD_LIMIT
                : Math.min(limit, MAX_POPULAR_KEYWORD_LIMIT);

        return artistSearchKeywordRepository.findTopKeywords(resolvedLimit).stream()
                .map(this::toResponse)
                .toList();
    }

    private ArtistPopularSearchResponse toResponse(ZSetOperations.TypedTuple<String> tuple) {
        String keyword = tuple.getValue();
        Double score = tuple.getScore();
        return new ArtistPopularSearchResponse(
                keyword == null ? "" : keyword,
                score == null ? 0L : score.longValue()
        );
    }

    private String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }

        // 공백/대소문자 차이로 동일 검색어가 여러 키로 쪼개지지 않도록 정규화한다.
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUserKey(String userKey) {
        if (!StringUtils.hasText(userKey)) {
            return null;
        }

        // 토큰 principal(email)을 dedupe 식별자로 사용해 동일 사용자의 반복 검색을 막는다.
        return userKey.trim().toLowerCase(Locale.ROOT);
    }
}
