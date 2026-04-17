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

    private final ArtistSearchKeywordRepository artistSearchKeywordRepository;

    @Transactional
    public void recordSearchKeyword(String keyword) {
        // 인기 검색어는 검색 결과 캐시와 별개로 Redis ZSet 점수를 누적 집계한다.
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword == null) {
            return;
        }

        artistSearchKeywordRepository.incrementScore(normalizedKeyword);
    }

    public List<ArtistPopularSearchResponse> getPopularKeywords(Integer limit) {
        int resolvedLimit = (limit == null || limit <= 0) ? DEFAULT_POPULAR_KEYWORD_LIMIT : limit;

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
}
