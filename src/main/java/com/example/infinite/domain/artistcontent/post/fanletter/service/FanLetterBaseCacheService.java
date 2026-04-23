package com.example.infinite.domain.artistcontent.post.fanletter.service;

import com.example.infinite.domain.artistcontent.interaction.enums.ReactionType;
import com.example.infinite.domain.artistcontent.interaction.repository.InteractionRepository;
import com.example.infinite.domain.artistcontent.media.entity.Media;
import com.example.infinite.domain.artistcontent.media.repository.MediaRepository;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.*;
import com.example.infinite.domain.artistcontent.post.fanletter.repository.FanLetterRepository;
import com.example.infinite.domain.subscriptionmembership.dto.response.WriterSubscriptionBadge;
import com.example.infinite.domain.subscriptionmembership.service.SubscriptionMembershipService;
import com.example.infinite.global.common.constant.CacheNames;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
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
public class FanLetterBaseCacheService {

    private static final int FAN_LETTER_SLICE_SIZE = 10;

    private final FanLetterRepository fanLetterRepository;
    private final MediaRepository mediaRepository;
    private final InteractionRepository interactionRepository;
    private final SubscriptionMembershipService subscriptionMembershipService;

    /**
     * FanLetter 목록 base 캐시.
     *
     * 목록은 이미지/수신자/special-like 표시용 정보처럼 비교적 안정적인 읽기 조립 결과가 중심이다.
     * 그래서 목록 자체는 길게 캐시하고, hot 필드가 필요하면 상세에서만 따로 합친다.
     */
    @Cacheable(
            value = CacheNames.FAN_LETTER_LIST_BASE,
            key = "#artistId + ':' + (#cursor == null ? 'first' : #cursor)"
    )
    public CursorSliceResponse<FanLetterListResponse> getFanLetterListBaseSlice(Long artistId, Long cursor) {
        return loadFanLetterListBaseSlice(artistId, cursor);
    }

    /**
     * FanLetter 상세 base 캐시.
     *
     * writer badge, recipient, special-like 표시용 메타데이터는 자주 안 바뀌므로 base에 포함하고,
     * likeCount만 hot cache에서 나중에 합친다.
     */
    @Cacheable(
            value = CacheNames.FAN_LETTER_DETAIL_BASE,
            key = "#artistId + ':' + #fanLetterId"
    )
    public FanLetterBaseResponse getFanLetterBaseDetail(Long artistId, Long fanLetterId) {
        return loadFanLetterBaseDetail(artistId, fanLetterId);
    }

    public CursorSliceResponse<FanLetterListResponse> loadFanLetterListBaseSlice(Long artistId, Long cursor) {
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

        List<Long> fanLetterIds = visibleRows.stream()
                .map(FanLetterListRow::fanLetterId)
                .toList();
        Map<Long, FanLetterImageResponse> imageByLetterId = loadImageMap(fanLetterIds);
        // special-like는 persisted field가 아니라 Reaction 원본을 바탕으로 읽기 시점에 조립한다.
        Map<Long, SpecialLikeInfo> specialLikeInfoByLetterId = loadSpecialLikeInfo(
                artistId,
                visibleRows,
                FanLetterListRow::fanLetterId,
                FanLetterListRow::artistDisplayName,
                FanLetterListRow::artistProfileImageUrl
        );

        List<FanLetterListResponse> content = visibleRows.stream()
                .map(row -> FanLetterListResponse.from(
                        row,
                        imageByLetterId.get(row.fanLetterId()),
                        specialLikeInfoByLetterId.getOrDefault(row.fanLetterId(), SpecialLikeInfo.empty()).artistLiked(),
                        specialLikeInfoByLetterId.getOrDefault(row.fanLetterId(), SpecialLikeInfo.empty()).artistLikeDisplayName(),
                        specialLikeInfoByLetterId.getOrDefault(row.fanLetterId(), SpecialLikeInfo.empty()).artistLikeProfileImageUrl()
                ))
                .toList();

        Long nextCursor = hasNext
                ? visibleRows.get(visibleRows.size() - 1).fanLetterId()
                : null;
        return new CursorSliceResponse<>(content, nextCursor, hasNext, FAN_LETTER_SLICE_SIZE);
    }

    public FanLetterBaseResponse loadFanLetterBaseDetail(Long artistId, Long fanLetterId) {
        FanLetterReadRow row = fanLetterRepository.findDetailRowByArtistIdAndFanLetterId(artistId, fanLetterId)
                .orElseThrow(() -> new ArtistContentException(ArtistContentErrorCode.POST_NOT_FOUND));

        Map<Long, FanLetterImageResponse> imageByLetterId = loadImageMap(List.of(fanLetterId));
        WriterSubscriptionBadge writerBadge = loadWriterBadges(artistId, List.of(row.writerId()))
                .getOrDefault(row.writerId(), WriterSubscriptionBadge.empty(row.writerId()));
        // 상세도 special-like 진실값은 FanLetter 엔티티가 아니라 Reaction 조회 결과에서 만든다.
        SpecialLikeInfo specialLikeInfo = loadSpecialLikeInfo(
                artistId,
                List.of(row),
                FanLetterReadRow::fanLetterId,
                FanLetterReadRow::artistDisplayName,
                FanLetterReadRow::artistProfileImageUrl
        )
                .getOrDefault(fanLetterId, SpecialLikeInfo.empty());

        return FanLetterBaseResponse.from(
                row,
                imageByLetterId.get(fanLetterId),
                writerBadge,
                specialLikeInfo.artistLiked(),
                specialLikeInfo.artistLikeDisplayName(),
                specialLikeInfo.artistLikeProfileImageUrl()
        );
    }

    private Map<Long, FanLetterImageResponse> loadImageMap(Collection<Long> fanLetterIds) {
        if (fanLetterIds.isEmpty()) {
            return Map.of();
        }

        // FanLetter는 정책상 이미지 1장만 허용하므로 같은 targetId에서 첫 값만 유지하면 된다.
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

        // "그 아티스트 소속 멤버 중 한 명이라도 좋아요했는가"를 한 번에 조회한다.
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

        // 현재 정책상 "누가 눌렀는지" 세부 멤버를 노출하지 않고 아티스트 대표 표시 정보만 반환한다.
        return new SpecialLikeInfo(true, artistDisplayName, artistProfileImageUrl);
    }

    private record SpecialLikeInfo(
            boolean artistLiked,
            String artistLikeDisplayName,
            String artistLikeProfileImageUrl
    ) {
        private static SpecialLikeInfo empty() {
            return new SpecialLikeInfo(false, null, null);
        }
    }
}
