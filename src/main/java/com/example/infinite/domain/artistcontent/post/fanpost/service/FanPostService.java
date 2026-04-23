package com.example.infinite.domain.artistcontent.post.fanpost.service;

import com.example.infinite.domain.artistcontent.comment.service.CommentService;
import com.example.infinite.domain.artistcontent.hashtag.service.HashtagService;
import com.example.infinite.domain.artistcontent.media.service.MediaService;
import com.example.infinite.domain.artistcontent.post.cache.PostHotData;
import com.example.infinite.domain.artistcontent.post.cache.PostHotDataCacheService;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.request.FanPostCreateRequest;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.request.FanPostUpdateRequest;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostBaseResponse;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostCreateResponse;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostDetailResponse;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostHotBaseSlice;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostResponse;
import com.example.infinite.domain.artistcontent.post.fanpost.entity.FanPost;
import com.example.infinite.domain.artistcontent.post.fanpost.repository.FanPostRepository;
import com.example.infinite.domain.artistcontent.post.fanpost.support.FanPostReader;
import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.artist.support.ArtistReader;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.support.MemberInputSupport;
import com.example.infinite.domain.member.member.support.MemberReader;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.constant.CacheNames;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import com.example.infinite.global.common.dto.ScoreCursorSliceResponse;
import com.example.infinite.global.common.util.querydsl.CursorSliceUtils;
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
// FanPost는 읽기 경로에서 base/hot을 분리한다.
// 본문/미디어/해시태그/작성자 정보는 base 캐시, like/comment count는 hot 캐시로 나눠 수명을 다르게 둔다.
public class FanPostService {

    private static final int HOT_FAN_POST_DEFAULT_SIZE = 10;
    private static final int HOT_FAN_POST_MAX_SIZE = 30;
    private static final long HOT_LOOKBACK_HOURS = 24L;
    private static final long HOT_MIN_LIKE_COUNT = 5L;
    private static final long HOT_MIN_COMMENT_COUNT = 5L;

