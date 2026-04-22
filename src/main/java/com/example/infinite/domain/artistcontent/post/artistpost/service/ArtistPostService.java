package com.example.infinite.domain.artistcontent.post.artistpost.service;

import com.example.infinite.domain.artistcontent.comment.service.CommentService;
import com.example.infinite.domain.artistcontent.media.entity.Media;
import com.example.infinite.domain.artistcontent.media.repository.MediaRepository;
import com.example.infinite.domain.artistcontent.media.service.MediaService;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.request.ArtistPostCreateRequest;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.request.ArtistPostUpdateRequest;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostCreateResponse;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostDetailResponse;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostMediaResponse;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostReadRow;
import com.example.infinite.domain.artistcontent.post.artistpost.dto.response.ArtistPostResponse;
import com.example.infinite.domain.artistcontent.post.artistpost.entity.ArtistPost;
import com.example.infinite.domain.artistcontent.post.artistpost.repository.ArtistPostRepository;
import com.example.infinite.domain.artistcontent.post.artistpost.support.ArtistPostReader;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import com.example.infinite.domain.artistcontent.hashtag.service.HashtagService;
import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.artist.repository.ArtistMemberRepository;
import com.example.infinite.domain.member.artist.support.ArtistReader;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.enums.MemberRole;
import com.example.infinite.domain.member.member.support.MemberInputSupport;
import com.example.infinite.domain.member.member.support.MemberReader;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
// ArtistPostService 는 FanPost 패턴을 재사용하되,
// "공식 아티스트 멤버만 작성 가능"이라는 권한 규칙과 artist badge 응답을 추가로 책임진다.
public class ArtistPostService {

    private static final int ARTIST_POST_SLICE_SIZE = 10;
    private static final int ARTIST_POST_LIST_MEDIA_PREVIEW_LIMIT = 6;

    private final ArtistPostRepository artistPostRepository;
    private final MediaRepository mediaRepository;
    private final MemberReader memberReader;
    private final ArtistReader artistReader;
    private final ArtistPostReader artistPostReader;
    private final ArtistMemberRepository artistMemberRepository;
    private final HashtagService hashtagService;
    private final CommentService commentService;
    private final MediaService mediaService;

    @Transactional
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
        // 작성 후에는 hashtag/media 를 FanPost와 같은 순서로 동기화한다.
        hashtagService.syncHashtags(PostType.ARTIST_POST, artistPost.getId(), artistPost.getContent());
        mediaService.attachArtistPostMedia(artistId, artistPost, request.getFiles());

