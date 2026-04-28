package com.example.infinite.global.bootstrap;

import com.example.infinite.domain.artistcontent.comment.entity.Comment;
import com.example.infinite.domain.artistcontent.comment.repository.CommentRepository;
import com.example.infinite.domain.artistcontent.follow.entity.Follow;
import com.example.infinite.domain.artistcontent.follow.repository.FollowRepository;
import com.example.infinite.domain.artistcontent.interaction.entity.Reaction;
import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.interaction.repository.InteractionRepository;
import com.example.infinite.domain.artistcontent.media.entity.Media;
import com.example.infinite.domain.artistcontent.media.enums.MediaType;
import com.example.infinite.domain.artistcontent.media.repository.MediaRepository;
import com.example.infinite.domain.artistcontent.post.artistpost.entity.ArtistPost;
import com.example.infinite.domain.artistcontent.post.artistpost.repository.ArtistPostRepository;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.artistcontent.post.fanletter.entity.FanLetter;
import com.example.infinite.domain.artistcontent.post.fanletter.enums.FanLetterRecipientType;
import com.example.infinite.domain.artistcontent.post.fanletter.repository.FanLetterRepository;
import com.example.infinite.domain.artistcontent.post.fanpost.entity.FanPost;
import com.example.infinite.domain.artistcontent.post.fanpost.repository.FanPostRepository;
import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.artist.entity.ArtistMember;
import com.example.infinite.domain.member.artist.repository.ArtistMemberRepository;
import com.example.infinite.domain.member.artist.repository.ArtistRepository;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.enums.MemberRole;
import com.example.infinite.domain.member.member.enums.MemberStatus;
import com.example.infinite.domain.member.member.repository.MemberRepository;
import com.example.infinite.domain.subscriptionmembership.entity.DmSubscription;
import com.example.infinite.domain.subscriptionmembership.entity.FanMembership;
import com.example.infinite.domain.subscriptionmembership.enums.SubscriptionStatus;
import com.example.infinite.domain.subscriptionmembership.repository.DmSubscriptionRepository;
import com.example.infinite.domain.subscriptionmembership.repository.FanMembershipRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "showcase-data", havingValue = "true")
public class DeployShowcaseDataSeeder implements ApplicationRunner {

    private static final String LOCK_NAME = "deploy-showcase-data-seed-v1";

    private static final List<FanSeed> FAN_SEEDS = List.of(
            new FanSeed("fan.01@connectfin.demo", "starlit_one", "010-9700-0001"),
            new FanSeed("fan.02@connectfin.demo", "midnight_loop", "010-9700-0002"),
            new FanSeed("fan.03@connectfin.demo", "violet_signal", "010-9700-0003"),
            new FanSeed("fan.04@connectfin.demo", "cloudberry", "010-9700-0004"),
            new FanSeed("fan.05@connectfin.demo", "aurora_note", "010-9700-0005"),
            new FanSeed("fan.06@connectfin.demo", "silverframe", "010-9700-0006"),
            new FanSeed("fan.07@connectfin.demo", "neonharbor", "010-9700-0007"),
            new FanSeed("fan.08@connectfin.demo", "peachsignal", "010-9700-0008"),
            new FanSeed("fan.09@connectfin.demo", "moonlitpage", "010-9700-0009"),
            new FanSeed("fan.10@connectfin.demo", "afterglowfan", "010-9700-0010")
    );

    private static final List<String> ARTIST_POST_TEMPLATES = List.of(
            "%s입니다. 오늘 리허설 막 끝났어요. 무대 준비 잘해서 곧 만날게요.",
            "새 콘텐츠 준비하면서 팬분들 반응 다 보고 있어요. %s 응원 덕분에 더 힘나요.",
            "녹음본 마지막 체크 중입니다. 오늘은 특히 %s 파트가 마음에 들어요.",
            "%s 저녁에 사진이랑 짧은 비하인드도 더 올릴게요. 오늘도 잘 부탁해요."
    );

