package com.example.infinite.domain.artistcontent.index;

import com.example.infinite.domain.artistcontent.comment.entity.Comment;
import com.example.infinite.domain.artistcontent.comment.repository.CommentRepository;
import com.example.infinite.domain.artistcontent.follow.entity.Follow;
import com.example.infinite.domain.artistcontent.follow.repository.FollowRepository;
import com.example.infinite.domain.artistcontent.post.artistpost.entity.ArtistPost;
import com.example.infinite.domain.artistcontent.post.artistpost.repository.ArtistPostRepository;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.artistcontent.post.fanpost.entity.FanPost;
import com.example.infinite.domain.artistcontent.post.fanpost.repository.FanPostRepository;
import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.artist.entity.ArtistMember;
import com.example.infinite.domain.member.artist.repository.ArtistMemberRepository;
import com.example.infinite.domain.member.artist.repository.ArtistRepository;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("member/artistcontent 인덱스 EXPLAIN 검증")
class MemberArtistContentIndexExplainIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private ArtistMemberRepository artistMemberRepository;

    @Autowired
    private FanPostRepository fanPostRepository;

    @Autowired
    private ArtistPostRepository artistPostRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private FollowRepository followRepository;

    private Long artistId;
    private Long commentRootId;
    private Long followerMemberId;
    private Long fanPostCursor;
    private Long artistPostCursor;
    private Long artistPostTargetId;

    @BeforeEach
    void setUp() {
        truncateTestTables();
        seedFixture();
    }

    @Test
    @DisplayName("FanPost 목록 커서 조회는 idx_fan_posts_artist_id_id를 사용한다")
    void fanPostSliceQueryUsesArtistIdIdIndex() {
        Map<String, Object> plan = explain("""
                SELECT id, artist_id, member_id
                  FROM fan_posts
                 WHERE artist_id = ?
                   AND id < ?
                   AND deleted_at IS NULL
                 ORDER BY id DESC
                 LIMIT 20
                """, artistId, fanPostCursor);

        assertThat(String.valueOf(plan.get("possible_keys"))).contains("idx_fan_posts_artist_id_id");
        assertThat(plan.get("key").toString()).isIn("idx_fan_posts_artist_id_id", "PRIMARY");
        assertThat(plan.get("type").toString()).isNotEqualTo("ALL");
    }

    @Test
    @DisplayName("ArtistPost 목록 커서 조회는 idx_artist_posts_artist_id_id를 사용한다")
    void artistPostSliceQueryUsesArtistIdIdIndex() {
        Map<String, Object> plan = explain("""
                SELECT id, artist_id, member_id
                  FROM artist_posts
                 WHERE artist_id = ?
                   AND id < ?
                   AND deleted_at IS NULL
                 ORDER BY id DESC
                 LIMIT 20
                """, artistId, artistPostCursor);

        assertThat(String.valueOf(plan.get("possible_keys"))).contains("idx_artist_posts_artist_id_id");
        assertThat(plan.get("key").toString()).isIn("idx_artist_posts_artist_id_id", "PRIMARY");
        assertThat(plan.get("type").toString()).isNotEqualTo("ALL");
    }

    @Test
    @DisplayName("댓글 루트 커서 조회는 idx_comments_target을 사용한다")
    void rootCommentSliceQueryUsesTargetIndex() {
        Map<String, Object> plan = explain("""
                SELECT id, target_id, parent_id
                  FROM comments
                 WHERE target_type = 'ARTIST_POST'
                   AND target_id = ?
                   AND parent_id IS NULL
                   AND deleted_at IS NULL
                   AND id < ?
                 ORDER BY id DESC
                 LIMIT 20
                """, artistPostTargetId, commentRootId + 10_000);

        assertThat(String.valueOf(plan.get("possible_keys"))).contains("idx_comments_target");
        assertThat(plan.get("key").toString()).isIn("idx_comments_target", "PRIMARY");
        assertThat(plan.get("type").toString()).isNotEqualTo("ALL");
    }

    @Test
    @DisplayName("댓글 대댓글 조회는 idx_comments_parent를 사용한다")
    void replyQueryUsesParentIndex() {
        Map<String, Object> plan = explain("""
                SELECT id, parent_id
                  FROM comments
                 WHERE parent_id = ?
                   AND deleted_at IS NULL
                 ORDER BY id ASC
                 LIMIT 20
                """, commentRootId);

        assertThat(plan.get("key")).isEqualTo("idx_comments_parent");
        assertThat(plan.get("type").toString()).isNotEqualTo("ALL");
    }

    @Test
    @DisplayName("Follow 목록 조회는 idx_follows_follower_id_id를 사용한다")
    void followListQueryUsesFollowerIdIdIndex() {
        Map<String, Object> plan = explain("""
                SELECT id, follower_member_id, target_artist_member_id
                  FROM follows
                 WHERE follower_member_id = ?
                 ORDER BY id DESC
                 LIMIT 20
                """, followerMemberId);

        assertThat(String.valueOf(plan.get("possible_keys")))
                .contains("idx_follows_follower_id_id")
                .contains("uk_follow_follower_target_artist_member");
        assertThat(plan.get("key").toString())
                .isIn("idx_follows_follower_id_id", "uk_follow_follower_target_artist_member");
        assertThat(plan.get("type").toString()).isNotEqualTo("ALL");
    }

    private void seedFixture() {
        Member writer = memberRepository.save(Member.createNewMember(
                "writer@index-test.example.com",
                "password",
                "writer-index-test",
                "010-4000-0001"
        ));
        Member follower = memberRepository.save(Member.createNewMember(
                "follower@index-test.example.com",
                "password",
                "follower-index-test",
                "010-4000-0002"
        ));
        followerMemberId = follower.getId();

        Artist artist = artistRepository.save(Artist.create(
                "Index Artist",
                "index-artist",
                null,
                null,
                "index fixture artist"
        ));
        artistId = artist.getId();

        List<FanPost> fanPosts = java.util.stream.IntStream.rangeClosed(1, 40)
                .mapToObj(index -> FanPost.create(artist, writer, "fan-post-" + index))
                .toList();
        fanPostRepository.saveAll(fanPosts);
        fanPostCursor = fanPosts.get(fanPosts.size() - 1).getId() + 10_000;

        List<ArtistPost> artistPosts = java.util.stream.IntStream.rangeClosed(1, 40)
                .mapToObj(index -> ArtistPost.create(artist, writer, "artist-post-" + index))
                .toList();
        artistPostRepository.saveAll(artistPosts);
        artistPostTargetId = artistPosts.get(artistPosts.size() - 1).getId();
        artistPostCursor = artistPosts.get(artistPosts.size() - 1).getId() + 10_000;

        List<Comment> rootComments = java.util.stream.IntStream.rangeClosed(1, 25)
                .mapToObj(index -> Comment.create(
                        PostType.ARTIST_POST,
                        artistPostTargetId,
                        writer,
                        "root-comment-" + index,
                        null
                ))
                .toList();
        commentRepository.saveAll(rootComments);
        Comment rootComment = rootComments.get(0);
        commentRootId = rootComment.getId();
        List<Comment> replyComments = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(index -> Comment.create(
                        PostType.ARTIST_POST,
                        artistPostTargetId,
                        writer,
                        "reply-comment-" + index,
                        rootComment
                ))
                .toList();
        commentRepository.saveAll(replyComments);

        List<ArtistMember> targetArtistMembers = java.util.stream.IntStream.rangeClosed(1, 15)
                .mapToObj(index -> {
                    Member targetMember = memberRepository.save(Member.createNewMember(
                            "target-" + index + "@index-test.example.com",
                            "password",
                            "target-" + index,
                            "010-4100-%04d".formatted(index)
                    ));
                    return ArtistMember.create(artist, targetMember, "stage-" + index, null, index);
                })
                .toList();
        artistMemberRepository.saveAll(targetArtistMembers);

        List<Follow> follows = targetArtistMembers.stream()
                .map(targetArtistMember -> Follow.create(follower, targetArtistMember))
                .toList();
        followRepository.saveAll(follows);
    }

    private Map<String, Object> explain(String sql, Object... args) {
        return jdbcTemplate.queryForList("EXPLAIN " + sql, args).get(0);
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
