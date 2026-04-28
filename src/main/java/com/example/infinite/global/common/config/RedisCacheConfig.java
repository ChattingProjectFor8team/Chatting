package com.example.infinite.global.common.config;

import com.example.infinite.global.common.constant.CacheNames;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
/**
 * Spring Cache + Redis 설정.
 *
 * <p>현재 프로젝트는 Redis를 인기 검색어 집계, 분산 락, Redis Stream 보조 상태, 조회 캐시에 함께 사용한다.
 * 같은 Redis를 쓰더라도 용도를 분리해서 봐야 하므로, Spring Cache는 "읽기 최적화용 namespace"로 따로 운영한다.</p>
 *
 * <p>학습 포인트:
 * <ul>
 *   <li>base cache: 본문/미디어/해시태그처럼 상대적으로 덜 변하는 조립 결과</li>
 *   <li>hot cache: like/comment count처럼 매우 자주 바뀌는 숫자</li>
 *   <li>comment cache: 구조 전체가 같이 움직이므로 짧은 TTL 통캐시</li>
 * </ul>
 * </p>
 *
 * <p>핵심 설정 포인트:
 * <ul>
 *   <li>TTL을 명시해 캐시 데이터가 영구히 남지 않게 한다.</li>
 *   <li>key prefix를 분리해 기존 Redis 직접 사용 키(ZSet/Lock/Lua/Stream 보조 키)와 충돌을 방지한다.</li>
 *   <li>값 직렬화는 JSON으로 맞춰 redis-cli에서 확인 가능한 형태를 유지한다.</li>
 * </ul>
 * </p>
 */
public class RedisCacheConfig {

    @Bean
    @Primary
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 값 직렬화는 JSON으로 맞춰 캐시 내용을 사람이 읽고 디버깅할 수 있게 한다.
        //
        // 이번 수정 배경:
        // - mixed read/write 테스트에서 Redis cache에 들어간 DTO/record가
        //   읽을 때 LinkedHashMap으로 돌아와 ClassCastException이 발생했다
        // - 원인은 현재 GenericJacksonJsonRedisSerializer 설정에서
        //   프로젝트가 기대하는 타입 메타데이터 복원이 충분히 맞지 않았기 때문이다
        //
        // 그래서 Spring Data Redis 4의 기본 대체재인
        // GenericJacksonJsonRedisSerializer로 옮기되,
        // 기존과 같은 "타입 정보 포함 JSON" 정책을 유지한다.
        //
        // 핵심은 두 가지다.
        // - Jackson 3에서 제거된 EVERYTHING 대신 NON_FINAL_AND_RECORDS 로
        //   record 응답과 컬렉션/비final 컨테이너의 타입 복원을 유지한다
        // - DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS 비활성화로 날짜 필드를 사람이 읽기 쉬운 JSON으로 유지
        // Redis value JSON 안에 실제 Java 타입 힌트를 어떤 필드명으로 넣을지 정한다.
        // 예: {"@class":"com.example...PageResponse","content":[...]}
        //
        // Spring Cache는 메서드 반환 타입이 Object/제네릭으로 다뤄지는 구간이 많아서
        // 이 메타데이터가 없으면 역직렬화 시 DTO 대신 LinkedHashMap으로 돌아오기 쉽다.
        String typeHintProperty = "@class";
        GenericJacksonJsonRedisSerializer jsonSerializer = GenericJacksonJsonRedisSerializer.create(builder -> {
            // Spring Cache는 내부적으로 null 캐시를 위해 NullValue sentinel을 쓸 수 있다.
            // 이 설정을 켜 두면 serializer가 그 sentinel도 같은 타입 힌트 규칙으로 처리한다.
            builder.enableSpringCacheNullValueSupport(typeHintProperty);
            builder.customize(mapperBuilder -> mapperBuilder
                    // Jackson 3에서는 날짜 timestamp on/off 설정이
                    // 기존 SerializationFeature가 아니라 DateTimeFeature로 이동했다.
                    .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .activateDefaultTypingAsProperty(
                            // 현재 Redis는 "우리 애플리케이션이 직접 쓴 내부 캐시"라는 전제라
                            // 타입 검증을 넓게 허용한다.
                            // 만약 외부/불신 입력을 읽는 구조라면 package prefix 기준으로 더 좁혀야 안전하다.
                            BasicPolymorphicTypeValidator.builder()
                                    .allowIfSubType(Object.class)
                                    .build(),
                            // Jackson 2의 EVERYTHING은 Jackson 3에서 제거되었다.
                            // 여기서는 record DTO가 많이 쓰이므로 NON_FINAL만으로는 부족하다.
                            // record는 final이라 타입 힌트가 빠지면 다시 읽을 때 Map으로 풀릴 수 있어
                            // NON_FINAL_AND_RECORDS로 맞춰 "비final + record"를 함께 복원한다.
                            DefaultTyping.NON_FINAL_AND_RECORDS,
                            typeHintProperty
                    ));
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
        Map<String, RedisCacheConfiguration> perCacheConfig = Map.ofEntries(
                // 검색 v3는 remote cache 비교용 버전이다. page당 10건 고정 응답을 Redis에 저장한다.
                Map.entry(CacheNames.ARTIST_SEARCH_V3, defaultConfig.entryTtl(Duration.ofMinutes(10))),
                // 아티스트 상세는 수정 빈도가 검색 결과보다 적지 않으므로 너무 길지 않은 5분 TTL을 둔다.
                Map.entry(CacheNames.ARTIST_DETAIL_V2, defaultConfig.entryTtl(Duration.ofMinutes(5))),
                // post base 캐시는 본문/작성자/미디어/해시태그처럼 상대적으로 덜 변하는 읽기 조립 결과다.
                Map.entry(CacheNames.ARTIST_POST_LIST_BASE, defaultConfig.entryTtl(Duration.ofMinutes(10))),
                Map.entry(CacheNames.ARTIST_POST_DETAIL_BASE, defaultConfig.entryTtl(Duration.ofMinutes(10))),
                Map.entry(CacheNames.FAN_POST_LIST_BASE, defaultConfig.entryTtl(Duration.ofMinutes(10))),
                Map.entry(CacheNames.FAN_POST_DETAIL_BASE, defaultConfig.entryTtl(Duration.ofMinutes(10))),
                Map.entry(CacheNames.FAN_LETTER_LIST_BASE, defaultConfig.entryTtl(Duration.ofMinutes(10))),
                Map.entry(CacheNames.FAN_LETTER_DETAIL_BASE, defaultConfig.entryTtl(Duration.ofMinutes(10))),
                // hot data는 like/comment count처럼 flush 주기와 함께 움직이는 짧은 수명 캐시다.
                // ArtistPost는 현재 3초 flush 배치에 맞춰 eventual consistency를 설명하는 구조다.
                Map.entry(CacheNames.POST_HOT_DATA, defaultConfig.entryTtl(Duration.ofSeconds(3))),
                // 댓글 목록은 구조 전체가 같이 움직이므로 base/hot 분리 대신 짧은 TTL 통캐시를 사용한다.
                Map.entry(CacheNames.COMMENT_ROOT_SLICE, defaultConfig.entryTtl(Duration.ofSeconds(3))),
                Map.entry(CacheNames.COMMENT_REPLY_LIST, defaultConfig.entryTtl(Duration.ofSeconds(3)))
        );

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfig)
                .build();
    }
}