    private static final List<String> FAN_POST_TEMPLATES = List.of(
            "%s 사진 뜬 거 보고 하루 종일 이야기 중이에요. 오늘 비주얼 진짜 최고예요.",
            "%s 라이브 공지 확인했어요. 본방 사수하려고 일정 다 비워뒀습니다.",
            "%s 파트 계속 반복 재생 중인데 들을수록 더 좋아져요.",
            "%s 팬미팅 후기 정리하다가 또 감동받았어요. 현장 분위기 최고였습니다.",
            "%s 굿즈 배송 왔는데 퀄리티 너무 좋아서 바로 글 남겨요.",
            "%s 오늘도 응원합니다. 다들 같이 스트리밍 달려요."
    );

    private static final List<String> ARTIST_POST_COMMENT_TEMPLATES = List.of(
            "오늘 글 올려줘서 고마워요. %s 일정 기대하면서 기다릴게요.",
            "비하인드 더 풀어주시면 너무 좋을 것 같아요. 항상 응원합니다."
    );

    private static final List<String> FAN_POST_COMMENT_TEMPLATES = List.of(
            "저도 같은 생각이에요. 이번 활동 진짜 반응 좋을 것 같아요.",
            "정보 감사합니다. 같이 달리면 더 재밌을 것 같네요."
    );

    private static final List<String> REPLY_TEMPLATES = List.of(
            "맞아요. 저도 그 장면에서 바로 저장했어요.",
            "같이 기다려봐요. 오늘도 좋은 하루 보내세요."
    );

    private static final List<ArtistSeed> ARTIST_SEEDS = List.of(
            new ArtistSeed(
                    1L,
                    "LUMEN8",
                    "lumen8",
                    "LUMEN8 공식 커뮤니티입니다. 팬포스트, 아티스트포스트, 팬레터, 댓글 동선을 한 번에 시연하기 위한 데이터입니다.",
                    List.of(
                            new ArtistMemberSeed("YUNA", "artist.lumen8.yuna@connectfin.demo", "lumen8_yuna", "010-9600-0001"),
                            new ArtistMemberSeed("RIN", "artist.lumen8.rin@connectfin.demo", "lumen8_rin", "010-9600-0002"),
                            new ArtistMemberSeed("SORA", "artist.lumen8.sora@connectfin.demo", "lumen8_sora", "010-9600-0003")
                    )
            ),
            new ArtistSeed(
                    2L,
                    "Kagero",
                    "kagero",
                    "Kagero 공식 커뮤니티입니다. mock 프론트 artistId 2와 맞춰 시연 가능한 데이터를 제공합니다.",
                    List.of(
                            new ArtistMemberSeed("REI", "artist.kagero.rei@connectfin.demo", "kagero_rei", "010-9600-0004"),
                            new ArtistMemberSeed("REN", "artist.kagero.ren@connectfin.demo", "kagero_ren", "010-9600-0005")
                    )
            ),
            new ArtistSeed(
                    3L,
                    "NOIR7",
                    "noir7",
                    "NOIR7 공식 커뮤니티입니다. 여러 멤버 계정과 게시물을 함께 시연할 수 있도록 구성된 데이터입니다.",
                    List.of(
                            new ArtistMemberSeed("KAI", "artist.noir7.kai@connectfin.demo", "noir7_kai", "010-9600-0006"),
                            new ArtistMemberSeed("JUNO", "artist.noir7.juno@connectfin.demo", "noir7_juno", "010-9600-0007"),
                            new ArtistMemberSeed("HARU", "artist.noir7.haru@connectfin.demo", "noir7_haru", "010-9600-0008")
                    )
            ),
            new ArtistSeed(
                    4L,
                    "hanabi*",
                    "hanabi-star",
                    "hanabi* 공식 커뮤니티입니다. 솔로 아티스트 흐름과 팬레터 수신 대상을 함께 확인할 수 있습니다.",
                    List.of(
                            new ArtistMemberSeed("hanabi*", "artist.hanabi.main@connectfin.demo", "hanabi_main", "010-9600-0009")
                    )
            ),
            new ArtistSeed(
                    5L,
                    "Velvet Static",
                    "velvet-static",
                    "Velvet Static 공식 커뮤니티입니다. 밴드형 아티스트 프로필과 커뮤니티 흐름 시연용입니다.",
                    List.of(
                            new ArtistMemberSeed("SEUNG", "artist.velvet.seung@connectfin.demo", "velvet_seung", "010-9600-0010"),
                            new ArtistMemberSeed("JIN", "artist.velvet.jin@connectfin.demo", "velvet_jin", "010-9600-0011")
                    )
            ),
            new ArtistSeed(
                    6L,
                    "ORBITAL",
                    "orbital",
                    "ORBITAL 공식 커뮤니티입니다. 힙합 크루 스타일 아티스트 테스트를 위한 시연 데이터입니다.",
                    List.of(
                            new ArtistMemberSeed("LOW-G", "artist.orbital.lowg@connectfin.demo", "orbital_lowg", "010-9600-0012"),
                            new ArtistMemberSeed("COSM", "artist.orbital.cosm@connectfin.demo", "orbital_cosm", "010-9600-0013"),
                            new ArtistMemberSeed("NOVA", "artist.orbital.nova@connectfin.demo", "orbital_nova", "010-9600-0014")
                    )
            )
    );

