package com.example.infinite.domain.artistcontent.post.artistpost.service;

import com.example.infinite.domain.artistcontent.media.entity.Media;
import com.example.infinite.domain.artistcontent.media.repository.MediaRepository;
import com.example.infinite.domain.artistcontent.hashtag.service.HashtagService;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostBaseResponse;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostMediaResponse;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostReadRow;
import com.example.infinite.domain.artistcontent.post.artistpost.repository.ArtistPostRepository;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.global.common.constant.CacheNames;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistPostBaseCacheService {

    private static final int ARTIST_POST_SLICE_SIZE = 10;
    private static final int ARTIST_POST_LIST_MEDIA_PREVIEW_LIMIT = 6;

    private final ArtistPostRepository artistPostRepository;
    private final MediaRepository mediaRepository;
    private final HashtagService hashtagService;

    /**
     * 목록 base 응답 캐시.
     *
     * ArtistPost는 좋아요/댓글 수가 3초 flush 구조를 타므로 count를 함께 캐시하면 전체 TTL이 짧아진다.
     * 그래서 본문/미디어/해시태그/작성자 정보만 먼저 길게 캐시하고 count는 나중에 hot cache로 합친다.
     */
    @Cacheable(
            value = CacheNames.ARTIST_POST_LIST_BASE,
            key = "#artistId + ':' + (#cursor == null ? 'first' : #cursor)"
    )
    public CursorSliceResponse<ArtistPostBaseResponse> getArtistPostBaseSlice(Long artistId, Long cursor) {
        return loadArtistPostBaseSlice(artistId, cursor);
    }

    /**
     * 상세 base 응답 캐시.
     * 댓글과 count는 변동성이 높아 별도 조회/별도 캐시 전략을 사용하므로 base 응답에는 섞지 않는다.
     */
    @Cacheable(
            value = CacheNames.ARTIST_POST_DETAIL_BASE,
            key = "#artistId + ':' + #artistPostId"
    )
    public ArtistPostBaseResponse getArtistPostBaseDetail(Long artistId, Long artistPostId) {
        return loadArtistPostBaseDetail(artistId, artistPostId);
    }

    /**
     * ArtistPost는 count 분리 후에도
     * 본문/미디어/해시태그 쪽 조립 비용이 남기 때문에 base 캐시 효과가 크다.
     */
    public CursorSliceResponse<ArtistPostBaseResponse> loadArtistPostBaseSlice(Long artistId, Long cursor) {
        List<ArtistPostReadRow> rows = artistPostRepository.findSliceRowsByArtistId(
                artistId,
                cursor,
                ARTIST_POST_SLICE_SIZE + 1
        );
        boolean hasNext = rows.size() > ARTIST_POST_SLICE_SIZE;
        List<ArtistPostReadRow> visibleRows = hasNext ? rows.subList(0, ARTIST_POST_SLICE_SIZE) : rows;
        if (visibleRows.isEmpty()) {
            return new CursorSliceResponse<>(List.of(), null, hasNext, ARTIST_POST_SLICE_SIZE);
        }

        List<Long> postIds = visibleRows.stream()
                .map(ArtistPostReadRow::artistPostId)
                .toList();
        Map<Long, List<ArtistPostMediaResponse>> mediaMap = loadPreviewMediaMap(postIds);
        Map<Long, List<String>> hashtagMap = hashtagService.findHashtagNamesByTargetIds(PostType.ARTIST_POST, postIds);

        List<ArtistPostBaseResponse> content = visibleRows.stream()
                .map(row -> ArtistPostBaseResponse.from(
                        row,
                        mediaMap.getOrDefault(row.artistPostId(), List.of()),
                        hashtagMap.getOrDefault(row.artistPostId(), List.of())
                ))
                .toList();

        Long nextCursor = hasNext
                ? visibleRows.get(visibleRows.size() - 1).artistPostId()
                : null;
        return new CursorSliceResponse<>(content, nextCursor, hasNext, ARTIST_POST_SLICE_SIZE);
    }

    public ArtistPostBaseResponse loadArtistPostBaseDetail(Long artistId, Long artistPostId) {
        ArtistPostReadRow row = artistPostRepository.findDetailRowByArtistIdAndArtistPostId(artistId, artistPostId)
                .orElseThrow(() -> new ArtistContentException(ArtistContentErrorCode.POST_NOT_FOUND));
        // 상세는 preview가 아니라 전체 미디어를 붙인다.
        List<ArtistPostMediaResponse> media = loadMediaMap(List.of(artistPostId)).getOrDefault(artistPostId, List.of());
        List<String> hashtags = hashtagService.findHashtagNamesByTargetIds(PostType.ARTIST_POST, List.of(artistPostId))
                .getOrDefault(artistPostId, List.of());
        return ArtistPostBaseResponse.from(row, media, hashtags);
    }

    public ArtistPostBaseResponse loadLatestArtistPostBase(Long artistId) {
        // 대시보드는 "최신 1건"만 보여 주는 요약 블록이므로
        // 목록 slice 전체를 만들지 않고 최신 row 하나만 골라 조립한다.
        ArtistPostReadRow row = artistPostRepository.findLatestRowByArtistId(artistId)
                .orElse(null);
        if (row == null) {
            return null;
        }

        List<ArtistPostMediaResponse> media = loadPreviewMediaMap(List.of(row.artistPostId()))
                .getOrDefault(row.artistPostId(), List.of());
        List<String> hashtags = hashtagService.findHashtagNamesByTargetIds(PostType.ARTIST_POST, List.of(row.artistPostId()))
                .getOrDefault(row.artistPostId(), List.of());
        return ArtistPostBaseResponse.from(row, media, hashtags);
    }

    public List<ArtistPostBaseResponse> loadLatestArtistPostBases(Long artistId, int limit) {
        // 구독 섹션은 artist별 최신 2건 정도만 필요하므로 고정 slice 캐시를 재사용하지 않고 즉시 조회한다.
        return buildBaseResponses(artistPostRepository.findSliceRowsByArtistId(artistId, null, limit));
    }

    public List<ArtistPostBaseResponse> loadLatestArtistPostBasesByWriterIds(Collection<Long> writerIds, int limit) {
        /*
         * 메인 홈 follow 섹션은 "artist별 묶음"이 아니라
         * "여러 writer의 글을 하나의 최신순 흐름으로 합친 목록" 이 필요하다.
         *
         * 그래서 여기서는 artistId 별 캐시 slice 를 재사용하지 않고
         * writer 집합을 기준으로 최신 row 를 한 번에 읽은 뒤
         * 공통 조립기(buildBaseResponses)로 base 응답을 만든다.
         */
        return buildBaseResponses(artistPostRepository.findLatestRowsByWriterIds(writerIds, limit));
    }

    public Map<Long, List<ArtistPostBaseResponse>> loadLatestArtistPostBaseMapByArtistIds(
            Collection<Long> artistIds,
            int perArtistLimit
    ) {
        /*
         * 이 메서드의 반환 타입이 List 가 아니라 Map 인 이유:
         *
         * 구독 섹션은 최종 응답이 "artist 하나 + 그 artist의 글들" 구조라서
         * 서비스 상위 계층에서 artistId 로 바로 꺼내 쓸 수 있는 모양이 더 편하다.
         *
         * 저장소는 artist 여러 개를 한 번에 읽어 오지만,
         * 여기서 다시 artistId 기준으로 묶어 Map 으로 바꿔 주면
         * MemberHomeDashboardService 쪽 조립 코드가 단순해진다.
         */
        if (artistIds == null || artistIds.isEmpty() || perArtistLimit < 1) {
            return Map.of();
        }

        List<ArtistPostBaseResponse> baseResponses = buildBaseResponses(
                artistPostRepository.findLatestRowsByArtistIds(artistIds, perArtistLimit)
        );
        if (baseResponses.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<ArtistPostBaseResponse>> baseResponseMap = new LinkedHashMap<>();
        for (ArtistPostBaseResponse baseResponse : baseResponses) {
            baseResponseMap.computeIfAbsent(baseResponse.artistId(), ignored -> new java.util.ArrayList<>())
                    .add(baseResponse);
        }
        return baseResponseMap;
    }

    private List<ArtistPostBaseResponse> buildBaseResponses(List<ArtistPostReadRow> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }

        // row 조회와 media/hashtag 조회를 분리해 둔 구조라서
        // 여기서 postId 배치 수집 -> 부가 데이터 일괄 조회 -> 최종 DTO 조립 순서가 반복된다.
        List<Long> postIds = rows.stream()
                .map(ArtistPostReadRow::artistPostId)
                .toList();
        Map<Long, List<ArtistPostMediaResponse>> mediaMap = loadPreviewMediaMap(postIds);
        Map<Long, List<String>> hashtagMap = hashtagService.findHashtagNamesByTargetIds(PostType.ARTIST_POST, postIds);

        return rows.stream()
                .map(row -> ArtistPostBaseResponse.from(
                        row,
                        mediaMap.getOrDefault(row.artistPostId(), List.of()),
                        hashtagMap.getOrDefault(row.artistPostId(), List.of())
                ))
                .toList();
    }

    private Map<Long, List<ArtistPostMediaResponse>> loadMediaMap(Collection<Long> artistPostIds) {
        if (artistPostIds.isEmpty()) {
            return Map.of();
        }

        return mediaRepository.findByTargetTypeAndTargetIdInOrderByTargetIdAscSortOrderAsc(PostType.ARTIST_POST, artistPostIds)
                .stream()
                .collect(Collectors.groupingBy(
                        Media::getTargetId,
                        Collectors.mapping(ArtistPostMediaResponse::from, Collectors.toList())
                ));
    }

    private Map<Long, List<ArtistPostMediaResponse>> loadPreviewMediaMap(Collection<Long> artistPostIds) {
        if (artistPostIds.isEmpty()) {
            return Map.of();
        }

        // 목록은 UI 정책상 앞 6개 preview만 내리고, 전체 개수는 mediaCount로 표현한다.
        return mediaRepository.findPreviewByTargetTypeAndTargetIdInOrderByTargetIdAscSortOrderAsc(
                        PostType.ARTIST_POST,
                        artistPostIds,
                        ARTIST_POST_LIST_MEDIA_PREVIEW_LIMIT
                )
                .stream()
                .collect(Collectors.groupingBy(
                        Media::getTargetId,
                        Collectors.mapping(ArtistPostMediaResponse::from, Collectors.toList())
                ));
    }
}
