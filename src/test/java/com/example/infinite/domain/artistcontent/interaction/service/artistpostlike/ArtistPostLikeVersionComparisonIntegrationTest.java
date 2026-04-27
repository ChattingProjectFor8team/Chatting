package com.example.infinite.domain.artistcontent.interaction.service.artistpostlike;

import com.example.infinite.domain.artistcontent.interaction.dto.response.ArtistPostLikeQueuedResponse;
import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.interaction.repository.InteractionRepository;
import com.example.infinite.domain.artistcontent.interaction.service.artistpostlike.stream.ArtistPostLikeStreamConsumer;
import com.example.infinite.domain.artistcontent.interaction.service.artistpostlike.stream.ArtistPostLikeStreamProducer;
import com.example.infinite.domain.artistcontent.interaction.service.artistpostlike.stream.ArtistPostLikeStreamV3Service;
import com.example.infinite.domain.artistcontent.post.artistpost.entity.ArtistPost;
import com.example.infinite.domain.artistcontent.post.artistpost.repository.ArtistPostRepository;
import com.example.infinite.domain.artistcontent.post.artistpost.service.likecount.ArtistPostLikeCountFlushScheduler;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.artist.repository.ArtistRepository;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.repository.MemberRepository;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.redis.RedisStreamGroupHelper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "artist-post.scheduler.enabled=false"
})
@ActiveProfiles("test")
@DisplayName("ArtistPost 좋아요 버전 비교 통합 테스트")
class ArtistPostLikeVersionComparisonIntegrationTest {

    private static final String LIKE_DLQ_STREAM_KEY = "artist-post:like:v3:dlq";

    @Autowired
    private ArtistPostLikeCoreService artistPostLikeCoreService;

    @Autowired
    private ArtistPostLikeLettuceV1Service artistPostLikeLettuceV1Service;

    @Autowired
    private ArtistPostLikeRedissonV2Service artistPostLikeRedissonV2Service;

    @Autowired
    private ArtistPostLikeStreamV3Service artistPostLikeStreamV3Service;

    @Autowired
    private ArtistPostLikeStreamConsumer artistPostLikeStreamConsumer;

