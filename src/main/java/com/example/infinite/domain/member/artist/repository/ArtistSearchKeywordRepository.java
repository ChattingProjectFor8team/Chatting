package com.example.infinite.domain.member.artist.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

//캐시 레포지토리
@Repository
@RequiredArgsConstructor
public class ArtistSearchKeywordRepository {

    public static final String POPULAR_ARTIST_SEARCH_KEY = "search:artist:popular";
    private static final String SEARCH_DEDUPE_KEY_FORMAT = "search:artist:dedupe:%s:%s";
    private static final Duration SEARCH_DEDUPE_TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;

    // 동일 사용자가 같은 검색어를 반복 입력해도 TTL 동안 한 번만 점수를 올린다.
    public void incrementScoreIfFirstSearch(String userKey, String keyword) {
        String dedupeKey = SEARCH_DEDUPE_KEY_FORMAT.formatted(userKey, keyword);
        Boolean isFirstSearch = stringRedisTemplate.opsForValue()
                .setIfAbsent(dedupeKey, "1", SEARCH_DEDUPE_TTL);

        if (Boolean.TRUE.equals(isFirstSearch)) {
            stringRedisTemplate.opsForZSet().incrementScore(POPULAR_ARTIST_SEARCH_KEY, keyword, 1);
        }
    }


    public Set<ZSetOperations.TypedTuple<String>> findTopKeywords(long start, long end) {
        if (end < start) {
            //검색결과가 없음 비워주기
            return Collections.emptySet();
        }

        Set<ZSetOperations.TypedTuple<String>> keywords = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(POPULAR_ARTIST_SEARCH_KEY, start, end);

        return keywords == null ? Collections.emptySet() : keywords;
    }
}