    private final MemberRepository memberRepository;
    private final ArtistRepository artistRepository;
    private final ArtistMemberRepository artistMemberRepository;
    private final ArtistPostRepository artistPostRepository;
    private final FanPostRepository fanPostRepository;
    private final FanLetterRepository fanLetterRepository;
    private final CommentRepository commentRepository;
    private final InteractionRepository interactionRepository;
    private final FollowRepository followRepository;
    private final FanMembershipRepository fanMembershipRepository;
    private final DmSubscriptionRepository dmSubscriptionRepository;
    private final MediaRepository mediaRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.seed.showcase-password:}")
    private String showcasePassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (showcasePassword == null || showcasePassword.isBlank()) {
            throw new IllegalStateException("app.seed.showcase-data=true 이면 app.seed.showcase-password 는 필수입니다.");
        }

        if (!acquireLock()) {
            log.info("배포 시연 데이터 시드 잠금을 획득하지 못해 이번 인스턴스는 건너뜁니다.");
            return;
        }

        try {
            List<Member> fanMembers = ensureFanMembers();
            int seededArtistCount = 0;
            int seededArtistMemberCount = 0;

            for (ArtistSeed artistSeed : ARTIST_SEEDS) {
                Artist artist = ensureArtist(artistSeed);
                List<ArtistMember> artistMembers = ensureArtistMembers(artist, artistSeed.members());
                seededArtistCount++;
                seededArtistMemberCount += artistMembers.size();

                ensureFanSubscriptions(artist, fanMembers);
                ensureDmSubscriptions(artist, fanMembers);
                ensureFollows(fanMembers, artistMembers.get(0));

                List<ArtistPost> artistPosts = seedArtistPosts(artist, artistMembers);
                List<FanPost> fanPosts = seedFanPosts(artist, artistMembers, fanMembers);
                List<FanLetter> fanLetters = seedFanLetters(artist, artistMembers, fanMembers);

                seedArtistPostComments(artist, artistMembers, fanMembers, artistPosts);
                seedFanPostComments(artist, artistMembers, fanMembers, fanPosts);

                seedArtistPostLikes(fanMembers, artistPosts);
                seedFanPostLikes(artistMembers, fanMembers, fanPosts);
                seedFanLetterLikes(artistMembers, fanMembers, fanLetters);
            }

            log.info(
                    "배포 시연 데이터 시드 완료: artistsSeeded={}, artistMembersSeeded={}, fans={}",
                    seededArtistCount,
                    seededArtistMemberCount,
                    FAN_SEEDS.size()
            );
        } finally {
            releaseLock();
        }
    }

    private List<Member> ensureFanMembers() {
        List<Member> fanMembers = new ArrayList<>();
        for (int index = 0; index < FAN_SEEDS.size(); index++) {
            FanSeed fanSeed = FAN_SEEDS.get(index);
            fanMembers.add(ensureMember(
                    fanSeed.email(),
                    fanSeed.nickname(),
                    fanSeed.phoneNumber(),
                    MemberRole.MEMBER,
                    profileImageUrl("fan", fanSeed.nickname()),
                    coverImageUrl("fan", fanSeed.nickname())
            ));
        }
        return fanMembers;
    }

    private Artist ensureArtist(ArtistSeed seed) {
        Artist artistById = artistRepository.findById(seed.id()).orElse(null);
        Artist artistBySlug = artistRepository.findBySlug(seed.slug()).orElse(null);

        if (artistById != null && !seed.slug().equals(artistById.getSlug())) {
            throw new IllegalStateException(
                    "showcase artistId=%d 충돌: 기존 slug=%s, 요청 slug=%s".formatted(
                    seed.id(),
                    artistById.getSlug(),
                    seed.slug()
            ));
        }

        if (artistBySlug != null && !artistBySlug.getId().equals(seed.id())) {
            throw new IllegalStateException(
                    "showcase slug=%s 충돌: 기존 artistId=%d, 요청 artistId=%d".formatted(
                    seed.slug(),
                    artistBySlug.getId(),
                    seed.id()
            ));
        }

        if (artistById != null) {
            artistById.updateProfile(
                    seed.name(),
                    seed.slug(),
                    profileImageUrl("artist", seed.slug()),
                    coverImageUrl("artist", seed.slug()),
                    seed.intro()
            );
            return artistById;
        }

        if (artistBySlug != null) {
            artistBySlug.updateProfile(
                    seed.name(),
                    seed.slug(),
                    profileImageUrl("artist", seed.slug()),
                    coverImageUrl("artist", seed.slug()),
                    seed.intro()
            );
            return artistBySlug;
        }

        insertArtistWithFixedId(seed);
        return artistRepository.findById(seed.id())
                .orElseThrow(() -> new IllegalStateException("시드 아티스트 생성 후 조회에 실패했습니다. id=" + seed.id()));
    }

    private List<ArtistMember> ensureArtistMembers(Artist artist, List<ArtistMemberSeed> seeds) {
        List<ArtistMember> artistMembers = new ArrayList<>();

        for (int index = 0; index < seeds.size(); index++) {
            ArtistMemberSeed seed = seeds.get(index);
            int sortOrder = index + 1;
            Member member = ensureMember(
                    seed.email(),
                    seed.nickname(),
                    seed.phoneNumber(),
                    MemberRole.ARTIST,
                    profileImageUrl("artist-member", seed.nickname()),
                    coverImageUrl("artist-member", seed.nickname())
            );

            ArtistMember artistMember = artistMemberRepository.findByArtistIdAndMemberId(artist.getId(), member.getId())
                    .map(existing -> {
                        existing.updateProfile(
                                seed.stageName(),
                                profileImageUrl("artist-member", seed.nickname()),
                                MemberStatus.ACTIVE,
                                sortOrder
                        );
                        return existing;
                    })
                    .orElseGet(() -> artistMemberRepository.save(ArtistMember.create(
                            artist,
                            member,
                            seed.stageName(),
                            profileImageUrl("artist-member", seed.nickname()),
                            sortOrder
                    )));

            artistMembers.add(artistMember);
        }

        return artistMembers;
    }

    private Member ensureMember(
            String email,
            String nickname,
            String phoneNumber,
            MemberRole role,
            String profileImageUrl,
            String coverImageUrl
    ) {
        return memberRepository.findByEmail(email)
                .map(existing -> {
                    if (existing.getRole() != role) {
                        existing.changeRole(role);
                    }
                    if (existing.getStatus() != MemberStatus.ACTIVE) {
                        existing.changeStatus(MemberStatus.ACTIVE);
                    }
                    if (!passwordEncoder.matches(showcasePassword, existing.getPassword())) {
                        existing.changePassword(passwordEncoder.encode(showcasePassword));
                    }
                    existing.updateProfile(nickname, phoneNumber, profileImageUrl, coverImageUrl);
                    return existing;
                })
                .orElseGet(() -> {
                    Member member = Member.createNewMember(
                            email,
                            passwordEncoder.encode(showcasePassword),
                            nickname,
                            phoneNumber
                    );
                    member.changeRole(role);
                    member.changeStatus(MemberStatus.ACTIVE);
                    member.updateProfile(nickname, phoneNumber, profileImageUrl, coverImageUrl);
                    return memberRepository.save(member);
                });
    }

    private void ensureFanSubscriptions(Artist artist, List<Member> fanMembers) {
        LocalDateTime now = LocalDateTime.now();
        for (Member fanMember : fanMembers) {
            boolean activeMembership = fanMembershipRepository
                    .findByUserIdAndArtistIdAndStatus(fanMember.getId(), artist.getId(), SubscriptionStatus.ACTIVE)
                    .filter(FanMembership::isActive)
                    .isPresent();

            if (activeMembership) {
                continue;
            }

            fanMembershipRepository.save(FanMembership.builder()
                    .userId(fanMember.getId())
                    .artistId(artist.getId())
                    .startedAt(now.minusDays(5))
                    .expiredAt(now.plusDays(45))
                    .jellyAmount(9)
                    .build());
        }
    }

    private void ensureDmSubscriptions(Artist artist, List<Member> fanMembers) {
        LocalDateTime now = LocalDateTime.now();
        for (int index = 0; index < Math.min(4, fanMembers.size()); index++) {
            Member fanMember = fanMembers.get(index);
            boolean activeSubscription = dmSubscriptionRepository
                    .findByUserIdAndArtistIdAndStatus(fanMember.getId(), artist.getId(), SubscriptionStatus.ACTIVE)
                    .filter(DmSubscription::isActive)
                    .isPresent();

            if (activeSubscription) {
                continue;
            }

            dmSubscriptionRepository.save(DmSubscription.builder()
                    .userId(fanMember.getId())
                    .artistId(artist.getId())
                    .startedAt(now.minusDays(3))
                    .expiredAt(now.plusDays(30))
                    .jellyAmount(15)
                    .build());
        }
    }

    private void ensureFollows(List<Member> fanMembers, ArtistMember primaryArtistMember) {
        for (Member fanMember : fanMembers) {
            if (followRepository.findByFollowerMemberIdAndTargetArtistMemberId(
                    fanMember.getId(),
                    primaryArtistMember.getId()
            ).isPresent()) {
                continue;
            }

            followRepository.save(Follow.create(fanMember, primaryArtistMember));
        }
    }

    private List<ArtistPost> seedArtistPosts(Artist artist, List<ArtistMember> artistMembers) {
        List<ArtistPost> posts = new ArrayList<>();

        for (int index = 0; index < ARTIST_POST_TEMPLATES.size(); index++) {
            ArtistMember writer = artistMembers.get(index % artistMembers.size());
            String content = buildArtistPostContent(artist.getName(), writer.getStageName(), index);

            ArtistPost post = artistPostRepository.findByArtistIdAndWriterIdAndContent(artist.getId(), writer.getMember().getId(), content)
                    .orElseGet(() -> artistPostRepository.save(ArtistPost.create(artist, writer.getMember(), content)));
            posts.add(post);
        }

        return posts;
    }

    private List<FanPost> seedFanPosts(Artist artist, List<ArtistMember> artistMembers, List<Member> fanMembers) {
        List<FanPost> posts = new ArrayList<>();
        String primaryStageName = artistMembers.get(0).getStageName();

        for (int index = 0; index < FAN_POST_TEMPLATES.size(); index++) {
            Member writer = fanMembers.get((int) ((artist.getId() + index) % fanMembers.size()));
            String content = buildFanPostContent(artist.getName(), primaryStageName, index);

            FanPost post = fanPostRepository.findByArtistIdAndWriterIdAndContent(artist.getId(), writer.getId(), content)
                    .orElseGet(() -> fanPostRepository.save(FanPost.create(artist, writer, content)));
            posts.add(post);
        }

        return posts;
    }

    private List<FanLetter> seedFanLetters(Artist artist, List<ArtistMember> artistMembers, List<Member> fanMembers) {
        List<FanLetter> letters = new ArrayList<>();

        for (int index = 0; index < 4; index++) {
            Member writer = fanMembers.get((int) ((artist.getId() + index + 2) % fanMembers.size()));
            FanLetterRecipientType recipientType = index < 2 ? FanLetterRecipientType.ARTIST : FanLetterRecipientType.ARTIST_MEMBER;
            ArtistMember recipientArtistMember = recipientType == FanLetterRecipientType.ARTIST
                    ? null
                    : artistMembers.get(index % artistMembers.size());

            FanLetter fanLetter = findExistingFanLetter(artist.getId(), writer.getId(), recipientType, recipientArtistMember)
                    .orElseGet(() -> fanLetterRepository.save(FanLetter.create(
                            artist,
                            writer,
                            recipientType,
                            recipientArtistMember
                    )));

            ensureFanLetterImage(artist, writer, fanLetter, recipientType, recipientArtistMember, index);
            letters.add(fanLetter);
        }

        return letters;
    }

    private Optional<FanLetter> findExistingFanLetter(
            Long artistId,
            Long writerId,
            FanLetterRecipientType recipientType,
            ArtistMember recipientArtistMember
    ) {
        if (recipientArtistMember == null) {
            return fanLetterRepository.findByArtistIdAndWriterIdAndRecipientTypeAndRecipientArtistMemberIsNull(
                    artistId,
                    writerId,
                    recipientType
            );
        }

        return fanLetterRepository.findByArtistIdAndWriterIdAndRecipientTypeAndRecipientArtistMemberId(
                artistId,
                writerId,
                recipientType,
                recipientArtistMember.getId()
        );
    }

    private void ensureFanLetterImage(
            Artist artist,
            Member writer,
            FanLetter fanLetter,
            FanLetterRecipientType recipientType,
            ArtistMember recipientArtistMember,
            int index
    ) {
        if (!mediaRepository.findByTargetTypeAndTargetIdOrderBySortOrderAsc(PostType.FAN_LETTER, fanLetter.getId()).isEmpty()) {
            return;
        }

        String recipientKey = recipientArtistMember == null ? "artist" : recipientArtistMember.getStageName().toLowerCase();
        String imageUrl = "https://picsum.photos/seed/showcase-letter-" + artist.getId() + "-" + writer.getId() + "-" + recipientKey + "-" + index + "/900/1200";

        mediaRepository.save(Media.create(
                PostType.FAN_LETTER,
                fanLetter.getId(),
                MediaType.IMAGE,
                "showcase/fan-letter/" + artist.getId() + "/" + fanLetter.getId() + "/" + recipientKey + ".jpg",
                imageUrl,
                imageUrl,
                "showcase-letter-" + artist.getId() + "-" + writer.getId() + "-" + index + ".jpg",
                "image/jpeg",
                250_000L,
                0
        ));
    }

    private void seedArtistPostComments(
            Artist artist,
            List<ArtistMember> artistMembers,
            List<Member> fanMembers,
            List<ArtistPost> artistPosts
    ) {
        for (int index = 0; index < artistPosts.size(); index++) {
            ArtistPost artistPost = artistPosts.get(index);
            Member rootWriter = fanMembers.get(index % fanMembers.size());
            Member secondWriter = fanMembers.get((index + 1) % fanMembers.size());
            Member replyWriter = artistMembers.get(index % artistMembers.size()).getMember();

            Comment rootOne = ensureComment(
                    PostType.ARTIST_POST,
                    artistPost.getId(),
                    rootWriter,
                    String.format(ARTIST_POST_COMMENT_TEMPLATES.get(0), artist.getName()),
                    null,
                    "showcase-artist-post-" + artistPost.getId() + "-root-1",
                    artistPost::changeCommentCountBy
            );
            ensureComment(
                    PostType.ARTIST_POST,
                    artistPost.getId(),
                    secondWriter,
                    ARTIST_POST_COMMENT_TEMPLATES.get(1),
                    null,
                    "showcase-artist-post-" + artistPost.getId() + "-root-2",
                    artistPost::changeCommentCountBy
            );
            ensureComment(
                    PostType.ARTIST_POST,
                    artistPost.getId(),
                    replyWriter,
                    REPLY_TEMPLATES.get(index % REPLY_TEMPLATES.size()),
                    rootOne,
                    "showcase-artist-post-" + artistPost.getId() + "-reply-1",
                    artistPost::changeCommentCountBy
            );
        }
    }

    private void seedFanPostComments(
            Artist artist,
            List<ArtistMember> artistMembers,
            List<Member> fanMembers,
            List<FanPost> fanPosts
    ) {
        for (int index = 0; index < fanPosts.size(); index++) {
            FanPost fanPost = fanPosts.get(index);
            Member rootWriter = fanMembers.get((index + 2) % fanMembers.size());
            Member secondWriter = fanMembers.get((index + 3) % fanMembers.size());
            Member replyWriter = artistMembers.get(index % artistMembers.size()).getMember();

            Comment rootOne = ensureComment(
                    PostType.FAN_POST,
                    fanPost.getId(),
                    rootWriter,
                    FAN_POST_COMMENT_TEMPLATES.get(0),
                    null,
                    "showcase-fan-post-" + fanPost.getId() + "-root-1",
                    fanPost::changeCommentCountBy
            );
            ensureComment(
                    PostType.FAN_POST,
                    fanPost.getId(),
                    secondWriter,
                    FAN_POST_COMMENT_TEMPLATES.get(1),
                    null,
                    "showcase-fan-post-" + fanPost.getId() + "-root-2",
                    fanPost::changeCommentCountBy
            );
            ensureComment(
                    PostType.FAN_POST,
                    fanPost.getId(),
                    replyWriter,
                    REPLY_TEMPLATES.get((index + 1) % REPLY_TEMPLATES.size()),
                    rootOne,
                    "showcase-fan-post-" + fanPost.getId() + "-reply-1",
                    fanPost::changeCommentCountBy
            );
        }
    }

    private Comment ensureComment(
            PostType targetType,
            Long targetId,
            Member writer,
            String content,
            Comment parent,
            String commandRequestId,
            java.util.function.IntConsumer commentCountChanger
    ) {
        return commentRepository.findByCommandRequestId(commandRequestId)
                .orElseGet(() -> {
                    Comment comment = commentRepository.save(Comment.create(
                            targetType,
                            targetId,
                            writer,
                            content,
                            parent,
                            commandRequestId
                    ));
                    commentCountChanger.accept(1);
                    return comment;
                });
    }

    private void seedArtistPostLikes(List<Member> fanMembers, List<ArtistPost> artistPosts) {
        for (int index = 0; index < artistPosts.size(); index++) {
            ArtistPost artistPost = artistPosts.get(index);
            int likeTarget = index < 12 ? 5 : 3;
            for (int likeIndex = 0; likeIndex < likeTarget; likeIndex++) {
                Member actor = fanMembers.get((index + likeIndex) % fanMembers.size());
                ensureLike(PostType.ARTIST_POST, artistPost.getId(), actor, () -> artistPost.changeLikeCountBy(1));
            }
        }
    }

    private void seedFanPostLikes(List<ArtistMember> artistMembers, List<Member> fanMembers, List<FanPost> fanPosts) {
        for (int index = 0; index < fanPosts.size(); index++) {
            FanPost fanPost = fanPosts.get(index);
            int likeTarget = index < 2 ? 6 : index < 4 ? 5 : 3;

            for (int likeIndex = 0; likeIndex < likeTarget; likeIndex++) {
                Member actor = fanMembers.get((index + likeIndex) % fanMembers.size());
                ensureLike(PostType.FAN_POST, fanPost.getId(), actor, () -> fanPost.changeLikeCountBy(1));
            }

            Member artistActor = artistMembers.get(index % artistMembers.size()).getMember();
            ensureLike(PostType.FAN_POST, fanPost.getId(), artistActor, () -> fanPost.changeLikeCountBy(1));
        }
    }

    private void seedFanLetterLikes(List<ArtistMember> artistMembers, List<Member> fanMembers, List<FanLetter> fanLetters) {
        for (int index = 0; index < fanLetters.size(); index++) {
            FanLetter fanLetter = fanLetters.get(index);

            for (int likeIndex = 0; likeIndex < 5; likeIndex++) {
                Member actor = fanMembers.get((index + likeIndex) % fanMembers.size());
                ensureLike(PostType.FAN_LETTER, fanLetter.getId(), actor, () -> fanLetter.changeLikeCountBy(1));
            }

            Member artistActor = artistMembers.get(index % artistMembers.size()).getMember();
            ensureLike(PostType.FAN_LETTER, fanLetter.getId(), artistActor, () -> fanLetter.changeLikeCountBy(1));
        }
    }

    private void ensureLike(PostType targetType, Long targetId, Member actor, Runnable onCreated) {
        if (interactionRepository.existsByTargetTypeAndTargetIdAndMemberIdAndReactionType(
                targetType,
                targetId,
                actor.getId(),
                ReactionType.LIKE
        )) {
            return;
        }

        interactionRepository.save(Reaction.create(targetType, targetId, actor.getId(), ReactionType.LIKE));
        onCreated.run();
    }

    private String buildArtistPostContent(String artistName, String stageName, int index) {
        return switch (index) {
            case 0 -> String.format(ARTIST_POST_TEMPLATES.get(0), stageName);
            case 1 -> String.format(ARTIST_POST_TEMPLATES.get(1), artistName);
            case 2 -> String.format(ARTIST_POST_TEMPLATES.get(2), stageName);
            default -> String.format(ARTIST_POST_TEMPLATES.get(3), artistName);
        };
    }

    private String buildFanPostContent(String artistName, String primaryStageName, int index) {
        return switch (index) {
            case 0 -> String.format(FAN_POST_TEMPLATES.get(0), artistName);
            case 1 -> String.format(FAN_POST_TEMPLATES.get(1), artistName);
            case 2 -> String.format(FAN_POST_TEMPLATES.get(2), primaryStageName);
            case 3 -> String.format(FAN_POST_TEMPLATES.get(3), artistName);
            case 4 -> String.format(FAN_POST_TEMPLATES.get(4), artistName);
            default -> String.format(FAN_POST_TEMPLATES.get(5), artistName);
        };
    }

    private void insertArtistWithFixedId(ArtistSeed seed) {
        entityManager.createNativeQuery("""
                insert into artists (
                    id,
                    name,
                    slug,
                    profile_image_url,
                    cover_image_url,
                    intro,
                    status,
                    created_at,
                    updated_at,
                    deleted_at
                ) values (
                    :id,
                    :name,
                    :slug,
                    :profileImageUrl,
                    :coverImageUrl,
                    :intro,
                    'ACTIVE',
                    current_timestamp,
                    current_timestamp,
                    null
                )
                """)
                .setParameter("id", seed.id())
                .setParameter("name", seed.name())
                .setParameter("slug", seed.slug())
                .setParameter("profileImageUrl", profileImageUrl("artist", seed.slug()))
                .setParameter("coverImageUrl", coverImageUrl("artist", seed.slug()))
                .setParameter("intro", seed.intro())
                .executeUpdate();
    }

    private boolean acquireLock() {
        Number result = (Number) entityManager.createNativeQuery("select get_lock(:lockName, 5)")
                .setParameter("lockName", LOCK_NAME)
                .getSingleResult();
        return result != null && result.intValue() == 1;
    }

    private void releaseLock() {
        entityManager.createNativeQuery("select release_lock(:lockName)")
                .setParameter("lockName", LOCK_NAME)
                .getSingleResult();
    }

    private String profileImageUrl(String prefix, String seedKey) {
        return "https://picsum.photos/seed/" + prefix + "-" + seedKey + "/600/600";
    }

    private String coverImageUrl(String prefix, String seedKey) {
        return "https://picsum.photos/seed/" + prefix + "-" + seedKey + "-cover/1440/720";
    }

    private record ArtistSeed(
            Long id,
            String name,
            String slug,
            String intro,
            List<ArtistMemberSeed> members
    ) {
    }

    private record ArtistMemberSeed(
            String stageName,
            String email,
            String nickname,
            String phoneNumber
    ) {
    }

    private record FanSeed(
            String email,
            String nickname,
            String phoneNumber
    ) {
    }
}
