package com.example.infinite.domain.artistcontent.post.artistpost.service;

import com.example.infinite.domain.artistcontent.comment.service.CommentService;
import com.example.infinite.domain.artistcontent.hashtag.service.HashtagService;
import com.example.infinite.domain.artistcontent.media.service.MediaService;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.request.ArtistPostCreateRequest;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.request.ArtistPostUpdateRequest;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostBaseResponse;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostCreateResponse;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostDetailResponse;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostResponse;
import com.example.infinite.domain.artistcontent.post.artistpost.entity.ArtistPost;
import com.example.infinite.domain.artistcontent.post.artistpost.repository.ArtistPostRepository;
import com.example.infinite.domain.artistcontent.post.artistpost.support.ArtistPostReader;
import com.example.infinite.domain.artistcontent.post.cache.PostHotData;
import com.example.infinite.domain.artistcontent.post.cache.PostHotDataCacheService;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import com.example.infinite.domain.artistcontent.post.enums.PostType;
import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.artist.repository.ArtistMemberRepository;
import com.example.infinite.domain.member.artist.support.ArtistReader;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.enums.MemberRole;
import com.example.infinite.domain.member.member.support.MemberInputSupport;
import com.example.infinite.domain.member.member.support.MemberReader;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.constant.CacheNames;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
// ArtistPost는 동시성 설계가 가장 복잡한 영역이므로, 캐시도 base/hot 분리를 명시적으로 드러낸다.
// 이렇게 해야 "본문은 길게, count는 flush 주기와 맞춘 짧은 TTL" 전략을 설명하기 쉽다.
public class ArtistPostService {

