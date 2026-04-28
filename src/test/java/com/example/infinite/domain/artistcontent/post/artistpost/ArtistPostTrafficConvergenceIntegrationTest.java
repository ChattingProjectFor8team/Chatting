package com.example.infinite.domain.artistcontent.post.artistpost;

import com.example.infinite.domain.artistcontent.comment.dto.request.CommentCreateRequest;
import com.example.infinite.domain.artistcontent.comment.dto.response.CommentResponse;
import com.example.infinite.domain.artistcontent.comment.repository.CommentRepository;
import com.example.infinite.domain.artistcontent.comment.service.CommentService;
import com.example.infinite.domain.artistcontent.comment.service.artistpoststream.ArtistPostCommentCountFlushScheduler;
import com.example.infinite.domain.artistcontent.comment.service.artistpoststream.ArtistPostCommentStreamConsumer;
import com.example.infinite.domain.artistcontent.interaction.dto.response.ArtistPostLikeQueuedResponse;
import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.interaction.repository.InteractionRepository;
import com.example.infinite.domain.artistcontent.interaction.service.InteractionService;
import com.example.infinite.domain.artistcontent.interaction.service.artistpostlike.stream.ArtistPostLikeStreamConsumer;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostDetailResponse;
import com.example.infinite.domain.artistcontent.post.artistpost.entity.ArtistPost;
import com.example.infinite.domain.artistcontent.post.artistpost.repository.ArtistPostRepository;
import com.example.infinite.domain.artistcontent.post.artistpost.service.ArtistPostService;
import com.example.infinite.domain.artistcontent.post.artistpost.service.likecount.ArtistPostLikeCountFlushScheduler;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.artist.entity.ArtistMember;
import com.example.infinite.domain.member.artist.repository.ArtistMemberRepository;
import com.example.infinite.domain.member.artist.repository.ArtistRepository;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.enums.MemberRole;
import com.example.infinite.domain.member.member.repository.MemberRepository;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import com.example.infinite.global.common.redis.RedisStreamGroupHelper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "artist-post.scheduler.enabled=false"
})
// 이 테스트는 "실서비스 스케줄러가 자동으로 돈다"는 가정을 끄고,
// consumer/flush를 테스트가 직접 한 틱씩 진행시키는 방식으로 작성한다.
//
// 왜 이렇게 하나:
// - 우리가 보고 싶은 것은 burst 이후 정합성이 어떻게 수렴하는지다
// - 그런데 스케줄러가 백그라운드에서 같이 돌면
//   테스트 수동 drain과 섞여 실패 원인이 "비즈니스 문제"인지 "타이밍 우연"인지 흐려진다
// - 실제로 수정 전에는 이중 소비/이중 flush 레이스가 발생해
//   duplicate key, 수렴 지연, 읽기 시점 혼선이 함께 나타났다
@ActiveProfiles("test")
@Tag("integration")
class ArtistPostTrafficConvergenceIntegrationTest {

    // 기본값은 평소 로컬/회귀 테스트에 맞춰 두고,
    // 더 큰 burst 실험이 필요할 때만 JVM system property로 수치를 올린다.
    //
    // 예:
    // -DartistPostTraffic.likeStormCount=100000
    // -DartistPostTraffic.commentStormCount=12000
    // -DartistPostTraffic.mixedLikeCount=4000
    // -DartistPostTraffic.mixedCommentCount=2000
    // -DartistPostTraffic.mixedReadCount=4000
    // -DartistPostTraffic.convergenceTimeoutSeconds=600
    //
    // 이렇게 해 두면 "기본 회귀 테스트"와 "로컬 고부하 정합성 실험"을
    // 같은 테스트 코드로 공유할 수 있고, 큰 숫자를 고정 커밋해서
    // 평소 테스트 시간을 불필요하게 늘리는 일도 피할 수 있다.
    private static final int LIKE_STORM_COUNT = intProperty("artistPostTraffic.likeStormCount", 10_000);
    private static final int COMMENT_STORM_COUNT = intProperty("artistPostTraffic.commentStormCount", 1_200);
    private static final int MIXED_LIKE_COUNT = intProperty("artistPostTraffic.mixedLikeCount", 1_500);
    private static final int MIXED_COMMENT_COUNT = intProperty("artistPostTraffic.mixedCommentCount", 700);
    private static final int MIXED_READ_COUNT = intProperty("artistPostTraffic.mixedReadCount", 2_000);
    private static final int CONVERGENCE_TIMEOUT_SECONDS =
            intProperty("artistPostTraffic.convergenceTimeoutSeconds", 180);

