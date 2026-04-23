package com.example.infinite.domain.artistcontent.post.fanletter.service;

import com.example.infinite.domain.artistcontent.media.service.MediaService;
import com.example.infinite.domain.artistcontent.post.cache.PostHotData;
import com.example.infinite.domain.artistcontent.post.cache.PostHotDataCacheService;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.request.FanLetterCreateRequest;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.request.FanLetterUpdateRequest;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterBaseResponse;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterCreateResponse;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterHotResponse;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterListResponse;
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
import com.example.infinite.domain.subscriptionmembership.service.SubscriptionMembershipService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.constant.CacheNames;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import com.example.infinite.global.common.dto.OffsetSliceResponse;
import com.example.infinite.global.common.util.querydsl.CursorSliceUtils;
import com.example.infinite.global.error.ErrorCode;
import com.example.infinite.global.error.SubscriptionMembershipException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FanLetterService {

    private static final int HOT_FAN_LETTER_DEFAULT_SIZE = 10;
    private static final int HOT_FAN_LETTER_MAX_SIZE = 30;
    private static final long HOT_LOOKBACK_HOURS = 24L;
    private static final long HOT_MIN_LIKE_COUNT = 5L;

    private final FanLetterRepository fanLetterRepository;
    private final FanLetterReader fanLetterReader;
    private final ArtistReader artistReader;
    private final MemberReader memberReader;
    private final MediaService mediaService;
    private final SubscriptionMembershipService subscriptionMembershipService;
    private final FanLetterBaseCacheService fanLetterBaseCacheService;
    private final PostHotDataCacheService postHotDataCacheService;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.FAN_LETTER_LIST_BASE, allEntries = true),
            @CacheEvict(value = CacheNames.FAN_LETTER_DETAIL_BASE, allEntries = true)
    })
    public FanLetterCreateResponse create(
            MemberDetailsImpl memberDetails,
            Long artistId,
            FanLetterCreateRequest request
    ) {
        Member writer = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        Artist artist = artistReader.findArtistByIdOrThrow(artistId);
        validateWritePermission(artistId, writer.getId());
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
        mediaService.attachFanLetterMedia(artistId, fanLetter, request.getImage());
        return FanLetterCreateResponse.from(fanLetter);
    }

    public CursorSliceResponse<FanLetterListResponse> getFanLetters(Long artistId, Long cursor) {
        artistReader.findArtistByIdOrThrow(artistId);
        // fan letter 목록은 count를 포함하지 않으므로 base 응답 자체를 길게 캐시해도 된다.
        return fanLetterBaseCacheService.getFanLetterListBaseSlice(artistId, cursor);
    }

    public OffsetSliceResponse<FanLetterHotResponse> getHotFanLetters(
            Long artistId,
            Integer offset,
            Integer size
    ) {
        artistReader.findArtistByIdOrThrow(artistId);
        int resolvedOffset = CursorSliceUtils.resolveOffset(offset);
        int resolvedSize = CursorSliceUtils.resolveLimit(size, HOT_FAN_LETTER_DEFAULT_SIZE, HOT_FAN_LETTER_MAX_SIZE);

        // FanLetter HOT도 최신글 목록 캐시를 재사용하지 않고,
        // 최근 24시간 + 좋아요 5개 이상 기준으로 후보를 좁힌 뒤 offset slice 를 적용한다.
        // content 가 비어 있으면 프론트에서 empty state 문구를 그대로 노출하면 된다.
        OffsetSliceResponse<FanLetterListResponse> baseSlice = fanLetterBaseCacheService.loadHotFanLetterListBase(
                artistId,
                LocalDateTime.now().minusHours(HOT_LOOKBACK_HOURS),
                resolvedOffset,
                resolvedSize,
                HOT_MIN_LIKE_COUNT
        );
        Map<Long, PostHotData> hotDataByLetterId = postHotDataCacheService.getPostHotDataMap(
                PostType.FAN_LETTER,
                baseSlice.content().stream().map(FanLetterListResponse::fanLetterId).toList()
        );

        // 일반 목록 응답 shape를 최대한 유지하고,
        // HOT 탭에서 실제로 필요한 likeCount만 추가한 전용 DTO로 마무리한다.
        List<FanLetterHotResponse> content = baseSlice.content().stream()
                .map(baseResponse -> FanLetterHotResponse.from(
                        baseResponse,
                        hotDataByLetterId.getOrDefault(baseResponse.fanLetterId(), PostHotData.empty())
                ))
                .toList();

        return new OffsetSliceResponse<>(
                content,
                baseSlice.nextOffset(),
                baseSlice.hasNext(),
                baseSlice.size()
        );
    }

    public FanLetterResponse getFanLetter(Long artistId, Long fanLetterId) {
        artistReader.findArtistByIdOrThrow(artistId);

        FanLetterBaseResponse baseResponse = fanLetterBaseCacheService.getFanLetterBaseDetail(artistId, fanLetterId);
        PostHotData hotData = postHotDataCacheService.getPostHotData(PostType.FAN_LETTER, fanLetterId);
        return FanLetterResponse.from(baseResponse, hotData);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.FAN_LETTER_LIST_BASE, allEntries = true),
            @CacheEvict(value = CacheNames.FAN_LETTER_DETAIL_BASE, allEntries = true)
    })
    public FanLetterResponse update(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long fanLetterId,
            FanLetterUpdateRequest request
    ) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        FanLetter fanLetter = findOwnedFanLetter(member.getId(), artistId, fanLetterId);

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
            mediaService.replaceFanLetterMedia(artistId, fanLetter, request.getImage());
        }

        return buildCurrentFanLetterResponse(artistId, fanLetterId, PostHotData.likeOnly(fanLetter.getLikeCount()));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.FAN_LETTER_LIST_BASE, allEntries = true),
            @CacheEvict(value = CacheNames.FAN_LETTER_DETAIL_BASE, allEntries = true)
    })
    public void delete(MemberDetailsImpl memberDetails, Long artistId, Long fanLetterId) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        FanLetter fanLetter = findOwnedFanLetter(member.getId(), artistId, fanLetterId);
        mediaService.deleteFanLetterMedia(fanLetter);
        fanLetter.delete();
    }

    private FanLetterResponse buildCurrentFanLetterResponse(Long artistId, Long fanLetterId, PostHotData hotData) {
        FanLetterBaseResponse baseResponse = fanLetterBaseCacheService.loadFanLetterBaseDetail(artistId, fanLetterId);
        return FanLetterResponse.from(baseResponse, hotData);
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
            return new ResolvedRecipient(recipientType, null);
        }

        if (recipientArtistMemberId == null) {
            throw new IllegalArgumentException("아티스트 멤버에게 보내는 팬레터는 수신 멤버 선택이 필요합니다.");
        }

        ArtistMember recipientArtistMember = artistReader.findArtistMemberByIdAndArtistIdOrThrow(recipientArtistMemberId, artistId);
        return new ResolvedRecipient(recipientType, recipientArtistMember);
    }

    private record ResolvedRecipient(
            FanLetterRecipientType recipientType,
            ArtistMember recipientArtistMember
    ) {
    }
}