    private final ArtistPostRepository artistPostRepository;
    private final MemberReader memberReader;
    private final ArtistReader artistReader;
    private final ArtistPostReader artistPostReader;
    private final ArtistMemberRepository artistMemberRepository;
    private final HashtagService hashtagService;
    private final CommentService commentService;
    private final MediaService mediaService;
    private final ArtistPostBaseCacheService artistPostBaseCacheService;
    private final PostHotDataCacheService postHotDataCacheService;

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.ARTIST_POST_LIST_BASE, allEntries = true),
            @CacheEvict(value = CacheNames.ARTIST_POST_DETAIL_BASE, allEntries = true)
    })
    public ArtistPostCreateResponse create(
            MemberDetailsImpl memberDetails,
            Long artistId,
            ArtistPostCreateRequest request
    ) {
        Artist artist = artistReader.findArtistByIdOrThrow(artistId);
        Member writer = findAuthorizedArtistWriter(memberDetails, artistId);
        validateCreateRequest(request);

        ArtistPost artistPost = artistPostRepository.save(ArtistPost.create(
                artist,
                writer,
                normalizeContent(request.getContent())
        ));
        hashtagService.syncHashtags(PostType.ARTIST_POST, artistPost.getId(), artistPost.getContent());
        mediaService.attachArtistPostMedia(artistId, artistPost, request.getFiles());

        return ArtistPostCreateResponse.from(artistPost);
    }

    public CursorSliceResponse<ArtistPostResponse> getArtistPosts(Long artistId, Long cursor) {
        artistReader.findArtistByIdOrThrow(artistId);

        CursorSliceResponse<ArtistPostBaseResponse> baseSlice = artistPostBaseCacheService.getArtistPostBaseSlice(artistId, cursor);
        Map<Long, PostHotData> hotDataByPostId = postHotDataCacheService.getPostHotDataMap(
                PostType.ARTIST_POST,
                baseSlice.content().stream().map(ArtistPostBaseResponse::artistPostId).toList()
        );

        List<ArtistPostResponse> content = baseSlice.content().stream()
                .map(baseResponse -> ArtistPostResponse.from(
                        baseResponse,
                        hotDataByPostId.getOrDefault(baseResponse.artistPostId(), PostHotData.empty())
                ))
                .toList();
        return new CursorSliceResponse<>(content, baseSlice.nextCursor(), baseSlice.hasNext(), baseSlice.size());
    }

    public ArtistPostResponse getLatestArtistPost(Long artistId) {
        artistReader.findArtistByIdOrThrow(artistId);

        // 아티스트 홈 하이라이트는 최신 1건만 쓰므로
        // 목록 10개 slice 대신 최신 row 1건만 읽어 조립한다.
        ArtistPostBaseResponse baseResponse = artistPostBaseCacheService.loadLatestArtistPostBase(artistId);
        if (baseResponse == null) {
            return null;
        }

        PostHotData hotData = postHotDataCacheService.getPostHotData(PostType.ARTIST_POST, baseResponse.artistPostId());
        return ArtistPostResponse.from(baseResponse, hotData);
    }

    public List<ArtistPostResponse> getLatestArtistPosts(Long artistId, int limit) {
        // 메인 홈은 cursor slice 전체가 아니라 artist별 최신 몇 건만 필요하다.
        artistReader.findArtistByIdOrThrow(artistId);
        return mapToArtistPostResponses(artistPostBaseCacheService.loadLatestArtistPostBases(artistId, limit));
    }

    public List<ArtistPostResponse> getLatestArtistPostsByWriterIds(Collection<Long> writerIds, int limit) {
        if (writerIds == null || writerIds.isEmpty()) {
            return List.of();
        }
        // 팔로우 섹션은 여러 writer의 글을 하나의 최신순 피드로 합쳐 보여 준다.
        return mapToArtistPostResponses(artistPostBaseCacheService.loadLatestArtistPostBasesByWriterIds(writerIds, limit));
    }

    public Map<Long, List<ArtistPostResponse>> getLatestArtistPostsByArtistIds(
            Collection<Long> artistIds,
            int perArtistLimit
    ) {
        if (artistIds == null || artistIds.isEmpty() || perArtistLimit < 1) {
            return Map.of();
        }

        List<Long> distinctArtistIds = new java.util.ArrayList<>(new LinkedHashSet<>(artistIds));
        Map<Long, List<ArtistPostBaseResponse>> baseResponseMap =
                artistPostBaseCacheService.loadLatestArtistPostBaseMapByArtistIds(distinctArtistIds, perArtistLimit);
        if (baseResponseMap.isEmpty()) {
            return Map.of();
        }

        Map<Long, PostHotData> hotDataByPostId = postHotDataCacheService.getPostHotDataMap(
                PostType.ARTIST_POST,
                baseResponseMap.values().stream()
                        .flatMap(List::stream)
                        .map(ArtistPostBaseResponse::artistPostId)
                        .toList()
        );

        Map<Long, List<ArtistPostResponse>> result = new LinkedHashMap<>();
        for (Long artistId : distinctArtistIds) {
            List<ArtistPostBaseResponse> baseResponses = baseResponseMap.get(artistId);
            if (baseResponses == null || baseResponses.isEmpty()) {
                continue;
            }

            List<ArtistPostResponse> responses = baseResponses.stream()
                    .map(baseResponse -> ArtistPostResponse.from(
                            baseResponse,
                            hotDataByPostId.getOrDefault(baseResponse.artistPostId(), PostHotData.empty())
                    ))
                    .toList();
            result.put(artistId, responses);
        }
        return result;
    }

    public ArtistPostDetailResponse getArtistPost(Long artistId, Long artistPostId, Long commentCursor) {
        artistReader.findArtistByIdOrThrow(artistId);

        ArtistPostBaseResponse baseResponse = artistPostBaseCacheService.getArtistPostBaseDetail(artistId, artistPostId);
        PostHotData hotData = postHotDataCacheService.getPostHotData(PostType.ARTIST_POST, artistPostId);
        ArtistPostResponse artistPostResponse = ArtistPostResponse.from(baseResponse, hotData);
        return ArtistPostDetailResponse.from(
                artistPostResponse,
                commentService.getArtistPostComments(artistId, artistPostId, commentCursor)
        );
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.ARTIST_POST_LIST_BASE, allEntries = true),
            @CacheEvict(value = CacheNames.ARTIST_POST_DETAIL_BASE, allEntries = true)
    })
    public ArtistPostResponse update(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long artistPostId,
            ArtistPostUpdateRequest request
    ) {
        Member writer = findAuthorizedArtistWriter(memberDetails, artistId);
        ArtistPost artistPost = findOwnedArtistPost(writer.getId(), artistId, artistPostId);

        String resolvedContent = resolveUpdatedContent(request.getContent(), artistPost.getContent());
        validateUpdateRequest(resolvedContent, artistPost, request);
        artistPost.update(resolvedContent);
        hashtagService.syncHashtags(PostType.ARTIST_POST, artistPost.getId(), artistPost.getContent());
        if (request.getFiles() != null) {
            mediaService.replaceArtistPostMedia(artistId, artistPost, request.getFiles());
        }

        return buildCurrentArtistPostResponse(
                artistId,
                artistPostId,
                PostHotData.of(artistPost.getLikeCount(), artistPost.getCommentCount())
        );
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.ARTIST_POST_LIST_BASE, allEntries = true),
            @CacheEvict(value = CacheNames.ARTIST_POST_DETAIL_BASE, allEntries = true)
    })
    public void delete(MemberDetailsImpl memberDetails, Long artistId, Long artistPostId) {
        Member writer = findAuthorizedArtistWriter(memberDetails, artistId);
        ArtistPost artistPost = findOwnedArtistPost(writer.getId(), artistId, artistPostId);

        hashtagService.syncHashtags(PostType.ARTIST_POST, artistPost.getId(), null);
        mediaService.deleteArtistPostMedia(artistPost);
        artistPost.delete();
    }

    private ArtistPostResponse buildCurrentArtistPostResponse(Long artistId, Long artistPostId, PostHotData hotData) {
        ArtistPostBaseResponse baseResponse = artistPostBaseCacheService.loadArtistPostBaseDetail(artistId, artistPostId);
        return ArtistPostResponse.from(baseResponse, hotData);
    }

    private List<ArtistPostResponse> mapToArtistPostResponses(List<ArtistPostBaseResponse> baseResponses) {
        if (baseResponses.isEmpty()) {
            return List.of();
        }

        Map<Long, PostHotData> hotDataByPostId = postHotDataCacheService.getPostHotDataMap(
                PostType.ARTIST_POST,
                baseResponses.stream().map(ArtistPostBaseResponse::artistPostId).toList()
        );

        return baseResponses.stream()
                .map(baseResponse -> ArtistPostResponse.from(
                        baseResponse,
                        hotDataByPostId.getOrDefault(baseResponse.artistPostId(), PostHotData.empty())
                ))
                .toList();
    }

    private ArtistPost findOwnedArtistPost(Long memberId, Long artistId, Long artistPostId) {
        ArtistPost artistPost = artistPostReader.findByIdAndArtistIdOrThrow(artistPostId, artistId);
        if (!artistPost.getWriter().getId().equals(memberId)) {
            throw new ArtistContentException(ArtistContentErrorCode.POST_PERMISSION_DENIED);
        }
        return artistPost;
    }

    private Member findAuthorizedArtistWriter(MemberDetailsImpl memberDetails, Long artistId) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        if (member.getRole() != MemberRole.ARTIST || !artistMemberRepository.existsByArtistIdAndMemberId(artistId, member.getId())) {
            throw new ArtistContentException(ArtistContentErrorCode.POST_PERMISSION_DENIED);
        }
        return member;
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

    private void validateCreateRequest(ArtistPostCreateRequest request) {
        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
        boolean hasFiles = request.getFiles() != null && !request.getFiles().isEmpty();

        if (!hasContent && !hasFiles) {
            throw new ArtistContentException(ArtistContentErrorCode.POST_CONTENT_OR_MEDIA_REQUIRED);
        }
    }

    private void validateUpdateRequest(String resolvedContent, ArtistPost artistPost, ArtistPostUpdateRequest request) {
        boolean hasContent = resolvedContent != null && !resolvedContent.isBlank();
        boolean hasMediaAfterUpdate = request.getFiles() == null
                ? artistPost.getMediaCount() > 0
                : !request.getFiles().isEmpty();

        if (!hasContent && !hasMediaAfterUpdate) {
            throw new ArtistContentException(ArtistContentErrorCode.POST_CONTENT_OR_MEDIA_REQUIRED);
        }
    }
}
