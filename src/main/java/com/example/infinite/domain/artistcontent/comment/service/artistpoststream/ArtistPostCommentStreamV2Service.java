package com.example.infinite.domain.artistcontent.comment.service.artistpoststream;

import com.example.infinite.domain.artistcontent.comment.dto.request.CommentCreateRequest;
import com.example.infinite.domain.artistcontent.comment.dto.response.ArtistPostCommentQueuedResponse;
import com.example.infinite.domain.artistcontent.comment.entity.Comment;
import com.example.infinite.domain.artistcontent.comment.error.CommentErrorCode;
import com.example.infinite.domain.artistcontent.comment.error.CommentException;
import com.example.infinite.domain.artistcontent.comment.support.CommentReader;
import com.example.infinite.domain.artistcontent.post.artistpost.support.ArtistPostReader;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.support.MemberInputSupport;
import com.example.infinite.domain.member.member.support.MemberReader;
import com.example.infinite.global.auth.MemberDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ArtistPostCommentStreamV2Service {

    private final MemberReader memberReader;
    private final ArtistPostReader artistPostReader;
    private final CommentReader commentReader;
    private final ArtistPostCommentStreamProducer streamProducer;

    /**
     * 댓글 생성 요청을 stream에 적재한다.
     *
     * producer 단계에서는 "완전히 틀린 요청"만 빠르게 걸러내고,
     * 최종 상태 검증과 실제 DB 쓰기는 consumer가 담당한다.
     */
    public ArtistPostCommentQueuedResponse queueCreate(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long artistPostId,
            CommentCreateRequest request
    ) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        artistPostReader.findByIdAndArtistIdOrThrow(artistPostId, artistId);

        ArtistPostCommentCommandType commandType = request.parentId() == null
                ? ArtistPostCommentCommandType.CREATE_ROOT
                : ArtistPostCommentCommandType.CREATE_REPLY;

        if (request.parentId() != null) {
            Comment parentComment = commentReader.findByIdAndTargetTypeAndTargetIdOrThrow(
                    request.parentId(),
                    PostType.ARTIST_POST,
                    artistPostId
            );
            if (!parentComment.isRootComment()) {
                throw new CommentException(CommentErrorCode.COMMENT_DEPTH_EXCEEDED);
            }
        }

        String requestId = UUID.randomUUID().toString();
        streamProducer.enqueue(ArtistPostCommentStreamCommand.create(
                requestId,
                commandType,
                artistId,
                artistPostId,
                member.getId(),
                request.parentId(),
                null,
                request.content()
        ));

        return new ArtistPostCommentQueuedResponse(
                requestId,
                commandType.name(),
                artistPostId,
                request.parentId(),
                null,
                LocalDateTime.now()
        );
    }

    /**
     * 삭제도 즉시 DB에서 지우지 않고 delete command를 stream에 적재한다.
     * 응답은 202 Accepted이며, 실제 삭제/placeholder 전환은 worker가 처리한다.
     */
    public ArtistPostCommentQueuedResponse queueDelete(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long artistPostId,
            Long commentId
    ) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        artistPostReader.findByIdAndArtistIdOrThrow(artistPostId, artistId);
        Comment comment = commentReader.findByIdAndTargetTypeAndTargetIdOrThrow(commentId, PostType.ARTIST_POST, artistPostId);
        if (!comment.isOwnedBy(member.getId())) {
            throw new CommentException(CommentErrorCode.COMMENT_PERMISSION_DENIED);
        }

        String requestId = UUID.randomUUID().toString();
        streamProducer.enqueue(ArtistPostCommentStreamCommand.create(
                requestId,
                ArtistPostCommentCommandType.DELETE,
                artistId,
                artistPostId,
                member.getId(),
                null,
                commentId,
                null
        ));

        return new ArtistPostCommentQueuedResponse(
                requestId,
                ArtistPostCommentCommandType.DELETE.name(),
                artistPostId,
                null,
                commentId,
                LocalDateTime.now()
        );
    }
}
