package com.example.infinite.domain.member.artist.service;

import com.example.infinite.domain.member.artist.dto.response.ArtistResponse;
import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.artist.entity.ArtistMember;
import com.example.infinite.domain.member.artist.repository.ArtistMemberRepository;
import com.example.infinite.domain.member.artist.repository.ArtistRepository;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.repository.MemberRepository;
import com.example.infinite.global.common.constant.CacheNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("아티스트 상세 Redis 캐시 성능 비교 벤치마크")
class ArtistDetailRedisCachingBenchmarkIntegrationTest {

    private static final int MEMBER_COUNT = 8;
    private static final int WARM_UP_COUNT = 5;
    private static final int MEASURE_COUNT = 30;

    @Autowired
    private ArtistService artistService;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private ArtistMemberRepository artistMemberRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        truncateTestTables();
    }

    @Test
    @DisplayName("아티스트 상세 v1과 v2 warm cache 평균 응답시간을 비교한다")
    void benchmarkArtistDetailRedisCachingPerformance() {
        Long artistId = createBenchmarkArtistWithMembers();

        for (int i = 0; i < WARM_UP_COUNT; i++) {
            artistService.getArtist(artistId);
            clearArtistDetailCache(artistId);
            artistService.getArtistV2(artistId);
        }

        clearArtistDetailCache(artistId);
        long v2ColdMs = measureOnceMillis(() -> assertArtistDetail(artistService.getArtistV2(artistId)));

        clearArtistDetailCache(artistId);
        assertArtistDetail(artistService.getArtistV2(artistId));
        double v2WarmAvgMs = measureAverageMillis(() -> assertArtistDetail(artistService.getArtistV2(artistId)));

        double v1AvgMs = measureAverageMillis(() -> assertArtistDetail(artistService.getArtist(artistId)));
        double warmImprovementPercent = improvementPercent(v1AvgMs, v2WarmAvgMs);

        System.out.printf(
                "ARTIST_DETAIL_REDIS_CACHE_BENCHMARK memberCount=%d measureCount=%d v1AvgMs=%.3f v2ColdMs=%d v2WarmAvgMs=%.3f warmImprovementPercent=%.1f%%%n",
                MEMBER_COUNT,
                MEASURE_COUNT,
                v1AvgMs,
                v2ColdMs,
                v2WarmAvgMs,
                warmImprovementPercent
        );

        assertThat(v2WarmAvgMs).isLessThan(v1AvgMs);
    }

    private Long createBenchmarkArtistWithMembers() {
        Artist artist = artistRepository.save(Artist.create(
                "SEVENTEEN",
                "seventeen",
                null,
                null,
                "artist detail benchmark"
        ));

        for (int i = 1; i <= MEMBER_COUNT; i++) {
            Member member = memberRepository.save(Member.createNewMember(
                    "artist-member-" + i + "@example.com",
                    "password",
                    "artist-member-" + i,
                    "010-5000-" + String.format("%04d", i)
            ));
            artistMemberRepository.save(ArtistMember.create(
                    artist,
                    member,
                    "STAGE-" + i,
                    null,
                    i
            ));
        }
        artistMemberRepository.flush();
        return artist.getId();
    }

    private void assertArtistDetail(ArtistResponse response) {
        assertThat(response.name()).isEqualTo("SEVENTEEN");
        assertThat(response.artistMembers()).hasSize(MEMBER_COUNT);
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

    private void clearArtistDetailCache(Long artistId) {
        stringRedisTemplate.delete("cache:" + CacheNames.ARTIST_DETAIL_V2 + "::" + artistId);
    }

    private void truncateTestTables() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE artist_members");
        jdbcTemplate.execute("TRUNCATE TABLE artists");
        jdbcTemplate.execute("TRUNCATE TABLE members");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}
