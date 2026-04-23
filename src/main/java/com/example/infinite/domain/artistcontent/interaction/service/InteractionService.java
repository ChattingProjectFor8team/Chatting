package com.example.infinite.domain.artistcontent.interaction.service;

import com.example.infinite.domain.artistcontent.comment.entity.Comment;
import com.example.infinite.domain.artistcontent.comment.support.CommentReader;
import com.example.infinite.domain.artistcontent.interaction.dto.response.ArtistPostLikeQueuedResponse;
import com.example.infinite.domain.artistcontent.interaction.dto.response.InteractionResponse;
import com.example.infinite.domain.artistcontent.interaction.entity.Reaction;
import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.interaction.repository.InteractionRepository;
import com.example.infinite.domain.artistcontent.interaction.service.commentlike.CommentLikeRedissonService;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.artistcontent.interaction.service.fanletterlike.FanLetterLikeRedissonService;
import com.example.infinite.domain.artistcontent.interaction.service.fanpostlike.FanPostLikeRedissonService;
import com.example.infinite.domain.artistcontent.interaction.service.artistpostlike.ArtistPostLikeRedissonV2Service;
import com.example.infinite.domain.artistcontent.interaction.service.artistpostlike.stream.ArtistPostLikeStreamV3Service;
import com.example.infinite.domain.artistcontent.post.artistpost.support.ArtistPostReader;
import com.example.infinite.domain.artistcontent.post.fanletter.entity.FanLetter;
import com.example.infinite.domain.artistcontent.post.fanletter.support.FanLetterReader;
import com.example.infinite.domain.artistcontent.post.fanpost.entity.FanPost;
import com.example.infinite.domain.artistcontent.post.fanpost.support.FanPostReader;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.support.MemberInputSupport;
import com.example.infinite.domain.member.member.support.MemberReader;
import com.example.infinite.global.auth.MemberDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InteractionService {

    private final InteractionRepository interactionRepository;
    private final FanPostReader fanPostReader;
    private final ArtistPostReader artistPostReader;
    private final FanLetterReader fanLetterReader;
    private final CommentReader commentReader;
    private final MemberReader memberReader;
    private final FanPostLikeRedissonService fanPostLikeRedissonService;
    private final FanLetterLikeRedissonService fanLetterLikeRedissonService;
    private final ArtistPostLikeRedissonV2Service artistPostLikeRedissonV2Service;
    private final ArtistPostLikeStreamV3Service artistPostLikeStreamV3Service;
    private final CommentLikeRedissonService commentLikeRedissonService;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public InteractionResponse toggleFanPostLike(MemberDetailsImpl memberDetails, Long artistId, Long fanPostId) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        // 여기서 outer transaction을 열어두면 "락 해제 후 커밋" 순서가 되어
        // 같은 member-target 요청이 미커밋 상태를 다시 읽는 레이스가 생길 수 있다.
        // 그래서 진입점은 비트랜잭션으로 두고, 락 안쪽 core service가 실제 write 트랜잭션을 시작한다.
        return fanPostLikeRedissonService.toggle(member.getId(), artistId, fanPostId);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public InteractionResponse toggleArtistPostLike(MemberDetailsImpl memberDetails, Long artistId, Long artistPostId) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        // ArtistPost 좋아요는 다른 좋아요와 달리 고트래픽 후보라서 별도 서비스로 분리했다.
        // 실제 운영 경로는 Redisson 기반 V2를 사용하고, Lettuce V1은 비교/과제 설명용 구현으로만 남긴다.
        //
        // 이렇게 분리한 이유:
        // 1) InteractionService 안에서 락, 토글, delta 집계까지 모두 처리하면 책임이 너무 커진다.
        // 2) Redisson AOP 락은 별도 빈을 통해 호출해야 정상 적용된다(self-invocation 회피).
        // 3) 나중에 V1/Lettuce와 V2/Redisson을 비교 설명하기도 쉬워진다.
        return artistPostLikeRedissonV2Service.toggle(member.getId(), artistId, artistPostId);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ArtistPostLikeQueuedResponse queueArtistPostLikeV3(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long artistPostId
    ) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        // V3는 stream 기반 비동기 처리 진입점이다. 실사용 동기 경로(V2)와 계약이 다르므로 메서드를 분리한다.
        return artistPostLikeStreamV3Service.queue(member.getId(), artistId, artistPostId);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public InteractionResponse toggleFanLetterLike(MemberDetailsImpl memberDetails, Long artistId, Long fanLetterId) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        return fanLetterLikeRedissonService.toggle(member.getId(), artistId, fanLetterId);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public InteractionResponse toggleFanPostCommentLike(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long fanPostId,
            Long commentId
    ) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        // 댓글 좋아요도 이제 member + comment 단위 Redisson 락 안에서 처리한다.
        // 그렇지 않으면 같은 댓글에 대한 연타나 동시 요청에서 Reaction/likeCount 정합성이 깨질 수 있다.
        return commentLikeRedissonService.toggle(
                member.getId(),
                artistId,
                fanPostId,
                commentId,
                PostType.FAN_POST
        );
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public InteractionResponse toggleArtistPostCommentLike(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long artistPostId,
            Long commentId
    ) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        return commentLikeRedissonService.toggle(
                member.getId(),
                artistId,
                artistPostId,
                commentId,
                PostType.ARTIST_POST
        );
    }
}