    private final FanPostRepository fanPostRepository;
    private final MemberReader memberReader;
    private final ArtistReader artistReader;
    private final FanPostReader fanPostReader;
    private final HashtagService hashtagService;
    private final CommentService commentService;
    private final MediaService mediaService;
    private final FanPostBaseCacheService fanPostBaseCacheService;
    private final PostHotDataCacheService postHotDataCacheService;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.FAN_POST_LIST_BASE, allEntries = true),
            @CacheEvict(value = CacheNames.FAN_POST_DETAIL_BASE, allEntries = true)
    })
    public FanPostCreateResponse create(MemberDetailsImpl memberDetails, Long artistId, FanPostCreateRequest request) {
        Member writer = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        Artist artist = artistReader.findArtistByIdOrThrow(artistId);
        validateCreateRequest(request);

        FanPost fanPost = fanPostRepository.save(FanPost.create(
                artist,
                writer,
                normalizeContent(request.getContent())
        ));
        hashtagService.syncHashtags(PostType.FAN_POST, fanPost.getId(), fanPost.getContent());
        mediaService.attachFanPostMedia(artistId, fanPost, request.getFiles());

        return FanPostCreateResponse.from(fanPost);
    }

    public CursorSliceResponse<FanPostResponse> getFanPosts(Long artistId, Long cursor) {
        artistReader.findArtistByIdOrThrow(artistId);

        CursorSliceResponse<FanPostBaseResponse> baseSlice = fanPostBaseCacheService.getFanPostBaseSlice(artistId, cursor);
        Map<Long, PostHotData> hotDataByPostId = postHotDataCacheService.getPostHotDataMap(
                PostType.FAN_POST,
                baseSlice.content().stream().map(FanPostBaseResponse::fanPostId).toList()
        );

        // 읽기 응답은 마지막에만 base + hot을 합친다.
        List<FanPostResponse> content = baseSlice.content().stream()
                .map(baseResponse -> FanPostResponse.from(
                        baseResponse,
                        hotDataByPostId.getOrDefault(baseResponse.fanPostId(), PostHotData.empty())
                ))
                .toList();
        return new CursorSliceResponse<>(content, baseSlice.nextCursor(), baseSlice.hasNext(), baseSlice.size());
    }

    public ScoreCursorSliceResponse<FanPostResponse> getHotFanPosts(
            Long artistId,
            Long scoreCursor,
            Long idCursor,
            Integer size
    ) {
        artistReader.findArtistByIdOrThrow(artistId);
        int resolvedSize = CursorSliceUtils.resolveLimit(size, HOT_FAN_POST_DEFAULT_SIZE, HOT_FAN_POST_MAX_SIZE);

        // FanPost HOT은 좋아요/댓글 반응이 활발하고 점수 변동도 크다.
        // 그래서 offset 대신 (score, id) 복합커서를 써서 정렬 기준과 페이지 기준을 일치시킨다.
        FanPostHotBaseSlice baseSlice = fanPostBaseCacheService.loadHotFanPostBaseSlice(
                artistId,
                LocalDateTime.now().minusHours(HOT_LOOKBACK_HOURS),
                scoreCursor,
                idCursor,
                resolvedSize,
                HOT_MIN_LIKE_COUNT,
                HOT_MIN_COMMENT_COUNT
        );
        Map<Long, PostHotData> hotDataByPostId = postHotDataCacheService.getPostHotDataMap(
                PostType.FAN_POST,
                baseSlice.content().stream().map(FanPostBaseResponse::fanPostId).toList()
        );

        // repository가 score/id 순서를 확정하고,
        // service는 그 순서를 보존한 채 hot count만 합쳐 최종 카드 응답을 만든다.
        List<FanPostResponse> content = baseSlice.content().stream()
                .map(baseResponse -> FanPostResponse.from(
                        baseResponse,
                        hotDataByPostId.getOrDefault(baseResponse.fanPostId(), PostHotData.empty())
                ))
                .toList();

        return new ScoreCursorSliceResponse<>(
                content,
                baseSlice.nextScoreCursor(),
                baseSlice.nextIdCursor(),
                baseSlice.hasNext(),
                baseSlice.size()
        );
    }

    public FanPostDetailResponse getFanPost(Long artistId, Long fanPostId, Long commentCursor) {
        artistReader.findArtistByIdOrThrow(artistId);

        FanPostBaseResponse baseResponse = fanPostBaseCacheService.getFanPostBaseDetail(artistId, fanPostId);
        PostHotData hotData = postHotDataCacheService.getPostHotData(PostType.FAN_POST, fanPostId);
        FanPostResponse fanPostResponse = FanPostResponse.from(baseResponse, hotData);
        return FanPostDetailResponse.from(
                fanPostResponse,
                commentService.getFanPostComments(artistId, fanPostId, commentCursor)
        );
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.FAN_POST_LIST_BASE, allEntries = true),
            @CacheEvict(value = CacheNames.FAN_POST_DETAIL_BASE, allEntries = true)
    })
    public FanPostResponse update(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long fanPostId,
            FanPostUpdateRequest request
    ) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        FanPost fanPost = findOwnedFanPost(member.getId(), artistId, fanPostId);

        fanPost.update(resolveUpdatedContent(request.getContent(), fanPost.getContent()));
        hashtagService.syncHashtags(PostType.FAN_POST, fanPost.getId(), fanPost.getContent());
        if (request.getFiles() != null) {
            mediaService.replaceFanPostMedia(artistId, fanPost, request.getFiles());
        }

        // write 응답은 캐시를 다시 채우지 않고, 현재 트랜잭션 상태를 기준으로 즉석 조립한다.
        return buildCurrentFanPostResponse(
                artistId,
                fanPostId,
                PostHotData.of(fanPost.getLikeCount(), fanPost.getCommentCount())
        );
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.FAN_POST_LIST_BASE, allEntries = true),
            @CacheEvict(value = CacheNames.FAN_POST_DETAIL_BASE, allEntries = true)
    })
    public void delete(MemberDetailsImpl memberDetails, Long artistId, Long fanPostId) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        FanPost fanPost = findOwnedFanPost(member.getId(), artistId, fanPostId);
        hashtagService.syncHashtags(PostType.FAN_POST, fanPost.getId(), null);
        mediaService.deleteFanPostMedia(fanPost);
        fanPost.delete();
    }

    private FanPostResponse buildCurrentFanPostResponse(Long artistId, Long fanPostId, PostHotData hotData) {
        FanPostBaseResponse baseResponse = fanPostBaseCacheService.loadFanPostBaseDetail(artistId, fanPostId);
        return FanPostResponse.from(baseResponse, hotData);
    }

    private FanPost findOwnedFanPost(Long memberId, Long artistId, Long fanPostId) {
        FanPost fanPost = fanPostReader.findByIdAndArtistIdOrThrow(fanPostId, artistId);
        if (!fanPost.getWriter().getId().equals(memberId)) {
            throw new ArtistContentException(ArtistContentErrorCode.POST_PERMISSION_DENIED);
        }
        return fanPost;
    }

    private String resolveUpdatedContent(String requestedContent, String currentContent) {
        if (requestedContent == null) {
            return currentContent;
        }
        return normalizeContent(requestedContent);
    }

    private String normalizeContent(String content) {
        return content == null ? "" : content;
    }

    private void validateCreateRequest(FanPostCreateRequest request) {
        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
        boolean hasFiles = request.getFiles() != null && !request.getFiles().isEmpty();

        if (!hasContent && !hasFiles) {
            throw new IllegalArgumentException("팬포스트는 본문 또는 첨부파일 중 하나가 필요합니다.");
        }
    }
}
