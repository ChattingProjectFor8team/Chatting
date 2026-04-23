package com.example.infinite.domain.artistcontent.post.fanpost.service;

import com.example.infinite.domain.artistcontent.media.entity.Media;
import com.example.infinite.domain.artistcontent.media.repository.MediaRepository;
import com.example.infinite.domain.artistcontent.hashtag.service.HashtagService;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostBaseResponse;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostHotBaseSlice;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostHotReadRow;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostMediaResponse;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostReadRow;
import com.example.infinite.domain.artistcontent.post.fanpost.repository.FanPostRepository;
import com.example.infinite.domain.subscriptionmembership.dto.response.WriterSubscriptionBadge;
import com.example.infinite.domain.subscriptionmembership.service.SubscriptionMembershipService;
import com.example.infinite.global.common.constant.CacheNames;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FanPostBaseCacheService {

    private static final int FAN_POST_SLICE_SIZE = 10;
    private static final int FAN_POST_LIST_MEDIA_PREVIEW_LIMIT = 6;

    private final FanPostRepository fanPostRepository;
    private final MediaRepository mediaRepository;
    private final HashtagService hashtagService;
    private final SubscriptionMembershipService subscriptionMembershipService;

    /**
     * 목록 base 응답 캐시.
     *
     * 여기서는 count를 제외한 조립 결과만 캐시한다.
     * count까지 함께 묶어 버리면 hot 필드 때문에 전체 TTL이 짧아져
     * 미디어/해시태그까지 자주 다시 만들어야 하기 때문이다.
     */
    @Cacheable(
            value = CacheNames.FAN_POST_LIST_BASE,
            key = "#artistId + ':' + (#cursor == null ? 'first' : #cursor)"
    )
    public CursorSliceResponse<FanPostBaseResponse> getFanPostBaseSlice(Long artistId, Long cursor) {
        return loadFanPostBaseSlice(artistId, cursor);
    }

    /**
     * 상세 base 응답 캐시.
     * 댓글과 count는 hot 영역이거나 별도 조회 대상이므로 여기서 섞지 않는다.
     */
    @Cacheable(
            value = CacheNames.FAN_POST_DETAIL_BASE,
            key = "#artistId + ':' + #fanPostId"
    )
    public FanPostBaseResponse getFanPostBaseDetail(Long artistId, Long fanPostId) {
        return loadFanPostBaseDetail(artistId, fanPostId);
    }

    public CursorSliceResponse<FanPostBaseResponse> loadFanPostBaseSlice(Long artistId, Long cursor) {
        List<FanPostReadRow> rows = fanPostRepository.findSliceRowsByArtistId(
                artistId,
                cursor,
                FAN_POST_SLICE_SIZE + 1
        );
        boolean hasNext = rows.size() > FAN_POST_SLICE_SIZE;
        List<FanPostReadRow> visibleRows = hasNext ? rows.subList(0, FAN_POST_SLICE_SIZE) : rows;
        if (visibleRows.isEmpty()) {
            return new CursorSliceResponse<>(List.of(), null, hasNext, FAN_POST_SLICE_SIZE);
        }

        List<FanPostBaseResponse> content = buildBaseResponses(artistId, visibleRows, true);

        Long nextCursor = hasNext
                ? visibleRows.get(visibleRows.size() - 1).fanPostId()
                : null;
        return new CursorSliceResponse<>(content, nextCursor, hasNext, FAN_POST_SLICE_SIZE);
    }

    public FanPostBaseResponse loadFanPostBaseDetail(Long artistId, Long fanPostId) {
        FanPostReadRow row = fanPostRepository.findDetailRowByArtistIdAndFanPostId(artistId, fanPostId)
                .orElseThrow(() -> new ArtistContentException(ArtistContentErrorCode.POST_NOT_FOUND));
        WriterSubscriptionBadge writerBadge = subscriptionMembershipService.getWriterBadges(artistId, List.of(row.writerId()))
                .getOrDefault(row.writerId(), WriterSubscriptionBadge.empty(row.writerId()));
        FanPostReadRow rowWithBadge = new FanPostReadRow(
                row.fanPostId(),
                row.artistId(),
                row.writerId(),
                row.writerNickname(),
                row.writerProfileImageUrl(),
                writerBadge.fanMembershipSubscribed(),
                writerBadge.dmSubscribed(),
                row.content(),
                row.mediaCount(),
                row.createdAt()
        );
        List<FanPostMediaResponse> media = loadMediaMap(List.of(fanPostId)).getOrDefault(fanPostId, List.of());
        List<String> hashtags = hashtagService.findHashtagNamesByTargetIds(PostType.FAN_POST, List.of(fanPostId))
                .getOrDefault(fanPostId, List.of());
        return FanPostBaseResponse.from(rowWithBadge, media, hashtags);
    }

    public FanPostHotBaseSlice loadHotFanPostBaseSlice(
            Long artistId,
            LocalDateTime since,
            Long scoreCursor,
            Long idCursor,
            int size,
            Long minLikeCount,
            Long minCommentCount
    ) {
        // HOT은 latest slice cache 와 정렬 기준이 다르므로
        // score/id 복합커서 전용 조회를 따로 사용한다.
        List<FanPostHotReadRow> rows = fanPostRepository.findHotSliceRowsByArtistId(
                artistId,
                since,
                scoreCursor,
                idCursor,
                size + 1,
                minLikeCount,
                minCommentCount
        );
        boolean hasNext = rows.size() > size;
        List<FanPostHotReadRow> visibleRows = hasNext ? rows.subList(0, size) : rows;
        List<FanPostBaseResponse> content = buildBaseResponses(
                artistId,
                visibleRows.stream().map(FanPostHotReadRow::toBaseReadRow).toList(),
                true
        );

        Long nextScoreCursor = hasNext
                ? visibleRows.get(visibleRows.size() - 1).hotScore()
                : null;
        Long nextIdCursor = hasNext
                ? visibleRows.get(visibleRows.size() - 1).fanPostId()
                : null;

        return new FanPostHotBaseSlice(content, nextScoreCursor, nextIdCursor, hasNext, size);
    }

    private List<FanPostBaseResponse> buildBaseResponses(Long artistId, List<FanPostReadRow> rows, boolean previewMedia) {
        if (rows.isEmpty()) {
            return List.of();
        }

        List<Long> postIds = rows.stream()
                .map(FanPostReadRow::fanPostId)
                .toList();
        Map<Long, List<FanPostMediaResponse>> mediaMap = previewMedia
                ? loadPreviewMediaMap(postIds)
                : loadMediaMap(postIds);
        // HOT도 일반 목록 카드와 같은 표시 규칙을 써야 하므로 미디어 preview, 해시태그, 작성자 배지를 동일하게 합친다.
        Map<Long, List<String>> hashtagMap = hashtagService.findHashtagNamesByTargetIds(PostType.FAN_POST, postIds);
        Map<Long, WriterSubscriptionBadge> badgeByWriterId = subscriptionMembershipService.getWriterBadges(
                artistId,
                rows.stream().map(FanPostReadRow::writerId).toList()
        );

        return rows.stream()
                .map(row -> {
                    WriterSubscriptionBadge writerBadge = badgeByWriterId.getOrDefault(
                            row.writerId(),
                            WriterSubscriptionBadge.empty(row.writerId())
                    );
                    FanPostReadRow rowWithBadge = new FanPostReadRow(
                            row.fanPostId(),
                            row.artistId(),
                            row.writerId(),
                            row.writerNickname(),
                            row.writerProfileImageUrl(),
                            writerBadge.fanMembershipSubscribed(),
                            writerBadge.dmSubscribed(),
                            row.content(),
                            row.mediaCount(),
                            row.createdAt()
                    );
                    return FanPostBaseResponse.from(
                            rowWithBadge,
                            mediaMap.getOrDefault(row.fanPostId(), List.of()),
                            hashtagMap.getOrDefault(row.fanPostId(), List.of())
                    );
                })
                .toList();
    }

    private Map<Long, List<FanPostMediaResponse>> loadMediaMap(Collection<Long> fanPostIds) {
        if (fanPostIds.isEmpty()) {
            return Map.of();
        }

        return mediaRepository.findByTargetTypeAndTargetIdInOrderByTargetIdAscSortOrderAsc(PostType.FAN_POST, fanPostIds)
                .stream()
                .collect(Collectors.groupingBy(
                        Media::getTargetId,
                        Collectors.mapping(FanPostMediaResponse::from, Collectors.toList())
                ));
    }

    private Map<Long, List<FanPostMediaResponse>> loadPreviewMediaMap(Collection<Long> fanPostIds) {
        if (fanPostIds.isEmpty()) {
            return Map.of();
        }

        return mediaRepository.findPreviewByTargetTypeAndTargetIdInOrderByTargetIdAscSortOrderAsc(
                        PostType.FAN_POST,
                        fanPostIds,
                        FAN_POST_LIST_MEDIA_PREVIEW_LIMIT
                )
                .stream()
                .collect(Collectors.groupingBy(
                        Media::getTargetId,
                        Collectors.mapping(FanPostMediaResponse::from, Collectors.toList())
                ));
    }
}