    private static final String LIKE_DLQ_STREAM_KEY = "artist-post:like:v3:dlq";
    private static final String COMMENT_DLQ_STREAM_KEY = "artist-post:comment:v2:dlq";

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("AWS_ACCESS_KEY_ID", () -> "test-access-key");
        registry.add("AWS_SECRET_ACCESS_KEY", () -> "test-secret-key");
        registry.add("AWS_REGION", () -> "ap-northeast-2");
        registry.add("AWS_S3_BUCKET", () -> "test-bucket");
        registry.add("CLOUDFRONT_DOMAIN", () -> "http://localhost");
        registry.add("JWT_SECRET_KEY", () -> "dGVzdC1qd3Qtc2VjcmV0LWZvci1hcnRpc3QtcG9zdC10ZXN0LTEyMzQ1Njc4OTA=");
        registry.add("PORTONE_API_SECRET", () -> "test-portone-secret");
        registry.add("PORTONE_WEBHOOK_SECRET", () -> "test-portone-webhook-secret");
        registry.add("MEDIA_STORAGE_ENABLED", () -> "false");
    }

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private ArtistMemberRepository artistMemberRepository;

    @Autowired
    private ArtistPostRepository artistPostRepository;

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private ArtistPostService artistPostService;

    @Autowired
    private ArtistPostLikeStreamConsumer artistPostLikeStreamConsumer;

    @Autowired
    private ArtistPostCommentStreamConsumer artistPostCommentStreamConsumer;

    @Autowired
    private ArtistPostLikeCountFlushScheduler artistPostLikeCountFlushScheduler;

    @Autowired
    private ArtistPostCommentCountFlushScheduler artistPostCommentCountFlushScheduler;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisStreamGroupHelper redisStreamGroupHelper;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        clearInitializedStreamGroups();
        clearAllCaches();
        ensureArtistPostStreamGroups();

        interactionRepository.deleteAllInBatch();
        commentRepository.deleteAllInBatch();
        artistPostRepository.deleteAllInBatch();
        artistMemberRepository.deleteAllInBatch();
        artistRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @Timeout(value = 900, unit = TimeUnit.SECONDS)
    @DisplayName("ArtistPost 좋아요 1만 건이 최종적으로 Reaction 수와 likeCount로 수렴한다")
    void likesEventuallyConvergeAfterTenThousandRequests() throws Exception {
        TestFixture fixture = seedArtistPost("bts-like-storm");
        List<MemberDetailsImpl> fanPrincipals = createFanPrincipals(LIKE_STORM_COUNT, 10_000);

        runConcurrently(fanPrincipals.size(), 128, index -> {
            ArtistPostLikeQueuedResponse response = interactionService.queueArtistPostLikeV3(
                    fanPrincipals.get(index),
                    fixture.artistId(),
                    fixture.artistPostId()
            );

            if (!response.expectedReacted()) {
                throw new AssertionError("first like burst should request reacted=true");
            }
        });

        awaitFinalConvergence(fixture.artistPostId(), LIKE_STORM_COUNT, 0);

        assertThat(interactionRepository.countByTargetTypeAndTargetIdAndReactionType(
                PostType.ARTIST_POST,
                fixture.artistPostId(),
                ReactionType.LIKE
        )).isEqualTo(LIKE_STORM_COUNT);
        assertThat(loadArtistPostLikeCount(fixture.artistPostId())).isEqualTo(LIKE_STORM_COUNT);
        assertDlqIsEmpty(LIKE_DLQ_STREAM_KEY);
    }

    @Test
    @Timeout(value = 600, unit = TimeUnit.SECONDS)
    @DisplayName("ArtistPost 댓글 수백~수천 건이 최종적으로 Comment 수와 commentCount로 수렴한다")
    void commentsEventuallyConvergeAfterBurstRequests() throws Exception {
        TestFixture fixture = seedArtistPost("bts-comment-storm");
        List<MemberDetailsImpl> fanPrincipals = createFanPrincipals(COMMENT_STORM_COUNT, 30_000);

        runConcurrently(fanPrincipals.size(), 96, index -> commentService.queueArtistPostCommentV2(
                fanPrincipals.get(index),
                fixture.artistId(),
                fixture.artistPostId(),
                new CommentCreateRequest("burst-comment-" + index, null)
        ));

        awaitFinalConvergence(fixture.artistPostId(), 0, COMMENT_STORM_COUNT);

        assertThat(loadActiveArtistPostCommentCount(fixture.artistPostId())).isEqualTo(COMMENT_STORM_COUNT);
        assertThat(loadArtistPostCommentCount(fixture.artistPostId())).isEqualTo(COMMENT_STORM_COUNT);

        CursorSliceResponse<CommentResponse> rootSlice =
                commentService.getArtistPostComments(fixture.artistId(), fixture.artistPostId(), null);
        assertThat(rootSlice.content()).isNotEmpty();
        assertThat(rootSlice.content()).hasSizeLessThanOrEqualTo(20);
        assertThat(rootSlice.content()).allSatisfy(comment -> {
            assertThat(comment.depth()).isEqualTo(1);
            assertThat(comment.replyCount()).isGreaterThanOrEqualTo(0);
        });
        assertDlqIsEmpty(COMMENT_DLQ_STREAM_KEY);
    }

    @Test
    @Timeout(value = 900, unit = TimeUnit.SECONDS)
    @DisplayName("읽기와 쓰기가 섞인 상황에서도 조회 응답은 깨지지 않고 최종 count가 수렴한다")
    void readsRemainStableDuringMixedReadWriteTraffic() throws Exception {
        TestFixture fixture = seedArtistPost("bts-mixed-traffic");
        List<MemberDetailsImpl> likePrincipals = createFanPrincipals(MIXED_LIKE_COUNT, 50_000);
        List<MemberDetailsImpl> commentPrincipals = createFanPrincipals(MIXED_COMMENT_COUNT, 70_000);

        List<ThrowingRunnable> tasks = new ArrayList<>();
        for (MemberDetailsImpl principal : likePrincipals) {
            tasks.add(() -> interactionService.queueArtistPostLikeV3(principal, fixture.artistId(), fixture.artistPostId()));
        }
        for (int i = 0; i < commentPrincipals.size(); i++) {
            MemberDetailsImpl principal = commentPrincipals.get(i);
            int commentIndex = i;
            tasks.add(() -> commentService.queueArtistPostCommentV2(
                    principal,
                    fixture.artistId(),
                    fixture.artistPostId(),
                    new CommentCreateRequest("mixed-comment-" + commentIndex, null)
            ));
        }
        for (int i = 0; i < MIXED_READ_COUNT; i++) {
            tasks.add(() -> assertReadSnapshotIsSane(fixture.artistId(), fixture.artistPostId()));
        }
        Collections.shuffle(tasks);

        AtomicBoolean keepDraining = new AtomicBoolean(true);
        ExecutorService drainExecutor = Executors.newSingleThreadExecutor();
        Future<?> drainFuture = drainExecutor.submit(() -> {
            while (keepDraining.get()) {
                consumeAndFlushOnce();
                Thread.sleep(50L);
            }
            return null;
        });

        try {
            runConcurrently(tasks, 128);
        } finally {
            keepDraining.set(false);
            // 수정 전 10초 제한은 drain 스레드가 마지막 backlog를 정리하기엔 빡빡했다.
            // 이 테스트는 성능 benchmark가 아니라 "읽기와 쓰기를 섞어도 최종 정합성이 깨지지 않는가"를 보므로
            // 종료 대기 시간을 조금 더 넉넉히 줘 false negative를 줄인다.
            drainFuture.get(30, TimeUnit.SECONDS);
            drainExecutor.shutdownNow();
        }

        awaitFinalConvergence(fixture.artistPostId(), MIXED_LIKE_COUNT, MIXED_COMMENT_COUNT);

        assertThat(loadArtistPostLikeCount(fixture.artistPostId())).isEqualTo(MIXED_LIKE_COUNT);
        assertThat(loadArtistPostCommentCount(fixture.artistPostId())).isEqualTo(MIXED_COMMENT_COUNT);
        assertThat(loadActiveArtistPostCommentCount(fixture.artistPostId())).isEqualTo(MIXED_COMMENT_COUNT);
        assertDlqIsEmpty(LIKE_DLQ_STREAM_KEY);
        assertDlqIsEmpty(COMMENT_DLQ_STREAM_KEY);
    }

    private void assertReadSnapshotIsSane(Long artistId, Long artistPostId) {
        // mixed traffic 테스트의 핵심은
        // "최종 count가 맞는다"뿐 아니라 "중간 읽기 응답 shape가 깨지지 않는다"까지 보는 것이다.
        // 즉 아직 delta flush가 덜 끝난 도중이라 숫자가 완전 최신이 아닐 수는 있어도,
        // 응답 구조가 null이 되거나 depth 규칙이 깨지거나 캐시 역직렬화 예외가 나면 안 된다.
        ArtistPostDetailResponse detail = artistPostService.getArtistPost(artistId, artistPostId, null);
        assertThat(detail.artistPostId()).isEqualTo(artistPostId);
        assertThat(detail.likeCount()).isGreaterThanOrEqualTo(0L);
        assertThat(detail.commentCount()).isGreaterThanOrEqualTo(0L);
        assertThat(detail.comments()).isNotNull();
        assertThat(detail.comments().content()).hasSizeLessThanOrEqualTo(20);

        CursorSliceResponse<CommentResponse> rootSlice = commentService.getArtistPostComments(artistId, artistPostId, null);
        assertThat(rootSlice.content()).hasSizeLessThanOrEqualTo(20);
        rootSlice.content().forEach(rootComment -> {
            assertThat(rootComment.depth()).isEqualTo(1);
            assertThat(rootComment.likeCount()).isGreaterThanOrEqualTo(0L);

            if (ThreadLocalRandom.current().nextBoolean()) {
                CursorSliceResponse<CommentResponse> replies = commentService.getArtistPostReplies(
                        artistId,
                        artistPostId,
                        rootComment.commentId(),
                        null
                );
                assertThat(replies.content()).hasSizeLessThanOrEqualTo(20);
                assertThat(replies.content()).allSatisfy(reply -> {
                    assertThat(reply.depth()).isEqualTo(2);
                    assertThat(reply.parentCommentId()).isEqualTo(rootComment.commentId());
                    assertThat(reply.likeCount()).isGreaterThanOrEqualTo(0L);
                });
            }
        });
    }

    private void awaitFinalConvergence(Long artistPostId, long expectedLikeCount, long expectedCommentCount) {
        Awaitility.await()
                .pollDelay(Duration.ZERO)
                .pollInterval(Duration.ofMillis(200))
                // 이 테스트의 목적은 "burst 이후 최종 정합성 수렴" 검증이지
                // 로컬 단일 consumer가 60초 안에 1만 건을 모두 처리해야 한다는 성능 SLA 검증이 아니다.
                //
                // 수정 전에는 60초 제한 때문에
                // "정합성은 결국 맞는데 로컬 처리량이 부족해서 timeout"인 false negative가 났다.
                // 그래서 이 테스트는 시간을 조금 더 주고,
                // 대신 원본 Reaction 수와 집계 count가 끝까지 정확히 일치하는지를 강하게 검증한다.
                .atMost(Duration.ofSeconds(CONVERGENCE_TIMEOUT_SECONDS))
                .untilAsserted(() -> {
                    consumeAndFlushOnce();

                    assertThat(loadArtistPostLikeCount(artistPostId)).isEqualTo(expectedLikeCount);
                    assertThat(loadArtistPostCommentCount(artistPostId)).isEqualTo(expectedCommentCount);
                    assertThat(interactionRepository.countByTargetTypeAndTargetIdAndReactionType(
                            PostType.ARTIST_POST,
                            artistPostId,
                            ReactionType.LIKE
                    )).isEqualTo(expectedLikeCount);
                    assertThat(loadActiveArtistPostCommentCount(artistPostId)).isEqualTo(expectedCommentCount);
                });
    }

    private void consumeAndFlushOnce() {
        // 테스트에서 "한 틱"의 의미를 명시적으로 보이기 위한 메서드다.
        // 1) like stream consume
        // 2) comment stream consume
        // 3) like delta flush
        // 4) comment delta flush
        //
        // 이 순서로 한 번 돌리고 나면
        // "queue에 있던 명령 일부가 DB 원본과 count에 어디까지 반영됐는지"를 단계적으로 관찰할 수 있다.
        artistPostLikeStreamConsumer.consume();
        artistPostCommentStreamConsumer.consume();
        artistPostLikeCountFlushScheduler.flush();
        artistPostCommentCountFlushScheduler.flush();
    }

    private TestFixture seedArtistPost(String slug) {
        Member artistWriter = Member.createNewMember(
                "artist-" + slug + "@example.com",
                "password",
                "artist-" + slug,
                buildPhoneNumber(1_000)
        );
        artistWriter.changeRole(MemberRole.ARTIST);
        artistWriter = memberRepository.saveAndFlush(artistWriter);

        Artist artist = artistRepository.saveAndFlush(Artist.create(
                "BTS-" + slug,
                slug,
                null,
                null,
                "traffic-test"
        ));

        artistMemberRepository.saveAndFlush(ArtistMember.create(
                artist,
                artistWriter,
                "RM",
                null,
                1
        ));

        ArtistPost artistPost = artistPostRepository.saveAndFlush(ArtistPost.create(
                artist,
                artistWriter,
                "BTS artist post for traffic convergence test"
        ));

        return new TestFixture(artist.getId(), artistPost.getId());
    }

    private List<MemberDetailsImpl> createFanPrincipals(int count, int baseIndex) {
        List<Member> members = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int seed = baseIndex + i;
            members.add(Member.createNewMember(
                    "fan-" + seed + "@example.com",
                    "password",
                    "fan-" + seed,
                    buildPhoneNumber(seed)
            ));
        }

        List<Member> savedMembers = memberRepository.saveAllAndFlush(members);
        return savedMembers.stream()
                .map(member -> MemberDetailsImpl.fromToken(member.getEmail(), member.getRole().name(), member.getId()))
                .toList();
    }

    private long loadArtistPostLikeCount(Long artistPostId) {
        Long count = jdbcTemplate.queryForObject(
                "select like_count from artist_posts where id = ?",
                Long.class,
                artistPostId
        );
        return count == null ? 0L : count;
    }

    private long loadArtistPostCommentCount(Long artistPostId) {
        Long count = jdbcTemplate.queryForObject(
                "select comment_count from artist_posts where id = ?",
                Long.class,
                artistPostId
        );
        return count == null ? 0L : count;
    }

    private long loadActiveArtistPostCommentCount(Long artistPostId) {
        Long count = jdbcTemplate.queryForObject(
                """
                select count(*)
                  from comments
                 where target_type = 'ARTIST_POST'
                   and target_id = ?
                   and deleted_at is null
                   and deleted_placeholder = false
                """,
                Long.class,
                artistPostId
        );
        return count == null ? 0L : count;
    }

    private void assertDlqIsEmpty(String streamKey) {
        Long size = stringRedisTemplate.opsForStream().size(streamKey);
        assertThat(size == null ? 0L : size).isZero();
    }

    private void clearAllCaches() {
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void clearInitializedStreamGroups() {
        Object field = ReflectionTestUtils.getField(redisStreamGroupHelper, "initializedGroups");
        if (field instanceof java.util.Set<?> groups) {
            ((java.util.Set<String>) groups).clear();
        }
    }

    private void ensureArtistPostStreamGroups() {
        redisStreamGroupHelper.ensureGroup(
                "artist-post:like:v3:commands",
                "artist-post-like-v3-group"
        );
        redisStreamGroupHelper.ensureGroup(
                "artist-post:comment:v2:commands",
                "artist-post-comment-v2-group"
        );
    }

    private void runConcurrently(int taskCount, int poolSize, IndexedTask indexedTask) throws Exception {
        List<ThrowingRunnable> tasks = new ArrayList<>(taskCount);
        for (int i = 0; i < taskCount; i++) {
            int index = i;
            tasks.add(() -> indexedTask.run(index));
        }
        runConcurrently(tasks, poolSize);
    }

    private void runConcurrently(List<ThrowingRunnable> tasks, int poolSize) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(tasks.size());
        ConcurrentLinkedQueue<Throwable> failures = new ConcurrentLinkedQueue<>();
        AtomicInteger submittedCount = new AtomicInteger();

        for (ThrowingRunnable task : tasks) {
            submittedCount.incrementAndGet();
            executor.submit(() -> {
                try {
                    start.await();
                    task.run();
                } catch (Throwable t) {
                    failures.add(t);
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(120, TimeUnit.SECONDS)).isTrue();

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        if (!failures.isEmpty()) {
            Throwable first = failures.peek();
            List<String> samples = failures.stream()
                    .filter(Objects::nonNull)
                    .limit(5)
                    .map(Throwable::toString)
                    .toList();
            fail("concurrent execution failed: submitted=%d, failureCount=%d, samples=%s"
                    .formatted(submittedCount.get(), failures.size(), samples), first);
        }
    }

    private String buildPhoneNumber(int seed) {
        int normalized = Math.abs(seed % 100_000_000);
        return "010-%04d-%04d".formatted(normalized / 10_000, normalized % 10_000);
    }

    private static int intProperty(String key, int defaultValue) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("invalid integer system property: %s=%s".formatted(key, raw), e);
        }
    }

    private record TestFixture(
            Long artistId,
            Long artistPostId
    ) {
    }

    @FunctionalInterface
    private interface IndexedTask {
        void run(int index) throws Exception;
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
