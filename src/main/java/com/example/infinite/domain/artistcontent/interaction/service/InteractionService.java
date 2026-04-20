package com.example.infinite.domain.artistcontent.interaction.service;

import com.example.infinite.domain.artistcontent.interaction.dto.response.InteractionResponse;
import com.example.infinite.domain.artistcontent.interaction.entity.Reaction;
import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.interaction.repository.InteractionRepository;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import com.example.infinite.domain.artistcontent.post.fanpost.entity.FanPost;
import com.example.infinite.domain.artistcontent.post.fanpost.support.FanPostReader;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.support.MemberInputSupport;
import com.example.infinite.domain.member.member.support.MemberReader;
import com.example.infinite.global.auth.MemberDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InteractionService {

    private final InteractionRepository interactionRepository;
    private final FanPostReader fanPostReader;
    private final MemberReader memberReader;

    @Transactional
    public InteractionResponse toggleFanPostLike(MemberDetailsImpl memberDetails, Long artistId, Long fanPostId) {
        // 좋아요 토글은 로그인 principal을 Member로 확정한 뒤 대상 팬포스트를 조회한다.
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        FanPost fanPost = fanPostReader.findByIdAndArtistIdOrThrow(fanPostId, artistId);

        return interactionRepository.findByTargetTypeAndTargetIdAndMemberIdAndReactionType(
                        PostType.FAN_POST,
                        fanPostId,
                        member.getId(),
                        ReactionType.LIKE
                )
                .map(existingReaction -> cancelLike(existingReaction, fanPost))
                .orElseGet(() -> addLike(member.getId(), fanPost));
    }

    private InteractionResponse addLike(Long memberId, FanPost fanPost) {
        // 반응 저장과 비정규화 likeCount 증감을 한 트랜잭션 안에서 같이 처리한다.
        interactionRepository.save(Reaction.create(
                PostType.FAN_POST,
                fanPost.getId(),
                memberId,
                ReactionType.LIKE
        ));
        fanPost.changeLikeCountBy(1);
        return InteractionResponse.of(fanPost.getId(), true, fanPost.getLikeCount());
    }

    private InteractionResponse cancelLike(Reaction existingReaction, FanPost fanPost) {
        // 토글 해제는 기존 반응 삭제 후 likeCount를 내려 조회 응답과 일치시킨다.
        interactionRepository.delete(existingReaction);
        fanPost.changeLikeCountBy(-1);
        return InteractionResponse.of(fanPost.getId(), false, fanPost.getLikeCount());
    }
}
