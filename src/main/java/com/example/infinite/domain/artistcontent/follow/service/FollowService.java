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
                        /*
                         * 여기서도 "조회 후 insert" 사이의 경쟁 조건을 의식해야 한다.
                         *
                         * 두 요청이 거의 동시에 들어오면 둘 다 기존 row 가 없다고 판단하고
                         * insert 를 시도할 수 있다.
                         * 애플리케이션 코드만으로는 이 틈을 완전히 없애기 어렵기 때문에
                         * 최종 정합성은 DB unique 제약에 맡긴다.
                         *
                         * saveAndFlush 를 쓰는 이유는
                         * commit 시점까지 예외를 미루지 않고 지금 여기서 충돌을 감지해
                         * "결과적으로는 follow 상태가 true 인가?"를 바로 다시 해석하기 위해서다.
                         */
                        followRepository.saveAndFlush(Follow.create(follower, targetArtistMember));
                        return FollowResponse.of(artistMemberId, true);
                    } catch (DataIntegrityViolationException exception) {
                        /*
                         * unique 충돌이 났다는 것은 대개 "다른 동시 요청이 먼저 insert 했다"는 뜻이다.
                         * 이 경우 사용자 입장에서는 이미 follow 된 상태가 최종 결과이므로
                         * 예외를 그대로 500 으로 올리기보다 현재 상태를 다시 읽어 true 로 돌려준다.
                         *
                         * 반대로 재조회해도 row 가 없다면 예상한 경쟁 상황이 아니므로
                         * 다른 무결성 오류로 보고 원래 예외를 다시 던진다.
                         */
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
        /*
         * 홈 대시보드가 필요한 것은 "follow row 자체"가 아니라
         * 그 row 가 가리키는 ArtistMember 들이다.
         * 그래서 서비스 경계에서 Follow 엔티티를 한 번 벗겨
         * 상위 계층은 ArtistMember 목록만 다루게 만든다.
         */
        return followRepository.findAllByFollowerMemberIdOrderByIdDesc(followerMemberId).stream()
                .map(Follow::getTargetArtistMember)
                .toList();
    }
}
