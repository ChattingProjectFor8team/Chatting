package com.example.infinite.global.common.config;

import com.example.infinite.global.common.constant.CacheNames;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
/**
 * Spring Cache + Redis 설정.
 *
 * <p>현재 프로젝트는 Redis를 인기 검색어 집계, 분산 락, 래플 상태 저장에도 사용하고 있다.
 * 여기에 Spring Cache 추상화를 추가로 얹어 "조회 결과 캐시"를 같은 Redis 안에서 분리 운영한다.</p>
 *
 * <p>이번 설정의 목적은 아티스트 상세 조회 v2를 Cache-aside 전략으로 캐싱하는 것이다.
 * 즉, 캐시 miss 때만 DB/Querydsl 원본 조회를 수행하고, hit면 Redis 값을 바로 반환한다.</p>
 *
 * <p>핵심 설정 포인트:
 * <ul>
 *   <li>TTL을 명시해 캐시 데이터가 영구히 남지 않게 한다.</li>
 *   <li>key prefix를 분리해 기존 Redis 직접 사용 키(ZSet/Lock/Lua)와 충돌을 방지한다.</li>
 *   <li>값 직렬화는 JSON으로 맞춰 redis-cli에서 확인 가능한 형태를 유지한다.</li>
 * </ul>
 * </p>
 */
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 값 직렬화는 JSON으로 맞춰 캐시 내용을 사람이 읽고 디버깅할 수 있게 한다.
        GenericJacksonJsonRedisSerializer jsonSerializer = GenericJacksonJsonRedisSerializer.create(builder -> {
        });

        // 모든 Redis 캐시에 적용되는 기본 설정이다.
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                // Cache-aside 전략에서는 TTL이 지나면 원본 조회 후 다시 적재한다.
                .entryTtl(Duration.ofMinutes(10))
                // cache:{cacheName}:: prefix를 강제해 직접 Redis key와 네임스페이스를 분리한다.
                .computePrefixWith(cacheName -> "cache:" + cacheName + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        // 캐시별 성격에 따라 TTL을 다르게 둘 수 있도록 분리한다.
        Map<String, RedisCacheConfiguration> perCacheConfig = Map.of(
                // 아티스트 상세는 수정 빈도가 검색 결과보다 적지 않으므로 너무 길지 않은 5분 TTL을 둔다.
                CacheNames.ARTIST_DETAIL_V2, defaultConfig.entryTtl(Duration.ofMinutes(5))
        );

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfig)
                .build();
    }
}
