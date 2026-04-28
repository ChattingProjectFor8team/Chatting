package com.example.infinite.domain.member.home.service;

import com.example.infinite.domain.artistcontent.follow.service.FollowService;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostResponse;
import com.example.infinite.domain.artistcontent.post.artistpost.service.ArtistPostService;
import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.artist.entity.ArtistMember;
import com.example.infinite.domain.member.artist.repository.ArtistRepository;
import com.example.infinite.domain.member.artist.service.ArtistSearchKeywordService;
import com.example.infinite.domain.member.home.dto.response.FollowedArtistMemberLatestPostResponse;
import com.example.infinite.domain.member.home.dto.response.HomeDashboardArtistSummaryResponse;
import com.example.infinite.domain.member.home.dto.response.MemberHomeDashboardResponse;
import com.example.infinite.domain.member.home.dto.response.SubscribedArtistLatestPostsResponse;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.support.MemberInputSupport;
import com.example.infinite.domain.member.member.support.MemberReader;
import com.example.infinite.domain.subscriptionmembership.entity.FanMembership;
import com.example.infinite.domain.subscriptionmembership.enums.SubscriptionStatus;
import com.example.infinite.domain.subscriptionmembership.repository.FanMembershipRepository;
import com.example.infinite.global.auth.MemberDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
// 메인 홈은 "서비스 전체 탐색 허브"라서 여러 도메인의 가벼운 요약만 한 번에 모은다.
public class MemberHomeDashboardService {

    private static final int POPULAR_KEYWORD_FIRST_OFFSET = 0;
    private static final int SUBSCRIBED_ARTIST_POST_LIMIT = 2;
    private static final int FOLLOWED_ARTIST_MEMBER_POST_LIMIT = 6;

    private final MemberReader memberReader;
    private final ArtistRepository artistRepository;
    private final ArtistSearchKeywordService artistSearchKeywordService;
    private final FanMembershipRepository fanMembershipRepository;
    private final FollowService followService;
    private final ArtistPostService artistPostService;

