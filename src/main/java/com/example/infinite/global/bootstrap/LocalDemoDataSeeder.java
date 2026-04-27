package com.example.infinite.global.bootstrap;

import com.example.infinite.domain.artistcontent.media.entity.ArtistYoutubeVideo;
import com.example.infinite.domain.artistcontent.media.repository.ArtistYoutubeVideoRepository;
import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.artist.entity.ArtistMember;
import com.example.infinite.domain.member.artist.repository.ArtistMemberRepository;
import com.example.infinite.domain.member.artist.repository.ArtistRepository;
import com.example.infinite.domain.member.artist.service.ArtistSearchKeywordService;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.enums.MemberRole;
import com.example.infinite.domain.member.member.repository.MemberRepository;
import com.example.infinite.domain.realtimelive.entity.RealtimeLive;
import com.example.infinite.domain.realtimelive.enums.LiveStatus;
import com.example.infinite.domain.realtimelive.repository.RealtimeLiveRepository;
import com.example.infinite.domain.subscriptionmembership.entity.FanMembership;
import com.example.infinite.domain.subscriptionmembership.enums.SubscriptionStatus;
import com.example.infinite.domain.subscriptionmembership.repository.FanMembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 로컬 프론트 확인용 더미 데이터 시드.
 *
 * 목표:
 * - 아티스트 검색 결과가 비지 않게 기본 아티스트를 채운다.
 * - YouTube 탭에서 바로 보이도록 아티스트당 6개 카드를 넣는다.
 * - LIVE Replay 탭에서 보이도록 REPLAY_READY VOD를 아티스트당 3개 넣는다.
 * - 로그인 테스트를 위해 팬 계정 1개와 아티스트 멤버 계정을 같이 만든다.
 *
 * 운영/배포 환경에서는 절대 돌지 않도록 local profile + property 두 겹으로 막는다.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "demo-data", havingValue = "true", matchIfMissing = true)