    @Autowired
    private ArtistPostLikeCountFlushScheduler artistPostLikeCountFlushScheduler;

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private ArtistPostRepository artistPostRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisStreamGroupHelper redisStreamGroupHelper;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        clearInitializedStreamGroups();
        redisStreamGroupHelper.ensureGroup(
                ArtistPostLikeStreamProducer.STREAM_KEY,
                ArtistPostLikeStreamProducer.CONSUMER_GROUP
        );
        truncateTestTables();
    }

    @Test
    @DisplayName("락 없는 core toggle은 같은 멤버의 동시 요청에서 예외가 발생할 수 있다")
    void noLockCoreBurstCanFailForSameMember() throws Exception {
        ArtistPostFixture fixture = seedArtistPostFixture("nolock");

        ConcurrentRunResult result = runConcurrently(12, () ->
                artistPostLikeCoreService.toggle(fixture.actorMemberId(), fixture.artistId(), fixture.artistPostId())
        );

        assertThat(result.failureCount())
                .as("락이 없으면 같은 멤버의 동시 toggle에서 unique 충돌/정합성 예외가 드러나야 한다")
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("Lettuce v1은 같은 멤버의 연타를 직렬화하고 최종 상태를 일관되게 남긴다")
    void lettuceV1SerializesSameMemberBurst() throws Exception {
        ArtistPostFixture fixture = seedArtistPostFixture("lettuce");

        ConcurrentRunResult result = runConcurrently(5, () ->
                artistPostLikeLettuceV1Service.toggle(fixture.actorMemberId(), fixture.artistId(), fixture.artistPostId())
        );

        artistPostLikeCountFlushScheduler.flush();

        assertThat(result.failureCount()).isZero();
        assertThat(countArtistPostLikes(fixture.artistPostId())).isEqualTo(1L);
        assertThat(loadArtistPostLikeCount(fixture.artistPostId())).isEqualTo(1L);
    }

    @Test
    @DisplayName("Redisson v2는 같은 멤버의 연타를 직렬화하고 최종 상태를 일관되게 남긴다")
    void redissonV2SerializesSameMemberBurst() throws Exception {
        ArtistPostFixture fixture = seedArtistPostFixture("redisson");

        ConcurrentRunResult result = runConcurrently(5, () ->
                artistPostLikeRedissonV2Service.toggle(fixture.actorMemberId(), fixture.artistId(), fixture.artistPostId())
        );

        artistPostLikeCountFlushScheduler.flush();

        assertThat(result.failureCount()).isZero();
        assertThat(countArtistPostLikes(fixture.artistPostId())).isEqualTo(1L);
        assertThat(loadArtistPostLikeCount(fixture.artistPostId())).isEqualTo(1L);
    }

    @Test
    @DisplayName("Redis Stream v3는 같은 멤버의 빠른 연속 요청에서도 최종 의도 상태로 수렴한다")
    void streamV3ConvergesToLatestIntentForSameMemberBurst() throws Exception {
        ArtistPostFixture fixture = seedArtistPostFixture("stream");
        List<ArtistPostLikeQueuedResponse> queuedResponses = new CopyOnWriteArrayList<>();

        ConcurrentRunResult result = runConcurrently(5, () ->
                queuedResponses.add(artistPostLikeStreamV3Service.queue(
                        fixture.actorMemberId(),
                        fixture.artistId(),
                        fixture.artistPostId()
                ))
        );

        assertThat(result.failureCount()).isZero();
        assertThat(queuedResponses).hasSize(5);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    artistPostLikeStreamConsumer.consume();
                    artistPostLikeCountFlushScheduler.flush();

                    assertThat(countArtistPostLikes(fixture.artistPostId())).isEqualTo(1L);
                    assertThat(loadArtistPostLikeCount(fixture.artistPostId())).isEqualTo(1L);
                });

        Long dlqSize = stringRedisTemplate.opsForStream().size(LIKE_DLQ_STREAM_KEY);
        assertThat(dlqSize == null ? 0L : dlqSize).isZero();
    }

    private ArtistPostFixture seedArtistPostFixture(String slugSuffix) {
        Member writer = memberRepository.save(Member.createNewMember(
                "writer-" + slugSuffix + "@example.com",
                "password",
                "writer-" + slugSuffix,
                "010-1111-" + suffixDigits(slugSuffix, 1111)
        ));
        Member actor = memberRepository.save(Member.createNewMember(
                "actor-" + slugSuffix + "@example.com",
                "password",
                "actor-" + slugSuffix,
                "010-2222-" + suffixDigits(slugSuffix, 2222)
        ));
        Artist artist = artistRepository.save(Artist.create(
                "Artist " + slugSuffix,
                "artist-" + slugSuffix,
                null,
                null,
                "artist intro"
        ));
        ArtistPost artistPost = artistPostRepository.save(ArtistPost.create(
                artist,
                writer,
                "artist-post-content-" + slugSuffix
        ));

        return new ArtistPostFixture(
                artist.getId(),
                artistPost.getId(),
                actor.getId(),
                new MemberDetailsImpl(actor)
        );
    }

    private ConcurrentRunResult runConcurrently(int taskCount, ThrowingRunnable task) throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(taskCount);
        CyclicBarrier barrier = new CyclicBarrier(taskCount);
        AtomicInteger successCount = new AtomicInteger();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        for (int index = 0; index < taskCount; index++) {
            executorService.submit(() -> {
                try {
                    barrier.await();
                    task.run();
                    successCount.incrementAndGet();
                } catch (Throwable throwable) {
                    failures.add(throwable);
                }
            });
        }

        executorService.shutdown();
        assertThat(executorService.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        return new ConcurrentRunResult(successCount.get(), failures.size(), failures);
    }

    private long countArtistPostLikes(Long artistPostId) {
        return interactionRepository.countByTargetTypeAndTargetIdAndReactionType(
                PostType.ARTIST_POST,
                artistPostId,
                ReactionType.LIKE
        );
    }

    private long loadArtistPostLikeCount(Long artistPostId) {
        return artistPostRepository.findById(artistPostId)
                .orElseThrow()
                .getLikeCount();
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

    @SuppressWarnings("unchecked")
    private void clearInitializedStreamGroups() {
        Object field = ReflectionTestUtils.getField(redisStreamGroupHelper, "initializedGroups");
        if (field instanceof java.util.Set<?> groups) {
            ((java.util.Set<String>) groups).clear();
        }
    }

    private String suffixDigits(String seed, int fallback) {
        int hash = Math.abs(seed.hashCode());
        int value = hash % 10_000;
        if (value == 0) {
            value = fallback;
        }
        return "%04d".formatted(value);
    }

    private record ArtistPostFixture(
            Long artistId,
            Long artistPostId,
            Long actorMemberId,
            MemberDetailsImpl actorPrincipal
    ) {
    }

    private record ConcurrentRunResult(
            int successCount,
            int failureCount,
            List<Throwable> failures
    ) {
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
