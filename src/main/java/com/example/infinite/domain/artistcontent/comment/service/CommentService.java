package com.example.infinite.domain.artistcontent.comment.service;

import com.example.infinite.domain.artistcontent.comment.dto.request.CommentCreateRequest;
import com.example.infinite.domain.artistcontent.comment.dto.response.ArtistPostCommentQueuedResponse;
import com.example.infinite.domain.artistcontent.comment.dto.response.CommentMentionResponse;
import com.example.infinite.domain.artistcontent.comment.dto.response.CommentResponse;
import com.example.infinite.domain.artistcontent.comment.entity.Comment;
import com.example.infinite.domain.artistcontent.comment.error.CommentErrorCode;
import com.example.infinite.domain.artistcontent.comment.error.CommentException;
import com.example.infinite.domain.artistcontent.comment.repository.CommentRepository;
import com.example.infinite.domain.artistcontent.comment.service.fanpost.FanPostCommentRedissonService;
import com.example.infinite.domain.artistcontent.comment.service.artistpoststream.ArtistPostCommentStreamV2Service;
import com.example.infinite.domain.artistcontent.comment.service.cache.CommentCacheInvalidationEvent;
import com.example.infinite.domain.artistcontent.comment.service.cache.CommentQueryCacheService;
import com.example.infinite.domain.artistcontent.comment.support.CommentReader;
import com.example.infinite.domain.artistcontent.comment.support.MentionParser;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.artistcontent.post.artistpost.entity.ArtistPost;
import com.example.infinite.domain.artistcontent.post.artistpost.support.ArtistPostReader;
import com.example.infinite.domain.artistcontent.post.fanpost.entity.FanPost;
import com.example.infinite.domain.artistcontent.post.fanpost.support.FanPostReader;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.support.MemberInputSupport;
import com.example.infinite.domain.member.member.support.MemberReader;
import com.example.infinite.domain.subscriptionmembership.dto.response.WriterSubscriptionBadge;
import com.example.infinite.domain.subscriptionmembership.service.SubscriptionMembershipService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private static final int COMMENT_SLICE_SIZE = 20;
    private static final int COMMENT_REPLY_LIMIT = 20;

    private final CommentRepository commentRepository;
    private final CommentReader commentReader;
    private final FanPostReader fanPostReader;
    private final ArtistPostReader artistPostReader;
    private final CommentMentionService commentMentionService;
    private final MemberReader memberReader;
    private final SubscriptionMembershipService subscriptionMembershipService;
    private final FanPostCommentRedissonService fanPostCommentRedissonService;
    private final ArtistPostCommentStreamV2Service artistPostCommentStreamV2Service;
    private final CommentQueryCacheService commentQueryCacheService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CommentResponse createFanPostComment(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long fanPostId,
            CommentCreateRequest request
    ) {
        // FanPost 댓글도 락 안에서 실제 write 트랜잭션이 열리게 해야
        // "락은 풀렸지만 아직 커밋 전" 상태를 다른 요청이 다시 읽는 문제를 막을 수 있다.
        return fanPostCommentRedissonService.create(memberDetails, artistId, fanPostId, request);
    }

    @Transactional
    public CommentResponse createArtistPostComment(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long artistPostId,
            CommentCreateRequest request
    ) {
        // legacy compatibility path.
        // ArtistPost 댓글의 실사용 최신 경로는 queueArtistPostCommentV2이고,
        // 이 동기 경로는 기존 형태를 깨지 않기 위해서만 남겨둔다.
        return createComment(memberDetails, artistId, artistPostId, request, PostType.ARTIST_POST);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ArtistPostCommentQueuedResponse queueArtistPostCommentV2(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long artistPostId,
            CommentCreateRequest request
    ) {
        // ArtistPost 댓글의 실제 사용 경로.
        // worker가 DB 반영을 맡는 비동기 계약이며, 최신 클라이언트는 이 경로만 사용한다.
        return artistPostCommentStreamV2Service.queueCreate(memberDetails, artistId, artistPostId, request);
    }

    public CursorSliceResponse<CommentResponse> getFanPostComments(Long artistId, Long fanPostId, Long cursor) {
        // 팬포스트 상세 댓글 조회는 공통 slice 로직을 FAN_POST 타입으로 실행한다.
        return getComments(PostType.FAN_POST, artistId, fanPostId, cursor);
    }

    public CursorSliceResponse<CommentResponse> getArtistPostComments(Long artistId, Long artistPostId, Long cursor) {
        // 아티스트 게시글도 댓글 조회 규칙은 동일하므로 targetType만 바꿔 재사용한다.
        return getComments(PostType.ARTIST_POST, artistId, artistPostId, cursor);
    }

    public CursorSliceResponse<CommentResponse> getFanPostReplies(Long artistId, Long fanPostId, Long parentCommentId, Long cursor) {
        validateCommentTarget(PostType.FAN_POST, artistId, fanPostId);
        return getReplies(PostType.FAN_POST, artistId, fanPostId, parentCommentId, cursor);
    }

    public CursorSliceResponse<CommentResponse> getArtistPostReplies(Long artistId, Long artistPostId, Long parentCommentId, Long cursor) {
        validateCommentTarget(PostType.ARTIST_POST, artistId, artistPostId);
        return getReplies(PostType.ARTIST_POST, artistId, artistPostId, parentCommentId, cursor);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void deleteFanPostComment(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long fanPostId,
            Long commentId
    ) {
        fanPostCommentRedissonService.delete(memberDetails, artistId, fanPostId, commentId);
    }

    @Transactional
    public void deleteArtistPostComment(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long artistPostId,
            Long commentId
    ) {
        // legacy compatibility path.
        // ArtistPost 댓글 삭제의 실사용 최신 경로는 queueDeleteArtistPostCommentV2다.
        deleteComment(memberDetails, artistId, artistPostId, commentId, PostType.ARTIST_POST);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ArtistPostCommentQueuedResponse queueDeleteArtistPostCommentV2(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long artistPostId,
            Long commentId
    ) {
        // ArtistPost 댓글 삭제의 실제 사용 경로.
        // v2에서는 stream command 적재까지만 동기 처리한다.
        return artistPostCommentStreamV2Service.queueDelete(memberDetails, artistId, artistPostId, commentId);
    }

    private CommentResponse createComment(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long targetId,
            CommentCreateRequest request,
            PostType targetType
    ) {
        // 댓글 작성자는 JWT principal(Member) 기준으로 고정하고, 대상 글이 해당 artist 소속인지 먼저 검증한다.
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        validateCommentTarget(targetType, artistId, targetId);

        // 부모 댓글 검증과 멘션 해석은 저장 전에 끝내야 잘못된 스레드 오염을 막을 수 있다.
        Comment parentComment = resolveParentComment(targetType, request.parentId(), targetId);
        String mentionNickname = resolveMentionNickname(targetType, request.content(), targetId, parentComment);

        Comment comment = commentRepository.save(Comment.create(
                targetType,
                targetId,
                member,
                request.content(),
                parentComment
        ));
        // 댓글 생성 시 대상 게시글 카운트를 같은 트랜잭션에서 함께 올린다.
        changeCommentCount(targetType, artistId, targetId, 1);
        CommentMentionResponse mentionedMember = commentMentionService.syncMention(comment, mentionNickname);
        WriterSubscriptionBadge writerBadge = loadSingleWriterBadge(artistId, member.getId());
        publishCommentCacheInvalidation(targetType, targetId, parentComment == null ? null : parentComment.getId());

        return CommentResponse.from(
                comment,
                writerBadge.fanMembershipSubscribed(),
                writerBadge.dmSubscribed(),
                mentionedMember
        );
    }

    private CursorSliceResponse<CommentResponse> getComments(PostType targetType, Long artistId, Long targetId, Long cursor) {
        // root comment는 "댓글 수도 충분히 많고 조회도 반복되는 post"에서만 admission을 통과한다.
        return commentQueryCacheService.getRootSlice(
                targetType,
                artistId,
                targetId,
                cursor,
                () -> loadRootComments(targetType, artistId, targetId, cursor)
        );
    }

    // 대댓글조회
    private CursorSliceResponse<CommentResponse> getReplies(
            PostType targetType,
            Long artistId,
            Long targetId,
            Long parentCommentId,
            Long cursor
    ) {
        Comment parentComment = commentReader.findByIdAndTargetTypeAndTargetIdOrThrow(parentCommentId, targetType, targetId);
        if (!parentComment.isRootComment()) {
            throw new CommentException(CommentErrorCode.INVALID_PARENT_COMMENT);
        }

        long replyCount = commentRepository.countByParentId(parentCommentId);
        // replies는 "개수가 많거나, 조회 heat가 높으면" 캐시 admission을 통과한다.
        return commentQueryCacheService.getReplies(
                targetType,
                artistId,
                targetId,
                parentCommentId,
                cursor,
                replyCount,
                () -> loadReplies(artistId, parentCommentId, cursor)
        );
    }

    private void deleteComment(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long targetId,
            Long commentId,
            PostType targetType
    ) {
        // 삭제 시에도 대상 글 소속을 먼저 확인해 다른 게시글의 commentId를 우회 호출하지 못하게 한다.
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        validateCommentTarget(targetType, artistId, targetId);
        Comment comment = commentReader.findByIdAndTargetTypeAndTargetIdOrThrow(commentId, targetType, targetId);

        if (!comment.isOwnedBy(member.getId())) {
            throw new CommentException(CommentErrorCode.COMMENT_PERMISSION_DENIED);
        }

        if (comment.isDeletedPlaceholder()) {
            return;
        }

        commentMentionService.deleteMention(comment.getId());

        if (comment.isRootComment()) {
            if (!commentRepository.existsByParentId(comment.getId())) {
                // 자식이 없는 부모 댓글은 그대로 soft delete 해 목록에서 숨긴다.
                comment.delete();
            } else {
                // 자식이 있으면 부모 댓글은 placeholder 로 남겨 스레드 문맥을 유지한다.
                comment.markDeletedPlaceholder();
            }
            changeCommentCount(targetType, artistId, targetId, -1);
            publishCommentCacheInvalidation(targetType, targetId, comment.getId());
            return;
        }

        comment.delete();
        changeCommentCount(targetType, artistId, targetId, -1);

        Comment parentComment = comment.getParent();
        if (parentComment != null && parentComment.isDeletedPlaceholder()) {
            if (!commentRepository.existsByParentId(parentComment.getId())) {
                // placeholder 부모에 달린 마지막 자식도 사라지면 부모를 실제 soft delete 로 마무리한다.
                parentComment.delete();
            }
        }
        publishCommentCacheInvalidation(targetType, targetId, parentComment == null ? null : parentComment.getId());
    }

    private Comment resolveParentComment(PostType targetType, Long parentId, Long targetId) {
        if (parentId == null) {
            return null;
        }

        // 부모 댓글은 같은 targetType/targetId 스레드 안에서만 허용해 다른 글 댓글에 대댓글을 다는 것을 막는다.
        Comment parentComment = commentReader.findByIdAndTargetTypeAndTargetIdOrThrow(parentId, targetType, targetId);
        // 부모도 댓글이고 또 그 아래에 달면 3뎁스가 되므로 원댓글만 부모로 허용한다.
        if (!parentComment.isRootComment()) {
            throw new CommentException(CommentErrorCode.COMMENT_DEPTH_EXCEEDED);
        }
        return parentComment;
    }

    private String resolveMentionNickname(
            PostType targetType,
            String content,
            Long targetId,
            Comment parentComment
    ) {
        List<String> mentionedNicknames = MentionParser.extractMentionedNicknames(content);
        if (mentionedNicknames.isEmpty()) {
            return null;
        }

        if (parentComment == null) {
            // 멘션은 3뎁스 대체 규칙상 대댓글에서만 해석하고, 원댓글의 @텍스트는 일반 본문으로 둔다.
            return null;
        }

        // 허용 멘션 범위는 "같은 루트 댓글 스레드 참여자"로 제한해 3-depth 대체 규칙을 유지한다.
        List<Comment> threadComments = commentRepository.findThreadCommentsByRootCommentId(
                targetType,
                targetId,
                parentComment.getId()
        );
        Set<String> allowedNicknames = threadComments.stream()
                .map(comment -> comment.getWriter().getNickname())
                .map(nickname -> nickname.strip().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 대댓글 멘션은 최대 1명만 허용하므로, 스레드 참여자 범위 안의 첫 번째 닉네임만 멘션으로 인정한다.
        return mentionedNicknames.stream()
                .filter(allowedNicknames::contains)
                .findFirst()
                .orElse(null);
    }

    private void validateCommentTarget(PostType targetType, Long artistId, Long targetId) {
        // 댓글 대상 검증은 targetType별 reader로 위임해 fan-post / artist-post 분기를 한곳에 모은다.
        switch (targetType) {
            case FAN_POST -> fanPostReader.findByIdAndArtistIdOrThrow(targetId, artistId);
            case ARTIST_POST -> artistPostReader.findByIdAndArtistIdOrThrow(targetId, artistId);
            default -> throw new IllegalArgumentException("지원하지 않는 댓글 대상 타입입니다: " + targetType);
        }
    }

    private void changeCommentCount(PostType targetType, Long artistId, Long targetId, int delta) {
        // 댓글 수 비정규화 컬럼도 targetType별 엔티티에 맞춰 같은 트랜잭션 안에서 함께 반영한다.
        switch (targetType) {
            case FAN_POST -> {
                FanPost fanPost = fanPostReader.findByIdAndArtistIdOrThrow(targetId, artistId);
                fanPost.changeCommentCountBy(delta);
            }
            case ARTIST_POST -> {
                ArtistPost artistPost = artistPostReader.findByIdAndArtistIdOrThrow(targetId, artistId);
                artistPost.changeCommentCountBy(delta);
            }
            default -> throw new IllegalArgumentException("지원하지 않는 댓글 대상 타입입니다: " + targetType);
        }
    }

    private CursorSliceResponse<CommentResponse> loadRootComments(
            PostType targetType,
            Long artistId,
            Long targetId,
            Long cursor
    ) {
        // 댓글 기본 조회는 부모 댓글 slice만 내리고, 대댓글은 별도 endpoint에서 온디맨드로 조회한다.
        List<Comment> rootComments = commentRepository.findRootSliceByTargetTypeAndTargetId(
                targetType,
                targetId,
                cursor,
                COMMENT_SLICE_SIZE + 1
        );
        boolean hasNext = rootComments.size() > COMMENT_SLICE_SIZE;
        List<Comment> visibleRootComments = hasNext ? rootComments.subList(0, COMMENT_SLICE_SIZE) : rootComments;

        List<Long> rootCommentIds = visibleRootComments.stream()
                .map(Comment::getId)
                .toList();
        Map<Long, Integer> replyCountsByParentId = commentRepository.findReplyCountsByParentIds(rootCommentIds);
        Map<Long, WriterSubscriptionBadge> badgeByWriterId = loadWriterBadges(
                artistId,
                visibleRootComments.stream().map(comment -> comment.getWriter().getId()).toList()
        );

        List<CommentResponse> content = visibleRootComments.stream()
                .map(rootComment -> {
                    WriterSubscriptionBadge writerBadge = badgeByWriterId.getOrDefault(
                            rootComment.getWriter().getId(),
                            WriterSubscriptionBadge.empty(rootComment.getWriter().getId())
                    );
                    return CommentResponse.from(
                            rootComment,
                            writerBadge.fanMembershipSubscribed(),
                            writerBadge.dmSubscribed(),
                            replyCountsByParentId.getOrDefault(rootComment.getId(), 0),
                            null
                    );
                })
                .toList();

        Long nextCursor = hasNext && !visibleRootComments.isEmpty()
                ? visibleRootComments.get(visibleRootComments.size() - 1).getId()
                : null;

        return new CursorSliceResponse<>(content, nextCursor, hasNext, COMMENT_SLICE_SIZE);
    }

    private CursorSliceResponse<CommentResponse> loadReplies(Long artistId, Long parentCommentId, Long cursor) {
        List<Comment> replies = commentRepository.findRepliesByParentId(parentCommentId, cursor, COMMENT_REPLY_LIMIT + 1);
        Map<Long, CommentMentionResponse> mentionByCommentId = loadMentionByCommentId(replies);
        Map<Long, WriterSubscriptionBadge> badgeByWriterId = loadWriterBadges(
                artistId,
                replies.stream().map(reply -> reply.getWriter().getId()).toList()
        );
        List<CommentResponse> rows = replies.stream()
                .map(reply -> {
                    WriterSubscriptionBadge writerBadge = badgeByWriterId.getOrDefault(
                            reply.getWriter().getId(),
                            WriterSubscriptionBadge.empty(reply.getWriter().getId())
                    );
                    return CommentResponse.from(
                            reply,
                            writerBadge.fanMembershipSubscribed(),
                            writerBadge.dmSubscribed(),
                            mentionByCommentId.get(reply.getId())
                    );
                })
                .toList();
        return CursorSliceResponse.of(rows, COMMENT_REPLY_LIMIT, CommentResponse::commentId);
    }

    private Map<Long, CommentMentionResponse> loadMentionByCommentId(List<Comment> comments) {
        List<Long> commentIds = comments.stream()
                .map(Comment::getId)
                .toList();
        return commentMentionService.findMentionResponseByCommentIds(commentIds);
    }

    private Map<Long, WriterSubscriptionBadge> loadWriterBadges(Long artistId, List<Long> writerIds) {
        if (writerIds.isEmpty()) {
            return Map.of();
        }
        return subscriptionMembershipService.getWriterBadges(artistId, writerIds);
    }

    private WriterSubscriptionBadge loadSingleWriterBadge(Long artistId, Long writerId) {
        return subscriptionMembershipService.getWriterBadges(artistId, List.of(writerId))
                .getOrDefault(writerId, WriterSubscriptionBadge.empty(writerId));
    }

    private void publishCommentCacheInvalidation(PostType targetType, Long targetId, Long rootCommentId) {
        applicationEventPublisher.publishEvent(new CommentCacheInvalidationEvent(targetType, targetId, rootCommentId));
    }
}