public class LocalDemoDataSeeder implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "demo1234!";

    private static final List<ArtistSeed> ARTISTS = List.of(
            new ArtistSeed("bts", "BTS", "Jin", "Global pop group demo artist"),
            new ArtistSeed("blackpink", "BLACKPINK", "Jennie", "Performance-focused demo artist"),
            new ArtistSeed("newjeans", "NewJeans", "Minji", "Fast-moving fandom demo artist"),
            new ArtistSeed("ive", "IVE", "Wonyoung", "Bright concept demo artist"),
            new ArtistSeed("aespa", "aespa", "Karina", "Digital concept demo artist"),
            new ArtistSeed("seventeen", "SEVENTEEN", "S.Coups", "Large member-count demo artist")
    );

    private static final List<YoutubeSeed> YOUTUBE_SEEDS = List.of(
            new YoutubeSeed("dQw4w9WgXcQ", "Performance Clip 01", 213),
            new YoutubeSeed("3JZ_D3ELwOQ", "Performance Clip 02", 246),
            new YoutubeSeed("kJQP7kiw5Fk", "Performance Clip 03", 281),
            new YoutubeSeed("fLexgOxsZu0", "Performance Clip 04", 234),
            new YoutubeSeed("9bZkp7q19f0", "Performance Clip 05", 252),
            new YoutubeSeed("L_jWHffIx5E", "Performance Clip 06", 242)
    );

    private static final List<ReplaySeed> REPLAY_SEEDS = List.of(
            new ReplaySeed(
                    "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    "Replay 01",
                    96,
                    3
            ),
            new ReplaySeed(
                    "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    "Replay 02",
                    74,
                    8
            ),
            new ReplaySeed(
                    "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
                    "Replay 03",
                    58,
                    14
            )
    );

    private final MemberRepository memberRepository;
    private final ArtistRepository artistRepository;
    private final ArtistMemberRepository artistMemberRepository;
    private final ArtistYoutubeVideoRepository artistYoutubeVideoRepository;
    private final RealtimeLiveRepository realtimeLiveRepository;
    private final FanMembershipRepository fanMembershipRepository;
    private final ArtistSearchKeywordService artistSearchKeywordService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Member fanDemoMember = ensureFanDemoMember();
        ensureSuperAdminDemoMember();

        for (int artistIndex = 0; artistIndex < ARTISTS.size(); artistIndex++) {
            ArtistSeed artistSeed = ARTISTS.get(artistIndex);
            Member member = ensureArtistMemberUser(artistSeed, artistIndex);
            Artist artist = ensureArtist(artistSeed, artistIndex);
            ArtistMember artistMember = ensureArtistMember(artist, member, artistSeed, artistIndex);

            seedYoutubeVideos(artist, artistMember, artistIndex);
            seedReplayVod(artist, artistMember, artistIndex);
            seedPopularKeyword(artistSeed, artistIndex);
            seedFanDemoMembership(fanDemoMember, artist);
        }

        log.info("로컬 더미데이터 시드 점검 완료: artists={}, youtubePerArtist=6, vodPerArtist=3",
                ARTISTS.size());
    }

    private Member ensureFanDemoMember() {
        String email = "fan.demo@infinite.local";
        return memberRepository.findByEmail(email)
                .orElseGet(() -> {
                    Member fan = Member.createNewMember(
                            email,
                            passwordEncoder.encode(DEMO_PASSWORD),
                            "fan_demo",
                            "010-9000-0001"
                    );
                    fan.updateProfile(
                            "fan_demo",
                            "010-9000-0001",
                            "https://picsum.photos/seed/infinite-fan-profile/480/480",
                            "https://picsum.photos/seed/infinite-fan-cover/1440/720"
                    );
                    return memberRepository.save(fan);
                });
    }

    private void ensureSuperAdminDemoMember() {
        String email = "admin.demo@infinite.local";
        memberRepository.findByEmail(email)
                .map(existing -> {
                    // 예전에 일반 계정으로 만든 로컬 admin demo도 재시드 시 SUPER_ADMIN으로 교정한다.
                    if (existing.getRole() != MemberRole.SUPER_ADMIN) {
                        existing.changeRole(MemberRole.SUPER_ADMIN);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Member admin = Member.createNewMember(
                            email,
                            passwordEncoder.encode(DEMO_PASSWORD),
                            "super_admin",
                            "010-9000-0002"
                    );
                    admin.changeRole(MemberRole.SUPER_ADMIN);
                    admin.updateProfile(
                            "super_admin",
                            "010-9000-0002",
                            "https://picsum.photos/seed/infinite-super-admin-profile/480/480",
                            "https://picsum.photos/seed/infinite-super-admin-cover/1440/720"
                    );
                    return memberRepository.save(admin);
                });
    }

    private Member ensureArtistMemberUser(ArtistSeed seed, int index) {
        String email = seed.slug() + ".host@infinite.local";
        return memberRepository.findByEmail(email)
                .map(existing -> {
                    // 예전에 MEMBER 권한으로 생성된 로컬 더미 계정도 재시드 시 ARTIST로 교정한다.
                    if (existing.getRole() != MemberRole.ARTIST) {
                        existing.changeRole(MemberRole.ARTIST);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Member member = Member.createNewMember(
                            email,
                            passwordEncoder.encode(DEMO_PASSWORD),
                            seed.stageName().toLowerCase().replace(" ", "_"),
                            String.format("010-9100-%04d", index + 1)
                    );
                    member.changeRole(MemberRole.ARTIST);
                    member.updateProfile(
                            seed.stageName().toLowerCase().replace(" ", "_"),
                            String.format("010-9100-%04d", index + 1),
                            artistMemberImage(seed.slug()),
                            artistCoverImage(seed.slug())
                    );
                    return memberRepository.save(member);
                });
    }

    private Artist ensureArtist(ArtistSeed seed, int index) {
        return artistRepository.findBySlug(seed.slug())
                .orElseGet(() -> artistRepository.save(Artist.create(
                        seed.name(),
                        seed.slug(),
                        artistProfileImage(seed.slug()),
                        artistCoverImage(seed.slug()),
                        seed.intro() + " #" + (index + 1)
                )));
    }

    private ArtistMember ensureArtistMember(Artist artist, Member member, ArtistSeed seed, int index) {
        return artistMemberRepository.findByArtistIdAndMemberId(artist.getId(), member.getId())
                .orElseGet(() -> artistMemberRepository.save(ArtistMember.create(
                        artist,
                        member,
                        seed.stageName(),
                        artistMemberImage(seed.slug()),
                        index + 1
                )));
    }

    private void seedYoutubeVideos(Artist artist, ArtistMember artistMember, int artistIndex) {
        if (artistYoutubeVideoRepository.countByArtistId(artist.getId()) >= YOUTUBE_SEEDS.size()) {
            return;
        }

        for (int videoIndex = 0; videoIndex < YOUTUBE_SEEDS.size(); videoIndex++) {
            YoutubeSeed youtubeSeed = YOUTUBE_SEEDS.get(videoIndex);
            if (artistYoutubeVideoRepository.existsByArtistIdAndYoutubeVideoId(artist.getId(), youtubeSeed.videoId())) {
                continue;
            }

            LocalDateTime publishedAt = LocalDateTime.now()
                    .minusDays((long) artistIndex * 5 + videoIndex + 2)
                    .withHour(20)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);

            artistYoutubeVideoRepository.save(ArtistYoutubeVideo.create(
                    artist.getId(),
                    artistMember.getMember().getId(),
                    artistMember.getStageName(),
                    artistMember.getProfileImageUrl(),
                    youtubeSeed.videoId(),
                    "https://www.youtube.com/watch?v=" + youtubeSeed.videoId(),
                    artist.getName() + " " + youtubeSeed.titleSuffix(),
                    "https://i.ytimg.com/vi/" + youtubeSeed.videoId() + "/hqdefault.jpg",
                    youtubeSeed.durationSeconds(),
                    publishedAt
            ));
        }
    }

    private void seedReplayVod(Artist artist, ArtistMember artistMember, int artistIndex) {
        if (realtimeLiveRepository.countByArtistIdAndLiveStatus(artist.getId(), LiveStatus.REPLAY_READY) >= REPLAY_SEEDS.size()) {
            return;
        }

        for (int replayIndex = 0; replayIndex < REPLAY_SEEDS.size(); replayIndex++) {
            ReplaySeed replaySeed = REPLAY_SEEDS.get(replayIndex);

            RealtimeLive live = realtimeLiveRepository.save(RealtimeLive.builder()
                    .artistId(artist.getId())
                    .hostMemberId(artistMember.getMember().getId())
                    .hostDisplayName(artistMember.getStageName())
                    .hostProfileImageUrl(artistMember.getProfileImageUrl())
                    .title(artist.getName() + " LIVE " + replaySeed.titleSuffix())
                    .description(artist.getName() + " replay demo content")
                    .thumbnailUrl(replayThumbnail(artist.getSlug(), replayIndex))
                    .build());

            LocalDateTime startedAt = LocalDateTime.now()
                    .minusDays((long) artistIndex * 4 + replaySeed.publishedDaysAgo())
                    .withHour(20)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);
            LocalDateTime endedAt = startedAt.plusMinutes(replaySeed.durationMinutes());
            LocalDateTime replayPublishedAt = endedAt.plusHours(6);

            live.start();
            live.end();
            live.markReplayReady(replaySeed.replayUrl());

            setField(live, "startedAt", startedAt);
            setField(live, "endedAt", endedAt);
            setField(live, "replayPublishedAt", replayPublishedAt);

            realtimeLiveRepository.save(live);
        }
    }

    private void seedPopularKeyword(ArtistSeed seed, int artistIndex) {
        int weight = 12 - artistIndex;
        for (int i = 0; i < weight; i++) {
            artistSearchKeywordService.recordSearchKeyword(
                    "seed-user-" + seed.slug() + "-" + i,
                    seed.name()
            );
        }
    }

    private void seedFanDemoMembership(Member fanDemoMember, Artist artist) {
        LocalDateTime now = LocalDateTime.now();

        boolean hasActiveMembership = fanMembershipRepository
                .findByUserIdAndStatusAndExpiredAtAfterOrderByIdDesc(
                        fanDemoMember.getId(),
                        SubscriptionStatus.ACTIVE,
                        now
                )
                .stream()
                .anyMatch(membership -> artist.getId().equals(membership.getArtistId()));

        if (hasActiveMembership) {
            return;
        }

        // 로컬 fan.demo 계정은 Fan Letter 시연이 바로 가능해야 해서
        // 모든 데모 아티스트에 대해 유효한 팬 멤버십을 기본 지급한다.
        fanMembershipRepository.save(FanMembership.builder()
                .userId(fanDemoMember.getId())
                .artistId(artist.getId())
                .startedAt(now.minusDays(3))
                .expiredAt(now.plusDays(30))
                .jellyAmount(9)
                .build());
    }

    private static String artistProfileImage(String slug) {
        return "https://picsum.photos/seed/artist-profile-" + slug + "/600/600";
    }

    private static String artistMemberImage(String slug) {
        return "https://picsum.photos/seed/artist-member-" + slug + "/480/480";
    }

    private static String artistCoverImage(String slug) {
        return "https://picsum.photos/seed/artist-cover-" + slug + "/1440/720";
    }

    private static String replayThumbnail(String slug, int index) {
        return "https://picsum.photos/seed/replay-" + slug + "-" + index + "/1280/720";
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("더미데이터 필드 세팅 실패: " + fieldName, e);
        }
    }

    private record ArtistSeed(
            String slug,
            String name,
            String stageName,
            String intro
    ) {
    }

    private record YoutubeSeed(
            String videoId,
            String titleSuffix,
            long durationSeconds
    ) {
    }

    private record ReplaySeed(
            String replayUrl,
            String titleSuffix,
            int durationMinutes,
            int publishedDaysAgo
    ) {
    }
}
