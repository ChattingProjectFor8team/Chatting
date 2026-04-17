package com.example.infinite.domain.member.artist.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class ArtistSearchKeywordRepository {

    public static final String POPULAR_ARTIST_SEARCH_KEY = "search:artist:popular";

    private final StringRedisTemplate stringRedisTemplate;

    // 키워드 캐시점수올리기
    public void incrementScore(String keyword) {
        stringRedisTemplate.opsForZSet().incrementScore(POPULAR_ARTIST_SEARCH_KEY, keyword, 1);
    }


    public Set<ZSetOperations.TypedTuple<String>> findTopKeywords(int limit) {
        if (limit <= 0) {
            //검색결과가 없음 비워주기
            return Collections.emptySet();
        }

        Set<ZSetOperations.TypedTuple<String>> keywords = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(POPULAR_ARTIST_SEARCH_KEY, 0, limit - 1L);

        return keywords == null ? Collections.emptySet() : keywords;
    }
}
