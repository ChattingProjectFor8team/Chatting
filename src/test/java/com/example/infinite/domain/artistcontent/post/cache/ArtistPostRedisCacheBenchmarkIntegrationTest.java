package com.example.infinite.domain.artistcontent.post.cache;

import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostBaseResponse;
import com.example.infinite.domain.artistcontent.post.artistpost.service.ArtistPostBaseCacheService;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.artist.entity.ArtistMember;
import com.example.infinite.domain.member.artist.repository.ArtistMemberRepository;
import com.example.infinite.domain.member.artist.repository.ArtistRepository;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.enums.MemberRole;
import com.example.infinite.domain.member.member.repository.MemberRepository;
import com.example.infinite.global.common.constant.CacheNames;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ArtistPost Redis 캐시 성능 비교 벤치마크")
class ArtistPostRedisCacheBenchmarkIntegrationTest {

    private static final int POST_COUNT = 200;
    private static final int LIST_SIZE = 10;
    private static final int WARM_UP_COUNT = 5;
    private static final int MEASURE_COUNT = 30;

    @Autowired
    private ArtistPostBaseCacheService artistPostBaseCacheService;

    @Autowired
    private PostHotDataCacheService postHotDataCacheService;

    @Autowired
    private PostHotDataLoaderService postHotDataLoaderService;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private ArtistMemberRepository artistMemberRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        clearRedisCache(CacheNames.ARTIST_POST_LIST_BASE, CacheNames.ARTIST_POST_DETAIL_BASE, CacheNames.POST_HOT_DATA);
        truncateTestTables();
    }

    @Test
    @DisplayName("ArtistPost base list/detail cache와 hot cache의 cold/warm 성능을 비교한다")
    void benchmarkArtistPostRedisCaches() {
        BenchmarkFixture fixture = createBenchmarkFixture();

        for (int i = 0; i < WARM_UP_COUNT; i++) {
            artistPostBaseCacheService.loadArtistPostBaseSlice(fixture.artistId(), null);
            clearRedisCache(CacheNames.ARTIST_POST_LIST_BASE);
            artistPostBaseCacheService.getArtistPostBaseSlice(fixture.artistId(), null);

            artistPostBaseCacheService.loadArtistPostBaseDetail(fixture.artistId(), fixture.detailPostId());
            clearRedisCache(CacheNames.ARTIST_POST_DETAIL_BASE);
            artistPostBaseCacheService.getArtistPostBaseDetail(fixture.artistId(), fixture.detailPostId());

            postHotDataLoaderService.load(PostType.ARTIST_POST, fixture.hotPostIds());
            clearRedisCache(CacheNames.POST_HOT_DATA);
            postHotDataCacheService.getPostHotDataMap(PostType.ARTIST_POST, fixture.hotPostIds());
        }

        double listUncachedAvgMs = measureAverageMillis(() -> assertBaseSlice(
                artistPostBaseCacheService.loadArtistPostBaseSlice(fixture.artistId(), null)
        ));
        clearRedisCache(CacheNames.ARTIST_POST_LIST_BASE);
        long listColdMs = measureOnceMillis(() -> assertBaseSlice(
                artistPostBaseCacheService.getArtistPostBaseSlice(fixture.artistId(), null)
        ));
        clearRedisCache(CacheNames.ARTIST_POST_LIST_BASE);
        assertBaseSlice(artistPostBaseCacheService.getArtistPostBaseSlice(fixture.artistId(), null));
        double listWarmAvgMs = measureAverageMillis(() -> assertBaseSlice(
                artistPostBaseCacheService.getArtistPostBaseSlice(fixture.artistId(), null)
        ));

        double detailUncachedAvgMs = measureAverageMillis(() -> assertBaseDetail(
                artistPostBaseCacheService.loadArtistPostBaseDetail(fixture.artistId(), fixture.detailPostId())
        ));
        clearRedisCache(CacheNames.ARTIST_POST_DETAIL_BASE);
        long detailColdMs = measureOnceMillis(() -> assertBaseDetail(
                artistPostBaseCacheService.getArtistPostBaseDetail(fixture.artistId(), fixture.detailPostId())
        ));
        clearRedisCache(CacheNames.ARTIST_POST_DETAIL_BASE);
        assertBaseDetail(artistPostBaseCacheService.getArtistPostBaseDetail(fixture.artistId(), fixture.detailPostId()));
        double detailWarmAvgMs = measureAverageMillis(() -> assertBaseDetail(
                artistPostBaseCacheService.getArtistPostBaseDetail(fixture.artistId(), fixture.detailPostId())
        ));

        double hotUncachedAvgMs = measureAverageMillis(() -> assertHotData(
                postHotDataLoaderService.load(PostType.ARTIST_POST, fixture.hotPostIds())
        ));
        clearRedisCache(CacheNames.POST_HOT_DATA);
        long hotColdMs = measureOnceMillis(() -> assertHotData(
                postHotDataCacheService.getPostHotDataMap(PostType.ARTIST_POST, fixture.hotPostIds())
        ));
        clearRedisCache(CacheNames.POST_HOT_DATA);
        assertHotData(postHotDataCacheService.getPostHotDataMap(PostType.ARTIST_POST, fixture.hotPostIds()));
        double hotWarmAvgMs = measureAverageMillis(() -> assertHotData(
                postHotDataCacheService.getPostHotDataMap(PostType.ARTIST_POST, fixture.hotPostIds())
        ));

        System.out.printf(
                "ARTIST_POST_BASE_LIST_CACHE_BENCHMARK postCount=%d measureCount=%d uncachedAvgMs=%.3f coldMs=%d warmAvgMs=%.3f improvementPercent=%.1f%%%n",
                POST_COUNT,
                MEASURE_COUNT,
                listUncachedAvgMs,
                listColdMs,
                listWarmAvgMs,
                improvementPercent(listUncachedAvgMs, listWarmAvgMs)
        );
        System.out.printf(
                "ARTIST_POST_BASE_DETAIL_CACHE_BENCHMARK postCount=%d measureCount=%d uncachedAvgMs=%.3f coldMs=%d warmAvgMs=%.3f improvementPercent=%.1f%%%n",
                POST_COUNT,
                MEASURE_COUNT,
                detailUncachedAvgMs,
                detailColdMs,
                detailWarmAvgMs,
                improvementPercent(detailUncachedAvgMs, detailWarmAvgMs)
        );
        System.out.printf(
                "ARTIST_POST_HOT_DATA_CACHE_BENCHMARK hotPostCount=%d measureCount=%d uncachedAvgMs=%.3f coldMs=%d warmAvgMs=%.3f improvementPercent=%.1f%%%n",
                fixture.hotPostIds().size(),
                MEASURE_COUNT,
                hotUncachedAvgMs,
                hotColdMs,
                hotWarmAvgMs,
                improvementPercent(hotUncachedAvgMs, hotWarmAvgMs)
        );

        assertThat(listWarmAvgMs).isLessThan(listUncachedAvgMs);
        assertThat(detailWarmAvgMs).isLessThan(detailUncachedAvgMs);
        assertThat(hotWarmAvgMs).isGreaterThan(0.0);
    }

    private BenchmarkFixture createBenchmarkFixture() {
        Member writer = memberRepository.save(Member.createNewMember(
                "artist-post-benchmark@example.com",
                "password",
                "artist-post-benchmark",
                "010-7000-0001"
        ));
        writer.changeRole(MemberRole.ARTIST);
        memberRepository.saveAndFlush(writer);

        Artist artist = artistRepository.saveAndFlush(Artist.create(
                "Benchmark Artist",
                "benchmark-artist",
                null,
                null,
                "artist post cache benchmark"
        ));

        artistMemberRepository.saveAndFlush(ArtistMember.create(
                artist,
                writer,
                "BENCHMARK",
                null,
                1
        ));

        insertArtistPosts(artist.getId(), writer.getId());

        List<Long> postIds = jdbcTemplate.queryForList(
                "SELECT id FROM artist_posts WHERE artist_id = ? ORDER BY id DESC",
                Long.class,
                artist.getId()
        );
        return new BenchmarkFixture(
                artist.getId(),
                postIds.get(0),
                postIds.subList(0, LIST_SIZE)
        );
    }

    private void insertArtistPosts(Long artistId, Long writerId) {
        String sql = """
                INSERT INTO artist_posts (
                    artist_id,
                    member_id,
                    visibility,
                    content,
                    like_count,
                    comment_count,
                    media_count,
                    created_at,
                    updated_at,
                    deleted_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        LocalDateTime now = LocalDateTime.now();
        Timestamp nowTimestamp = Timestamp.valueOf(now);

        for (int i = 1; i <= POST_COUNT; i++) {
            jdbcTemplate.update(
                    sql,
                    artistId,
                    writerId,
                    "PUBLIC",
                    "artist post benchmark content " + i,
                    50L + i,
                    20L + i,
                    0,
                    nowTimestamp,
                    nowTimestamp,
                    null
            );
        }
    }

    private void assertBaseSlice(CursorSliceResponse<ArtistPostBaseResponse> response) {
        assertThat(response.content()).hasSize(LIST_SIZE);
        assertThat(response.size()).isEqualTo(LIST_SIZE);
        assertThat(response.hasNext()).isTrue();
    }

    private void assertBaseDetail(ArtistPostBaseResponse response) {
        assertThat(response.artistId()).isNotNull();
        assertThat(response.content()).startsWith("artist post benchmark content");
    }

    private void assertHotData(java.util.Map<Long, PostHotData> hotDataMap) {
        assertThat(hotDataMap).hasSize(LIST_SIZE);
        assertThat(hotDataMap.values())
                .allSatisfy(hotData -> {
                    assertThat(hotData.likeCount()).isGreaterThan(0L);
                    assertThat(hotData.commentCount()).isGreaterThan(0L);
                });
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

    private void clearRedisCache(String... cacheNames) {
        for (String cacheName : cacheNames) {
            if (cacheManager.getCache(cacheName) != null) {
                cacheManager.getCache(cacheName).clear();
            }
        }
    }

    private void truncateTestTables() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE content_hashtags");
        jdbcTemplate.execute("TRUNCATE TABLE hashtags");
        jdbcTemplate.execute("TRUNCATE TABLE media_files");
        jdbcTemplate.execute("TRUNCATE TABLE artist_posts");
        jdbcTemplate.execute("TRUNCATE TABLE artist_members");
        jdbcTemplate.execute("TRUNCATE TABLE artists");
        jdbcTemplate.execute("TRUNCATE TABLE members");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    private record BenchmarkFixture(
            Long artistId,
            Long detailPostId,
            List<Long> hotPostIds
    ) {
    }
}