        return ArtistPostCreateResponse.from(artistPost);
    }

    public CursorSliceResponse<ArtistPostResponse> getArtistPosts(Long artistId, Long cursor) {
        artistReader.findArtistByIdOrThrow(artistId);

        List<ArtistPostReadRow> rows = artistPostRepository.findSliceRowsByArtistId(
                artistId,
                cursor,
                ARTIST_POST_SLICE_SIZE + 1
        );
        boolean hasNext = rows.size() > ARTIST_POST_SLICE_SIZE;
        List<ArtistPostReadRow> visibleRows = hasNext ? rows.subList(0, ARTIST_POST_SLICE_SIZE) : rows;

        // 본문 row만 먼저 읽고 media/hashtag를 배치로 붙여 N+1을 피한다.
        return toCursorSliceResponse(visibleRows, hasNext);
    }

    public ArtistPostDetailResponse getArtistPost(Long artistId, Long artistPostId, Long commentCursor) {
        artistReader.findArtistByIdOrThrow(artistId);

        // 상세는 게시글 1건 조립 결과 위에 댓글 슬라이스를 덧붙이는 구조다.
        ArtistPostResponse artistPostResponse = buildArtistPostResponse(artistId, artistPostId);
        return ArtistPostDetailResponse.from(
                artistPostResponse,
                commentService.getArtistPostComments(artistId, artistPostId, commentCursor)
        );
    }

    @Transactional
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

        return buildArtistPostResponse(artistId, artistPostId);
    }

    @Transactional
    public void delete(MemberDetailsImpl memberDetails, Long artistId, Long artistPostId) {
        Member writer = findAuthorizedArtistWriter(memberDetails, artistId);
        ArtistPost artistPost = findOwnedArtistPost(writer.getId(), artistId, artistPostId);

        hashtagService.syncHashtags(PostType.ARTIST_POST, artistPost.getId(), null);
        mediaService.deleteArtistPostMedia(artistPost);
        artistPost.delete();
    }

    private CursorSliceResponse<ArtistPostResponse> toCursorSliceResponse(List<ArtistPostReadRow> visibleRows, boolean hasNext) {
        if (visibleRows.isEmpty()) {
            return new CursorSliceResponse<>(List.of(), null, hasNext, ARTIST_POST_SLICE_SIZE);
        }

        List<Long> postIds = extractPostIds(visibleRows);
        Map<Long, List<ArtistPostMediaResponse>> mediaMap = loadPreviewMediaMap(postIds);
        Map<Long, List<String>> hashtagMap = hashtagService.findHashtagNamesByTargetIds(PostType.ARTIST_POST, postIds);

        List<ArtistPostResponse> responses = visibleRows.stream()
                .map(row -> ArtistPostResponse.from(
                        row,
                        mediaMap.getOrDefault(row.artistPostId(), List.of()),
                        hashtagMap.getOrDefault(row.artistPostId(), List.of())
                ))
                .toList();

        Long nextCursor = hasNext && !visibleRows.isEmpty()
                ? visibleRows.get(visibleRows.size() - 1).artistPostId()
                : null;

        return new CursorSliceResponse<>(responses, nextCursor, hasNext, ARTIST_POST_SLICE_SIZE);
    }

    private ArtistPostResponse buildArtistPostResponse(Long artistId, Long artistPostId) {
        ArtistPostReadRow row = artistPostRepository.findDetailRowByArtistIdAndArtistPostId(artistId, artistPostId)
                .orElseThrow(() -> new ArtistContentException(ArtistContentErrorCode.POST_NOT_FOUND));
        // 상세 1건도 목록과 같은 media/hashtag 조립 규칙을 재사용해 응답 일관성을 유지한다.
        List<ArtistPostMediaResponse> media = loadMediaMap(List.of(artistPostId)).getOrDefault(artistPostId, List.of());
        List<String> hashtags = hashtagService.findHashtagNamesByTargetIds(PostType.ARTIST_POST, List.of(artistPostId))
                .getOrDefault(artistPostId, List.of());
        return ArtistPostResponse.from(row, media, hashtags);
    }

    private List<Long> extractPostIds(List<ArtistPostReadRow> rows) {
        return rows.stream()
                .map(ArtistPostReadRow::artistPostId)
                .toList();
    }

    private Map<Long, List<ArtistPostMediaResponse>> loadMediaMap(Collection<Long> artistPostIds) {
        if (artistPostIds.isEmpty()) {
            return Map.of();
        }

        // ArtistPost도 Media 공통 테이블을 targetType 기반으로 묶어 조회한다.
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
        // 목록 카드는 post당 앞쪽 6장만 미리보기로 내리고,
        // 전체 장수는 mediaCount 필드를 그대로 사용한다.
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

    private ArtistPost findOwnedArtistPost(Long memberId, Long artistId, Long artistPostId) {
        ArtistPost artistPost = artistPostReader.findByIdAndArtistIdOrThrow(artistPostId, artistId);
        // 공식 계정 글이라도 수정/삭제는 실제 작성자 본인만 허용한다.
        if (!artistPost.getWriter().getId().equals(memberId)) {
            throw new ArtistContentException(ArtistContentErrorCode.POST_PERMISSION_DENIED);
        }
        return artistPost;
    }

    private Member findAuthorizedArtistWriter(MemberDetailsImpl memberDetails, Long artistId) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        // ArtistPost 작성/수정/삭제는 "ARTIST 역할"과 "해당 artist 소속 artist member"를 둘 다 만족해야 한다.
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
        // 사진/영상만 있는 공식 게시글도 허용하기 위해 본문은 optional 로 둔다.
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

        // 수정도 생성과 같은 불변조건을 지켜야 한다.
        // 기존 media를 모두 비우는 요청이라면 최종 본문이 비어 있지 않아야 한다.
        if (!hasContent && !hasMediaAfterUpdate) {
            throw new ArtistContentException(ArtistContentErrorCode.POST_CONTENT_OR_MEDIA_REQUIRED);
        }
    }
}
