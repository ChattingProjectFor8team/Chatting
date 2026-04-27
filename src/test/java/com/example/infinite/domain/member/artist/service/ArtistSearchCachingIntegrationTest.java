package com.example.infinite.domain.member.artist.service;

import com.example.infinite.domain.member.artist.dto.request.ArtistUpdateRequest;
import com.example.infinite.domain.member.artist.dto.response.ArtistPopularSearchResponse;
import com.example.infinite.domain.member.artist.dto.response.ArtistResponse;
import com.example.infinite.domain.member.artist.dto.response.ArtistSearchResponse;
import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.artist.entity.ArtistMember;
import com.example.infinite.domain.member.artist.repository.ArtistMemberRepository;
import com.example.infinite.domain.member.artist.repository.ArtistRepository;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.enums.MemberRole;
import com.example.infinite.domain.member.member.repository.MemberRepository;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.config.CacheConfig;
import com.example.infinite.global.common.constant.CacheNames;
import com.example.infinite.global.common.dto.OffsetSliceResponse;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("아티스트 검색/캐시/인기검색어 통합 테스트")
class ArtistSearchCachingIntegrationTest {

    @Autowired
    private ArtistService artistService;

    @Autowired
    private ArtistSearchKeywordService artistSearchKeywordService;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private ArtistMemberRepository artistMemberRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    @Qualifier("caffeineCacheManager")
    private CacheManager caffeineCacheManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        clearCaches(cacheManager, CacheNames.ARTIST_DETAIL_V2);
        clearCaches(cacheManager, CacheNames.ARTIST_SEARCH_V3);
        clearCaches(caffeineCacheManager, CacheConfig.ARTIST_SEARCH_V2_CACHE);
        truncateTestTables();
    }

    @Test
    @DisplayName("검색 v2는 로컬 캐시된 결과를 유지하고 v1은 최신 DB 상태를 즉시 반영한다")
    void searchV2KeepsLocalCachedResultWhileV1ReflectsLatestDatabaseState() {
        Artist artist = artistRepository.save(Artist.create(
                "BTS",
                "bts",
                null,
                null,
                "cached search target"
        ));

        PageResponse<ArtistSearchResponse> cachedBefore = artistService.searchArtistsV2("bts", 1);
        assertThat(cachedBefore.content()).extracting(ArtistSearchResponse::name).containsExactly("BTS");
        assertThat(caffeineCacheManager.getCache(CacheConfig.ARTIST_SEARCH_V2_CACHE).get("keyword:bts:page:1")).isNotNull();

        artist.updateProfile("SEVENTEEN", "seventeen", null, null, "renamed");
        artistRepository.saveAndFlush(artist);

        PageResponse<ArtistSearchResponse> v1AfterRename = artistService.searchArtistsV1("bts", 1);
        PageResponse<ArtistSearchResponse> v2AfterRename = artistService.searchArtistsV2(" BTS ", 1);

        assertThat(v1AfterRename.content()).isEmpty();
        assertThat(v2AfterRename.content())
                .extracting(ArtistSearchResponse::name)
                .containsExactly("BTS");
    }

    @Test
    @DisplayName("검색 v3는 Redis 캐시에 적재되고 동일 키/페이지 재조회 시 Redis key를 재사용한다")
    void searchV3UsesRedisCacheWithKeywordAndPageKey() {
        artistRepository.saveAndFlush(Artist.create(
                "BTS",
                "bts",
                null,
                null,
                "redis cached search target"
        ));

        PageResponse<ArtistSearchResponse> firstResponse = artistService.searchArtistsV3("bts", 1);
        PageResponse<ArtistSearchResponse> secondResponse = artistService.searchArtistsV3(" BTS ", 1);

        assertThat(firstResponse.content()).extracting(ArtistSearchResponse::name).containsExactly("BTS");
        assertThat(secondResponse.content()).extracting(ArtistSearchResponse::name).containsExactly("BTS");
        assertThat(stringRedisTemplate.keys("cache:" + CacheNames.ARTIST_SEARCH_V3 + "::keyword:bts:page:1"))
                .containsExactly("cache:" + CacheNames.ARTIST_SEARCH_V3 + "::keyword:bts:page:1");
    }

    @Test
    @DisplayName("아티스트 상세 v2는 Redis 캐시를 사용하고 update 시 evict 후 최신 값으로 다시 적재된다")
    void artistDetailV2UsesRedisCacheAndEvictsOnUpdate() {
        Member owner = memberRepository.save(Member.createNewMember(
                "artist-owner@example.com",
                "password",
                "artist-owner",
                "010-3000-0001"
        ));
        owner.changeRole(MemberRole.ARTIST);
        memberRepository.saveAndFlush(owner);

        Artist artist = artistRepository.save(Artist.create(
                "OLD NAME",
                "old-name",
                null,
                null,
                "old intro"
        ));
        artistMemberRepository.saveAndFlush(ArtistMember.create(
                artist,
                owner,
                "OWNER",
                null,
                1
        ));

        ArtistResponse cachedResponse = artistService.getArtistV2(artist.getId());
        assertThat(cachedResponse.name()).isEqualTo("OLD NAME");
        assertThat(stringRedisTemplate.keys("cache:" + CacheNames.ARTIST_DETAIL_V2 + "::" + artist.getId()))
                .containsExactly("cache:" + CacheNames.ARTIST_DETAIL_V2 + "::" + artist.getId());

        ArtistResponse updatedResponse = artistService.updateArtist(
                new MemberDetailsImpl(owner),
                artist.getId(),
                new ArtistUpdateRequest("NEW NAME", null, null, null, "new intro")
        );
        assertThat(updatedResponse.name()).isEqualTo("NEW NAME");
        assertThat(stringRedisTemplate.keys("cache:" + CacheNames.ARTIST_DETAIL_V2 + "::" + artist.getId()))
                .isEmpty();

        ArtistResponse reloadedResponse = artistService.getArtistV2(artist.getId());
        assertThat(reloadedResponse.name()).isEqualTo("NEW NAME");
    }

    @Test
    @DisplayName("인기 검색어는 동일 사용자 중복을 막고 공백/대소문자를 정규화한다")
    void popularKeywordsDeduplicateSameUserAndNormalizeInput() {
        artistSearchKeywordService.recordSearchKeyword("USER1@EXAMPLE.COM", " BTS ");
        artistSearchKeywordService.recordSearchKeyword("user1@example.com", "bts");
        artistSearchKeywordService.recordSearchKeyword("user2@example.com", "BTS");
        artistSearchKeywordService.recordSearchKeyword("user3@example.com", "BLACKPINK");

        OffsetSliceResponse<ArtistPopularSearchResponse> popularKeywords = artistSearchKeywordService.getPopularKeywords(0);

        assertThat(popularKeywords.content()).hasSize(2);
        assertThat(popularKeywords.content().get(0).keyword()).isEqualTo("bts");
        assertThat(popularKeywords.content().get(0).score()).isEqualTo(2L);
        assertThat(popularKeywords.content().get(1).keyword()).isEqualTo("blackpink");
        assertThat(popularKeywords.content().get(1).score()).isEqualTo(1L);
    }

    @Test
    @DisplayName("검색은 size 10 고정으로 페이지를 넘겨 전체 결과를 볼 수 있다")
    void searchUsesFixedPageSizeButSupportsNextPages() {
        for (int i = 1; i <= 15; i++) {
            artistRepository.save(Artist.create(
                    "BTS-" + i,
                    "bts-" + i,
                    null,
                    null,
                    "artist-" + i
            ));
        }
        artistRepository.flush();

        PageResponse<ArtistSearchResponse> firstPage = artistService.searchArtistsV1("bts", 1);
        PageResponse<ArtistSearchResponse> secondPage = artistService.searchArtistsV1("bts", 2);

        assertThat(firstPage.size()).isEqualTo(10);
        assertThat(firstPage.content()).hasSize(10);
        assertThat(firstPage.isLast()).isFalse();
        assertThat(secondPage.content()).hasSize(5);
        assertThat(secondPage.number()).isEqualTo(2);
        assertThat(secondPage.isLast()).isTrue();
    }

    @Test
    @DisplayName("인기 검색어도 10개씩 offset 페이지를 넘겨 계속 조회할 수 있다")
    void popularKeywordsSupportNextOffsetPagesWithFixedPageSize() {
        for (int i = 1; i <= 12; i++) {
            artistSearchKeywordService.recordSearchKeyword("user" + i + "@example.com", "keyword-" + i);
        }

        OffsetSliceResponse<ArtistPopularSearchResponse> firstSlice = artistSearchKeywordService.getPopularKeywords(0);
        OffsetSliceResponse<ArtistPopularSearchResponse> secondSlice = artistSearchKeywordService.getPopularKeywords(firstSlice.nextOffset());

        assertThat(firstSlice.size()).isEqualTo(10);
        assertThat(firstSlice.content()).hasSize(10);
        assertThat(firstSlice.hasNext()).isTrue();
        assertThat(firstSlice.nextOffset()).isEqualTo(10);
        assertThat(secondSlice.content()).hasSize(2);
        assertThat(secondSlice.hasNext()).isFalse();
    }

    private void clearCaches(CacheManager targetCacheManager, String... cacheNames) {
        for (String cacheName : cacheNames) {
            if (targetCacheManager.getCache(cacheName) != null) {
                targetCacheManager.getCache(cacheName).clear();
            }
        }
    }

    private void truncateTestTables() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE follows");
        jdbcTemplate.execute("TRUNCATE TABLE comments");
        jdbcTemplate.execute("TRUNCATE TABLE reactions");
        jdbcTemplate.execute("TRUNCATE TABLE fan_posts");
        jdbcTemplate.execute("TRUNCATE TABLE artist_posts");
        jdbcTemplate.execute("TRUNCATE TABLE artist_members");
        jdbcTemplate.execute("TRUNCATE TABLE artists");
        jdbcTemplate.execute("TRUNCATE TABLE members");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}
