package com.example.infinite.domain.artistcontent.interaction.service;

import com.example.infinite.domain.artistcontent.interaction.dto.response.InteractionResponse;
import com.example.infinite.domain.artistcontent.interaction.entity.Reaction;
import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.interaction.repository.InteractionRepository;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import com.example.infinite.domain.artistcontent.post.artistpost.entity.ArtistPost;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InteractionService {

    private final InteractionRepository interactionRepository;
    private final FanPostReader fanPostReader;
    private final ArtistPostReader artistPostReader;
    private final FanLetterReader fanLetterReader;
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

    @Transactional
    public InteractionResponse toggleArtistPostLike(MemberDetailsImpl memberDetails, Long artistId, Long artistPostId) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        ArtistPost artistPost = artistPostReader.findByIdAndArtistIdOrThrow(artistPostId, artistId);

        return interactionRepository.findByTargetTypeAndTargetIdAndMemberIdAndReactionType(
                        PostType.ARTIST_POST,
                        artistPostId,
                        member.getId(),
                        ReactionType.LIKE
                )
                .map(existingReaction -> cancelLike(existingReaction, artistPost))
                .orElseGet(() -> addLike(member.getId(), artistPost));
    }

    @Transactional
    public InteractionResponse toggleFanLetterLike(MemberDetailsImpl memberDetails, Long artistId, Long fanLetterId) {
        // 팬레터도 fan post 와 같은 reaction 테이블을 재사용한다.
        // 차이는 targetType=FAN_LETTER 로 저장하고, likeCount 반영 대상이 FanLetter 라는 점뿐이다.
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        FanLetter fanLetter = fanLetterReader.findByIdAndArtistIdOrThrow(fanLetterId, artistId);

        return interactionRepository.findByTargetTypeAndTargetIdAndMemberIdAndReactionType(
                        PostType.FAN_LETTER,
                        fanLetterId,
                        member.getId(),
                        ReactionType.LIKE
                )
                .map(existingReaction -> cancelLike(existingReaction, fanLetter))
                .orElseGet(() -> addLike(member.getId(), fanLetter));
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

    private InteractionResponse addLike(Long memberId, ArtistPost artistPost) {
        interactionRepository.save(Reaction.create(
                PostType.ARTIST_POST,
                artistPost.getId(),
                memberId,
                ReactionType.LIKE
        ));
        artistPost.changeLikeCountBy(1);
        return InteractionResponse.of(artistPost.getId(), true, artistPost.getLikeCount());
    }

    private InteractionResponse cancelLike(Reaction existingReaction, ArtistPost artistPost) {
        interactionRepository.delete(existingReaction);
        artistPost.changeLikeCountBy(-1);
        return InteractionResponse.of(artistPost.getId(), false, artistPost.getLikeCount());
    }

    private InteractionResponse addLike(Long memberId, FanLetter fanLetter) {
        // special-like 표시는 별도 플래그를 저장하지 않고,
        // 나중에 "좋아요를 누른 사람이 아티스트 소속 멤버인가"를 조회 단계에서 해석한다.
        interactionRepository.save(Reaction.create(
                PostType.FAN_LETTER,
                fanLetter.getId(),
                memberId,
                ReactionType.LIKE
        ));
        fanLetter.changeLikeCountBy(1);
        return InteractionResponse.of(fanLetter.getId(), true, fanLetter.getLikeCount());
    }

    private InteractionResponse cancelLike(Reaction existingReaction, FanLetter fanLetter) {
        interactionRepository.delete(existingReaction);
        fanLetter.changeLikeCountBy(-1);
        return InteractionResponse.of(fanLetter.getId(), false, fanLetter.getLikeCount());
    }
}
