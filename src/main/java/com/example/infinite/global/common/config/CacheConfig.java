package com.example.infinite.global.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    // 필수 과제의 검색 v2 결과 캐시 이름이다. 인기 검색어 집계 Redis 키와는 역할이 다르다.
    public static final String ARTIST_SEARCH_V2_CACHE = "artistSearchV2";

    @Bean
    public CacheManager cacheManager() {
        // 과제 요구사항상 v2 검색만 우선 캐시 적용한다.
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(ARTIST_SEARCH_V2_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 검색 결과는 원본 변경 빈도가 아주 높지 않다고 보고 짧은 TTL만 둔다.
                .expireAfterWrite(Duration.ofMinutes(10))
                // 로컬 메모리 캐시가 과하게 커지지 않도록 최대 개수를 제한한다.
                .maximumSize(1_000));
        return cacheManager;
    }
}
