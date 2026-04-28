package com.example.infinite.domain.artistcontent.comment.service;

import com.example.infinite.domain.artistcontent.comment.dto.response.CommentResponse;
import com.example.infinite.domain.artistcontent.comment.entity.Comment;
import com.example.infinite.domain.artistcontent.comment.repository.CommentRepository;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.artistcontent.post.fanpost.entity.FanPost;
import com.example.infinite.domain.artistcontent.post.fanpost.repository.FanPostRepository;
import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.artist.repository.ArtistRepository;
import com.example.infinite.domain.member.member.entity.Member;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("댓글 대댓글 커서 슬라이스 통합 테스트")
class CommentReplyCursorSliceIntegrationTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private FanPostRepository fanPostRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        clearCache(CacheNames.COMMENT_REPLY_LIST);
        clearCache(CacheNames.COMMENT_ROOT_SLICE);
        truncateTestTables();
    }

    @Test
    @DisplayName("대댓글은 20개 단위 커서 슬라이스로 계속 내려갈 수 있다")
    void repliesArePaginatedWithCursorSlice() {
        Member postWriter = memberRepository.save(Member.createNewMember(
                "post-writer@example.com",
                "password",
                "post-writer",
                "010-4100-0001"
        ));
        Member replyWriter = memberRepository.save(Member.createNewMember(
                "reply-writer@example.com",
                "password",
                "reply-writer",
                "010-4100-0002"
        ));
        Artist artist = artistRepository.save(Artist.create(
                "SEVENTEEN",
                "seventeen",
                null,
                null,
                "artist"
        ));
        FanPost fanPost = fanPostRepository.save(FanPost.create(artist, postWriter, "fan post"));
        Comment rootComment = commentRepository.save(Comment.create(
                PostType.FAN_POST,
                fanPost.getId(),
                postWriter,
                "root",
                null
        ));

        for (int i = 1; i <= 25; i++) {
            commentRepository.save(Comment.create(
                    PostType.FAN_POST,
                    fanPost.getId(),
                    replyWriter,
                    "reply-" + i,
                    rootComment
            ));
        }
        commentRepository.flush();

        CursorSliceResponse<CommentResponse> firstSlice = commentService.getFanPostReplies(
                artist.getId(),
                fanPost.getId(),
                rootComment.getId(),
                null
        );

        assertThat(firstSlice.content()).hasSize(20);
        assertThat(firstSlice.hasNext()).isTrue();
        assertThat(firstSlice.nextCursor()).isNotNull();
        assertThat(firstSlice.content())
                .extracting(CommentResponse::content)
                .containsExactly(
                        "reply-1", "reply-2", "reply-3", "reply-4", "reply-5",
                        "reply-6", "reply-7", "reply-8", "reply-9", "reply-10",
                        "reply-11", "reply-12", "reply-13", "reply-14", "reply-15",
                        "reply-16", "reply-17", "reply-18", "reply-19", "reply-20"
                );

        CursorSliceResponse<CommentResponse> secondSlice = commentService.getFanPostReplies(
                artist.getId(),
                fanPost.getId(),
                rootComment.getId(),
                firstSlice.nextCursor()
        );

        assertThat(secondSlice.content()).hasSize(5);
        assertThat(secondSlice.hasNext()).isFalse();
        assertThat(secondSlice.nextCursor()).isNull();
        assertThat(secondSlice.content())
                .extracting(CommentResponse::content)
                .containsExactly("reply-21", "reply-22", "reply-23", "reply-24", "reply-25");
    }

    private void clearCache(String cacheName) {
        if (cacheManager.getCache(cacheName) != null) {
            cacheManager.getCache(cacheName).clear();
        }
    }

    private void truncateTestTables() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE comment_mentions");
        jdbcTemplate.execute("TRUNCATE TABLE comments");
        jdbcTemplate.execute("TRUNCATE TABLE fan_posts");
        jdbcTemplate.execute("TRUNCATE TABLE artist_members");
        jdbcTemplate.execute("TRUNCATE TABLE artists");
        jdbcTemplate.execute("TRUNCATE TABLE members");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}
