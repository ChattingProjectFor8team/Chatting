package com.example.infinite.domain.artistcontent.follow.service;

import com.example.infinite.domain.artistcontent.follow.dto.response.FollowResponse;
import com.example.infinite.domain.artistcontent.follow.entity.Follow;
import com.example.infinite.domain.artistcontent.follow.repository.FollowRepository;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import com.example.infinite.domain.member.artist.entity.ArtistMember;
import com.example.infinite.domain.member.artist.repository.ArtistMemberRepository;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.support.MemberInputSupport;
import com.example.infinite.domain.member.member.support.MemberReader;
import com.example.infinite.global.auth.MemberDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
// 이번 과제의 follow 범위는 "Member -> ArtistMember" 단방향 최소 기능이다.
// 일반 SNS처럼 상대 목록/추천/팔로워 수 집계까지 넓히지 않고, 토글과 홈 피드 조립에 필요한 조회만 둔다.
public class FollowService {

    private final FollowRepository followRepository;
    private final ArtistMemberRepository artistMemberRepository;
    private final MemberReader memberReader;

    @Transactional
    public FollowResponse toggleArtistMemberFollow(MemberDetailsImpl memberDetails, Long artistMemberId) {
        Member follower = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        ArtistMember targetArtistMember = artistMemberRepository.findById(artistMemberId)
                .orElseThrow(() -> new ArtistContentException(ArtistContentErrorCode.FOLLOW_TARGET_NOT_FOUND));

        // 일반 멤버/아티스트 여부와 무관하게 "자기 자신 follow"만 막으면 정책이 단순해진다.
        if (targetArtistMember.getMember().getId().equals(follower.getId())) {
            throw new ArtistContentException(ArtistContentErrorCode.FOLLOW_SELF_NOT_ALLOWED);
        }

        return followRepository.findByFollowerMemberIdAndTargetArtistMemberId(follower.getId(), artistMemberId)
                .map(existingFollow -> {
                    // soft delete 대신 실제 row 삭제로 처리해 같은 대상을 다시 follow할 때 unique 제약과 충돌하지 않게 한다.
                    followRepository.delete(existingFollow);
                    return FollowResponse.of(artistMemberId, false);
                })
                .orElseGet(() -> {
                    try {
                        // 같은 사용자가 같은 대상을 거의 동시에 follow 하면 둘 다 "미존재"를 보고 insert를 시도할 수 있다.
                        // 이 경우 unique 제약이 최종 방어선이 되므로, saveAndFlush로 여기서 바로 감지하고 현재 상태를 다시 해석한다.
                        followRepository.saveAndFlush(Follow.create(follower, targetArtistMember));
                        return FollowResponse.of(artistMemberId, true);
                    } catch (DataIntegrityViolationException exception) {
                        boolean followed = followRepository.findByFollowerMemberIdAndTargetArtistMemberId(
                                follower.getId(),
                                artistMemberId
                        ).isPresent();
                        if (followed) {
                            return FollowResponse.of(artistMemberId, true);
                        }
                        throw exception;
                    }
                });
    }

    public List<ArtistMember> getFollowedArtistMembers(Long followerMemberId) {
        // 홈 대시보드는 follow 엔티티를 직접 다루지 않고 아티스트 멤버 목록만 받는다.
        return followRepository.findAllByFollowerMemberIdOrderByIdDesc(followerMemberId).stream()
                .map(Follow::getTargetArtistMember)
                .toList();
    }
}
