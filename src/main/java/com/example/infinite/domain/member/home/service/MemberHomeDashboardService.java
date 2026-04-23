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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
// 메인 홈은 "서비스 전체 탐색 허브"라서 여러 도메인의 가벼운 요약만 한 번에 모은다.
public class MemberHomeDashboardService {

    private static final int POPULAR_KEYWORD_LIMIT = 10;
    private static final int SUBSCRIBED_ARTIST_POST_LIMIT = 2;
    private static final int FOLLOWED_ARTIST_MEMBER_POST_LIMIT = 6;

    private final MemberReader memberReader;
    private final ArtistRepository artistRepository;
    private final ArtistSearchKeywordService artistSearchKeywordService;
    private final FanMembershipRepository fanMembershipRepository;
    private final FollowService followService;
    private final ArtistPostService artistPostService;

    public MemberHomeDashboardResponse getDashboard(MemberDetailsImpl memberDetails) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));

        return new MemberHomeDashboardResponse(
                artistSearchKeywordService.getPopularKeywords(POPULAR_KEYWORD_LIMIT),
                buildSubscribedArtistsLatestPosts(member.getId()),
                buildFollowedArtistMembersLatestPosts(member.getId())
        );
    }

    private List<SubscribedArtistLatestPostsResponse> buildSubscribedArtistsLatestPosts(Long memberId) {
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

        return artistIds.stream()
                .map(artistId -> {
                    Artist artist = artistById.get(artistId);
                    if (artist == null) {
                        return null;
                    }
                    List<ArtistPostResponse> posts = artistPostService.getLatestArtistPosts(
                            artistId,
                            SUBSCRIBED_ARTIST_POST_LIMIT
                    );
                    if (posts.isEmpty()) {
                        return null;
                    }
                    return new SubscribedArtistLatestPostsResponse(
                            HomeDashboardArtistSummaryResponse.from(artist),
                            posts
                    );
                })
                .filter(response -> response != null)
                .toList();
    }

    private List<FollowedArtistMemberLatestPostResponse> buildFollowedArtistMembersLatestPosts(Long memberId) {
        List<ArtistMember> followedArtistMembers = followService.getFollowedArtistMembers(memberId);
        if (followedArtistMembers.isEmpty()) {
            return List.of();
        }

        // 한 회원은 현재 정책상 한 ArtistMember에만 연결되므로 writerId -> ArtistMember 매핑이 단순하다.
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

        return posts.stream()
                .map(post -> {
                    ArtistMember artistMember = artistMemberByWriterId.get(post.writerId());
                    return artistMember == null ? null : FollowedArtistMemberLatestPostResponse.from(artistMember, post);
                })
                .filter(response -> response != null)
                .toList();
    }
}
