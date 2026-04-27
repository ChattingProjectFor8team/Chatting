package com.example.infinite.domain.member.artist.service;

import com.example.infinite.domain.member.artist.dto.response.ArtistSearchResponse;
import com.example.infinite.global.common.config.CacheConfig;
import com.example.infinite.global.common.constant.CacheNames;
import com.example.infinite.global.common.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("아티스트 검색 캐싱 성능 비교 벤치마크")
class ArtistSearchCachingBenchmarkIntegrationTest {

    private static final int ARTIST_COUNT = 50_000;
    private static final int BATCH_SIZE = 1_000;
    private static final int WARM_UP_COUNT = 5;
    private static final int MEASURE_COUNT = 30;

    @Autowired
    private ArtistService artistService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    @Qualifier("caffeineCacheManager")
    private CacheManager caffeineCacheManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        clearLocalSearchCache();
        clearRedisSearchCache();
        truncateArtists();
    }

    @Test
    @DisplayName("검색 v1, v2, v3의 평균 응답시간을 비교한다")
    void benchmarkSearchCachingPerformance() {
        insertBenchmarkArtists();

        for (int i = 0; i < WARM_UP_COUNT; i++) {
            artistService.searchArtistsV1("bts", 1);
            clearLocalSearchCache();
            artistService.searchArtistsV2("bts", 1);
            clearRedisSearchCache();
            artistService.searchArtistsV3("bts", 1);
        }

        clearLocalSearchCache();
        long v2ColdMs = measureOnceMillis(() -> assertSearchResult(artistService.searchArtistsV2("bts", 1)));

        clearLocalSearchCache();
        assertSearchResult(artistService.searchArtistsV2("bts", 1));
        double v2WarmAvgMs = measureAverageMillis(() -> assertSearchResult(artistService.searchArtistsV2("bts", 1)));

        clearRedisSearchCache();
        long v3ColdMs = measureOnceMillis(() -> assertSearchResult(artistService.searchArtistsV3("bts", 1)));

        clearRedisSearchCache();
        assertSearchResult(artistService.searchArtistsV3("bts", 1));
        double v3WarmAvgMs = measureAverageMillis(() -> assertSearchResult(artistService.searchArtistsV3("bts", 1)));

        double v1AvgMs = measureAverageMillis(() -> assertSearchResult(artistService.searchArtistsV1("bts", 1)));

        double warmImprovementPercent = improvementPercent(v1AvgMs, v2WarmAvgMs);
        double remoteWarmImprovementPercent = improvementPercent(v1AvgMs, v3WarmAvgMs);
        System.out.printf(
                "ARTIST_SEARCH_CACHE_BENCHMARK artistCount=%d measureCount=%d v1AvgMs=%.3f v2ColdMs=%d v2WarmAvgMs=%.3f v3ColdMs=%d v3WarmAvgMs=%.3f v2WarmImprovementPercent=%.1f%% v3WarmImprovementPercent=%.1f%%%n",
                ARTIST_COUNT,
                MEASURE_COUNT,
                v1AvgMs,
                v2ColdMs,
                v2WarmAvgMs,
                v3ColdMs,
                v3WarmAvgMs,
                warmImprovementPercent,
                remoteWarmImprovementPercent
        );

        assertThat(v1AvgMs).isGreaterThan(0.0);
        assertThat(v2ColdMs).isGreaterThan(0L);
        assertThat(v2WarmAvgMs).isGreaterThan(0.0);
        assertThat(v3ColdMs).isGreaterThan(0L);
        assertThat(v3WarmAvgMs).isGreaterThan(0.0);
        assertThat(v2WarmAvgMs).isLessThan(v1AvgMs);
        assertThat(v3WarmAvgMs).isLessThan(v1AvgMs);
    }

    private void assertSearchResult(PageResponse<ArtistSearchResponse> response) {
        assertThat(response.content()).hasSize(10);
        assertThat(response.totalElements()).isEqualTo(ARTIST_COUNT);
    }

    private double measureAverageMillis(Runnable runnable) {
        long totalNanos = 0L;
        for (int i = 0; i < MEASURE_COUNT; i++) {
            long start = System.nanoTime();
            runnable.run();
            totalNanos += System.nanoTime() - start;
        }
        return totalNanos / 1_000_000.0 / MEASURE_COUNT;
    }

    private long measureOnceMillis(Runnable runnable) {
        long start = System.nanoTime();
        runnable.run();
        return Math.round((System.nanoTime() - start) / 1_000_000.0);
    }

    private double improvementPercent(double beforeMs, double afterMs) {
        if (beforeMs <= 0.0) {
            return 0.0;
        }
        return ((beforeMs - afterMs) / beforeMs) * 100.0;
    }

    private void clearLocalSearchCache() {
        if (caffeineCacheManager.getCache(CacheConfig.ARTIST_SEARCH_V2_CACHE) != null) {
            caffeineCacheManager.getCache(CacheConfig.ARTIST_SEARCH_V2_CACHE).clear();
        }
    }

    private void clearRedisSearchCache() {
        if (stringRedisTemplate.keys("cache:" + CacheNames.ARTIST_SEARCH_V3 + "::" + "*") != null) {
            stringRedisTemplate.delete(stringRedisTemplate.keys("cache:" + CacheNames.ARTIST_SEARCH_V3 + "::" + "*"));
        }
    }

    private void truncateArtists() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE artists");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    private void insertBenchmarkArtists() {
        String sql = """
                INSERT INTO artists (
                    name,
                    slug,
                    profile_image_url,
                    cover_image_url,
                    intro,
                    status,
                    created_at,
                    updated_at,
                    deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        LocalDateTime now = LocalDateTime.now();
        Timestamp nowTimestamp = Timestamp.valueOf(now);

        for (int start = 1; start <= ARTIST_COUNT; start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE - 1, ARTIST_COUNT);
            List<Object[]> batchArgs = new ArrayList<>(end - start + 1);
            for (int i = start; i <= end; i++) {
                batchArgs.add(new Object[]{
                        "BTS benchmark artist " + i,
                        "bts-benchmark-" + i,
                        null,
                        null,
                        "artist benchmark " + i,
                        "ACTIVE",
                        nowTimestamp,
                        nowTimestamp,
                        null
                });
            }
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }
    }
}
