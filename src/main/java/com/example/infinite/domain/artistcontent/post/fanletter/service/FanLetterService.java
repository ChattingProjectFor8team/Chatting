package com.example.infinite.domain.artistcontent.post.fanletter.service;

import com.example.infinite.domain.artistcontent.interaction.entity.Reaction;
import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.interaction.repository.InteractionRepository;
import com.example.infinite.domain.artistcontent.media.entity.Media;
import com.example.infinite.domain.artistcontent.media.repository.MediaRepository;
import com.example.infinite.domain.artistcontent.media.service.MediaService;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.request.FanLetterCreateRequest;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.request.FanLetterUpdateRequest;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterCreateResponse;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterImageResponse;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterListResponse;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterListRow;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterReadRow;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterResponse;
import com.example.infinite.domain.artistcontent.post.fanletter.entity.FanLetter;
import com.example.infinite.domain.artistcontent.post.fanletter.enums.FanLetterRecipientType;
import com.example.infinite.domain.artistcontent.post.fanletter.repository.FanLetterRepository;
import com.example.infinite.domain.artistcontent.post.fanletter.support.FanLetterReader;
import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.artist.entity.ArtistMember;
import com.example.infinite.domain.member.artist.support.ArtistReader;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.support.MemberInputSupport;
import com.example.infinite.domain.member.member.support.MemberReader;
import com.example.infinite.domain.subscriptionmembership.dto.response.WriterSubscriptionBadge;
import com.example.infinite.domain.subscriptionmembership.service.SubscriptionMembershipService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import com.example.infinite.global.error.ErrorCode;
import com.example.infinite.global.error.SubscriptionMembershipException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FanLetterService {

    private static final int FAN_LETTER_SLICE_SIZE = 10;

    private final FanLetterRepository fanLetterRepository;
    private final FanLetterReader fanLetterReader;
    private final ArtistReader artistReader;
    private final MemberReader memberReader;
    private final MediaRepository mediaRepository;
    private final MediaService mediaService;
    private final InteractionRepository interactionRepository;
    private final SubscriptionMembershipService subscriptionMembershipService;

    @Transactional
    public FanLetterCreateResponse create(
            MemberDetailsImpl memberDetails,
            Long artistId,
            FanLetterCreateRequest request
    ) {
        // 팬레터는 팬 멤버십이 있는 사용자만 작성 가능하다.
        // 생성 시점에 누구에게 보내는 편지인지(아티스트 전체 / 특정 멤버)를 먼저 확정한 뒤
        // 본문 역할을 하는 이미지 한 장을 media 도메인에 위임해 저장한다.
        Member writer = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        Artist artist = artistReader.findArtistByIdOrThrow(artistId);
        validateWritePermission(artistId, writer.getId());
        // 수신 대상은 "아티스트 전체" 또는 "특정 아티스트 멤버" 둘 중 하나로 정규화한다.
        ResolvedRecipient resolvedRecipient = resolveRecipient(
                artistId,
                request.getRecipientType(),
                request.getRecipientArtistMemberId()
        );

        FanLetter fanLetter = fanLetterRepository.save(FanLetter.create(
                artist,
                writer,
                resolvedRecipient.recipientType(),
                resolvedRecipient.recipientArtistMember()
        ));
        // 팬레터 본문 역할을 하는 이미지 1장은 media 서비스가 storage 와 DB 메타데이터를 함께 처리한다.
        mediaService.attachFanLetterMedia(artistId, fanLetter, request.getImage());
        return FanLetterCreateResponse.from(fanLetter);
    }

    public CursorSliceResponse<FanLetterListResponse> getFanLetters(Long artistId, Long cursor) {
        artistReader.findArtistByIdOrThrow(artistId);

        // 팬레터 목록은 사진 카드 렌더링에 필요한 최소 정보만 읽는다.
        // 작성자 프로필/배지는 상세에서만 내려주고, 목록은 이미지/수신자/special-like만 조립한다.
        List<FanLetterListRow> rows = fanLetterRepository.findSliceRowsByArtistId(
                artistId,
                cursor,
                FAN_LETTER_SLICE_SIZE + 1
        );
        boolean hasNext = rows.size() > FAN_LETTER_SLICE_SIZE;
        List<FanLetterListRow> visibleRows = hasNext ? rows.subList(0, FAN_LETTER_SLICE_SIZE) : rows;
        if (visibleRows.isEmpty()) {
            return new CursorSliceResponse<>(List.of(), null, false, FAN_LETTER_SLICE_SIZE);
        }

        return toCursorSliceResponse(artistId, visibleRows, hasNext);
    }

    public FanLetterResponse getFanLetter(Long artistId, Long fanLetterId) {
        artistReader.findArtistByIdOrThrow(artistId);
        FanLetterReadRow row = fanLetterRepository.findDetailRowByArtistIdAndFanLetterId(artistId, fanLetterId)
                .orElseThrow(() -> new ArtistContentException(ArtistContentErrorCode.POST_NOT_FOUND));

        // 상세는 작성자 프로필/배지까지 보여주므로 목록보다 더 많은 정보를 조립한다.
        Map<Long, FanLetterImageResponse> imageByLetterId = loadImageMap(List.of(fanLetterId));
        WriterSubscriptionBadge writerBadge = loadWriterBadges(artistId, List.of(row.writerId()))
                .getOrDefault(row.writerId(), WriterSubscriptionBadge.empty(row.writerId()));
        SpecialLikeInfo specialLikeInfo = loadSpecialLikeInfo(
                artistId,
                List.of(row),
                FanLetterReadRow::fanLetterId,
                FanLetterReadRow::artistDisplayName,
                FanLetterReadRow::artistProfileImageUrl
        )
                .getOrDefault(fanLetterId, SpecialLikeInfo.empty());

        return FanLetterResponse.from(
                row,
                imageByLetterId.get(fanLetterId),
                writerBadge,
                specialLikeInfo.artistLiked(),
                specialLikeInfo.artistLikeDisplayName(),
                specialLikeInfo.artistLikeProfileImageUrl()
        );
    }

    @Transactional
    public FanLetterResponse update(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long fanLetterId,
            FanLetterUpdateRequest request
    ) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        FanLetter fanLetter = findOwnedFanLetter(member.getId(), artistId, fanLetterId);

        // 수신 대상을 바꾸는 경우에는 "아티스트 전체" / "특정 멤버" 규칙을 다시 검증한다.
        // recipientType=ARTIST 로 바꾸면 recipientArtistMember 는 null 이어야 하므로 resolveRecipient 에서 정규화한다.
        if (request.getRecipientType() != null || request.getRecipientArtistMemberId() != null) {
            FanLetterRecipientType resolvedRecipientType =
                    request.getRecipientType() != null ? request.getRecipientType() : fanLetter.getRecipientType();
            Long resolvedRecipientArtistMemberId = null;

            if (resolvedRecipientType != FanLetterRecipientType.ARTIST) {
                if (request.getRecipientArtistMemberId() != null) {
                    resolvedRecipientArtistMemberId = request.getRecipientArtistMemberId();
                } else if (fanLetter.getRecipientArtistMember() != null) {
                    resolvedRecipientArtistMemberId = fanLetter.getRecipientArtistMember().getId();
                }
            }

            ResolvedRecipient resolvedRecipient = resolveRecipient(
                    artistId,
                    resolvedRecipientType,
                    resolvedRecipientArtistMemberId
            );
            fanLetter.updateRecipient(resolvedRecipient.recipientType(), resolvedRecipient.recipientArtistMember());
        }

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            // 팬레터는 이미지 1장만 유지하므로 수정도 append 가 아니라 교체 정책으로 처리한다.
            mediaService.replaceFanLetterMedia(artistId, fanLetter, request.getImage());
        }

        return getFanLetter(artistId, fanLetterId);
    }

    @Transactional
    public void delete(MemberDetailsImpl memberDetails, Long artistId, Long fanLetterId) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        FanLetter fanLetter = findOwnedFanLetter(member.getId(), artistId, fanLetterId);
        mediaService.deleteFanLetterMedia(fanLetter);
        fanLetter.delete();
    }

    private FanLetter findOwnedFanLetter(Long memberId, Long artistId, Long fanLetterId) {
        FanLetter fanLetter = fanLetterReader.findByIdAndArtistIdOrThrow(fanLetterId, artistId);
        if (!fanLetter.getWriter().getId().equals(memberId)) {
            throw new ArtistContentException(ArtistContentErrorCode.POST_PERMISSION_DENIED);
        }
        return fanLetter;
    }

    private void validateWritePermission(Long artistId, Long memberId) {
        if (!subscriptionMembershipService.checkFanLetterPermission(artistId, memberId).allowed()) {
            throw new SubscriptionMembershipException(ErrorCode.FAN_LETTER_PERMISSION_REQUIRED);
        }
    }

    private ResolvedRecipient resolveRecipient(Long artistId, FanLetterRecipientType recipientType, Long recipientArtistMemberId) {
        if (recipientType == null) {
            throw new IllegalArgumentException("팬레터 수신 대상은 필수입니다.");
        }

        if (recipientType == FanLetterRecipientType.ARTIST) {
            // To.세븐틴 같은 케이스다.
            // 별도 recipientArtistMember 를 저장하지 않고 artist 자체를 수신자로 해석한다.
            return new ResolvedRecipient(recipientType, null);
        }

        if (recipientArtistMemberId == null) {
            throw new IllegalArgumentException("아티스트 멤버에게 보내는 팬레터는 수신 멤버 선택이 필요합니다.");
        }

        // To.민규 같은 케이스다.
        // fan letter 와 artistMember 를 직접 연결해 두면
        // 상세 응답에서 stageName / profileImageUrl / special-like 판단을 안정적으로 재사용할 수 있다.
        ArtistMember recipientArtistMember = artistReader.findArtistMemberByIdAndArtistIdOrThrow(recipientArtistMemberId, artistId);
        return new ResolvedRecipient(recipientType, recipientArtistMember);
    }

    private CursorSliceResponse<FanLetterListResponse> toCursorSliceResponse(
            Long artistId,
            List<FanLetterListRow> visibleRows,
            boolean hasNext
    ) {
        List<Long> fanLetterIds = visibleRows.stream()
                .map(FanLetterListRow::fanLetterId)
                .toList();
        Map<Long, FanLetterImageResponse> imageByLetterId = loadImageMap(fanLetterIds);
        Map<Long, SpecialLikeInfo> specialLikeInfoByLetterId = loadSpecialLikeInfo(
                artistId,
                visibleRows,
                FanLetterListRow::fanLetterId,
                FanLetterListRow::artistDisplayName,
                FanLetterListRow::artistProfileImageUrl
        );

        List<FanLetterListResponse> responses = visibleRows.stream()
                .map(row -> FanLetterListResponse.from(
                        row,
                        imageByLetterId.get(row.fanLetterId()),
                        specialLikeInfoByLetterId.getOrDefault(row.fanLetterId(), SpecialLikeInfo.empty()).artistLiked(),
                        specialLikeInfoByLetterId.getOrDefault(row.fanLetterId(), SpecialLikeInfo.empty()).artistLikeDisplayName(),
                        specialLikeInfoByLetterId.getOrDefault(row.fanLetterId(), SpecialLikeInfo.empty()).artistLikeProfileImageUrl()
                ))
                .toList();

        Long nextCursor = hasNext && !visibleRows.isEmpty()
                ? visibleRows.get(visibleRows.size() - 1).fanLetterId()
                : null;
        return new CursorSliceResponse<>(responses, nextCursor, hasNext, FAN_LETTER_SLICE_SIZE);
    }

    private Map<Long, FanLetterImageResponse> loadImageMap(Collection<Long> fanLetterIds) {
        if (fanLetterIds.isEmpty()) {
            return Map.of();
        }

        // 팬레터는 이미지 한 장만 허용하므로 targetId 당 첫 번째 media row 만 응답에 매핑한다.
        return mediaRepository.findByTargetTypeAndTargetIdInOrderByTargetIdAscSortOrderAsc(PostType.FAN_LETTER, fanLetterIds)
                .stream()
                .collect(Collectors.toMap(
                        Media::getTargetId,
                        FanLetterImageResponse::from,
                        (left, right) -> left
                ));
    }

    private Map<Long, WriterSubscriptionBadge> loadWriterBadges(Long artistId, List<Long> writerIds) {
        if (writerIds.isEmpty()) {
            return Map.of();
        }
        // 팬포스트와 같은 배지 계약을 그대로 재사용해
        // 팬레터 작성자 옆에도 멤버십/DM 배지를 동일한 방식으로 붙인다.
        return subscriptionMembershipService.getWriterBadges(artistId, writerIds);
    }

    private <T> Map<Long, SpecialLikeInfo> loadSpecialLikeInfo(
            Long artistId,
            List<T> rows,
            Function<T, Long> fanLetterIdExtractor,
            Function<T, String> artistDisplayNameExtractor,
            Function<T, String> artistProfileImageUrlExtractor
    ) {
        if (rows.isEmpty()) {
            return Map.of();
        }

        // special-like 에 필요한 건 "아티스트 멤버가 좋아요한 fanLetterId 존재 여부"뿐이라
        // reaction 전체를 가져오지 않고 targetId 집합만 바로 조회한다.
        var likedFanLetterIds = interactionRepository.findTargetIdsReactedByArtistMembers(
                artistId,
                PostType.FAN_LETTER,
                rows.stream().map(fanLetterIdExtractor).toList(),
                ReactionType.LIKE
        );

        return rows.stream()
                .collect(Collectors.toMap(
                        fanLetterIdExtractor,
                        row -> resolveSpecialLikeInfo(
                                artistDisplayNameExtractor.apply(row),
                                artistProfileImageUrlExtractor.apply(row),
                                likedFanLetterIds.contains(fanLetterIdExtractor.apply(row))
                        )
                ));
    }

    private SpecialLikeInfo resolveSpecialLikeInfo(
            String artistDisplayName,
            String artistProfileImageUrl,
            boolean artistReacted
    ) {
        if (!artistReacted) {
            return SpecialLikeInfo.empty();
        }

        // 팬레터의 special-like 는 "아티스트 팀이 반응했는가"만 본다.
        // 따라서 수신 대상이 그룹이든 특정 멤버든 상관없이,
        // 아티스트 소속 멤버 중 한 명이라도 좋아요하면 artist 프로필 기준 오버레이를 켠다.
        return new SpecialLikeInfo(true, artistDisplayName, artistProfileImageUrl);
    }

    private record ResolvedRecipient(
            FanLetterRecipientType recipientType,
            ArtistMember recipientArtistMember
    ) {
        // recipientType 과 연결된 artistMember 를 한 덩어리로 다뤄
        // create/update 모두 같은 정규화 로직을 재사용한다.
    }

    private record SpecialLikeInfo(
            boolean artistLiked,
            String artistLikeDisplayName,
            String artistLikeProfileImageUrl
    ) {
        private static SpecialLikeInfo empty() {
            // 아티스트 측 반응이 아직 없으면 오버레이 관련 필드는 모두 비운다.
            return new SpecialLikeInfo(false, null, null);
        }
    }

}