    public MemberHomeDashboardResponse getDashboard(MemberDetailsImpl memberDetails) {
        /*
         * 메인 홈 대시보드는 "하나의 큰 피드"를 그대로 내려주는 API가 아니다.
         * 아래 3개 섹션을 각기 다른 규칙으로 조립해 한 응답에 묶어 주는 성격에 가깝다.
         *
         * 1) 인기 검색어
         * 2) 내가 구독한 아티스트별 최신 글 묶음
         * 3) 내가 follow 한 아티스트 멤버들의 최신 글 묶음
         *
         * 그래서 이 메서드는 단순 repository 1회 조회가 아니라
         * "섹션별 정책에 맞는 서비스 호출"을 오케스트레이션하는 진입점으로 읽는 것이 맞다.
         */
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));

        return new MemberHomeDashboardResponse(
                artistSearchKeywordService.getPopularKeywords(POPULAR_KEYWORD_FIRST_OFFSET).content(),
                buildSubscribedArtistsLatestPosts(member.getId()),
                buildFollowedArtistMembersLatestPosts(member.getId())
        );
    }

    private List<SubscribedArtistLatestPostsResponse> buildSubscribedArtistsLatestPosts(Long memberId) {
        /*
         * 구독 섹션은 "글 최신순 전역 2건"이 아니라
         * "구독한 각 artist마다 최신 n건" 이 필요하다.
         *
         * 즉 기준 축이 writer 가 아니라 artist 다.
         * 그래서 먼저 활성 구독 목록에서 artistId 들을 뽑고,
         * 그 뒤 artist 단위 batch query 로 각 artist의 최신 글 묶음을 조립한다.
         */
        List<FanMembership> activeMemberships = fanMembershipRepository.findByUserIdAndStatusAndExpiredAtAfterOrderByIdDesc(
                memberId,
                SubscriptionStatus.ACTIVE,
                java.time.LocalDateTime.now()
        );
        if (activeMemberships.isEmpty()) {
            return List.of();
        }

        // 상태값만 ACTIVE여도 스케줄러 반영 전이면 만료 구독이 남을 수 있어 expiredAt을 함께 필터링한다.
        // 동일 아티스트를 중복 구독하는 경우는 없지만, 홈 조립 시에는 artistId 순서를 한 번 더 안전하게 정리한다.
        List<Long> artistIds = activeMemberships.stream()
                .map(FanMembership::getArtistId)
                .distinct()
                .toList();
        Map<Long, Artist> artistById = artistRepository.findAllById(artistIds).stream()
                .collect(Collectors.toMap(Artist::getId, Function.identity()));
        Map<Long, List<ArtistPostResponse>> postsByArtistId = artistPostService.getLatestArtistPostsByArtistIds(
                artistIds,
                SUBSCRIBED_ARTIST_POST_LIMIT
        );

        return artistIds.stream()
                .map(artistId -> {
                    Artist artist = artistById.get(artistId);
                    if (artist == null) {
                        return null;
                    }
                    List<ArtistPostResponse> posts = postsByArtistId.getOrDefault(artistId, List.of());
                    if (posts.isEmpty()) {
                        return null;
                    }
                    return new SubscribedArtistLatestPostsResponse(
                            HomeDashboardArtistSummaryResponse.from(artist),
                            posts
                    );
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private List<FollowedArtistMemberLatestPostResponse> buildFollowedArtistMembersLatestPosts(Long memberId) {
        /*
         * follow 섹션은 구독 섹션과 반대로 "artist별 묶음"이 아니라
         * "내가 관심 있는 멤버들이 최근에 쓴 글을 한데 모은 전역 최신 목록" 이다.
         *
         * 그래서 여기서는 artistId 기준 그룹핑을 먼저 하지 않고,
         * follow 중인 ArtistMember -> writerId 집합으로 바꾼 뒤
         * writer 기준 최신 ArtistPost 를 모아 온다.
         */
        List<ArtistMember> followedArtistMembers = followService.getFollowedArtistMembers(memberId);
        if (followedArtistMembers.isEmpty()) {
            return List.of();
        }

        /*
         * 홈 카드 바깥쪽 메타데이터는 Follow 대상인 ArtistMember 에서 가져오고,
         * 실제 글 본문/좋아요 수/작성 시각 등은 ArtistPostResponse 에서 가져온다.
         *
         * 현재 정책상 한 Member 가 동시에 여러 ArtistMember 로 존재하지 않는다고 보고 있으므로
         * writerId -> ArtistMember 매핑을 단순 Map 으로 둘 수 있다.
         * 만약 미래에 한 member 가 여러 artist 소속을 동시에 가질 수 있게 되면
         * 이 매핑 정책은 다시 검토해야 한다.
         */
        Map<Long, ArtistMember> artistMemberByWriterId = new LinkedHashMap<>();
        for (ArtistMember followedArtistMember : followedArtistMembers) {
            artistMemberByWriterId.putIfAbsent(followedArtistMember.getMember().getId(), followedArtistMember);
        }

        List<ArtistPostResponse> posts = artistPostService.getLatestArtistPostsByWriterIds(
                artistMemberByWriterId.keySet(),
                FOLLOWED_ARTIST_MEMBER_POST_LIMIT
        );
        if (posts.isEmpty()) {
            return List.of();
        }

        // 여기서는 "글 1건"을 바로 반환하지 않고,
        // 프론트가 카드에 바로 쓸 수 있게 ArtistMember 메타데이터를 한 번 더 감싸서 내려준다.
        return posts.stream()
                .map(post -> {
                    ArtistMember artistMember = artistMemberByWriterId.get(post.writerId());
                    return artistMember == null ? null : FollowedArtistMemberLatestPostResponse.from(artistMember, post);
                })
                .filter(response -> response != null)
                .toList();
    }
}
