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
        Member writer = findAuthorizedArtistWriter(memberDetails, artistId);
        Artist artist = artistReader.findArtistByIdOrThrow(artistId);
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

        return toCursorSliceResponse(visibleRows, hasNext);
    }

    public ArtistPostDetailResponse getArtistPost(Long artistId, Long artistPostId, Long commentCursor) {
        artistReader.findArtistByIdOrThrow(artistId);

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

        artistPost.update(resolveUpdatedContent(request.getContent(), artistPost.getContent()));
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
        Map<Long, List<ArtistPostMediaResponse>> mediaMap = loadMediaMap(postIds);
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

    private ArtistPost findOwnedArtistPost(Long memberId, Long artistId, Long artistPostId) {
        ArtistPost artistPost = artistPostReader.findByIdAndArtistIdOrThrow(artistPostId, artistId);
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
            throw new IllegalArgumentException("아티스트포스트는 본문 또는 첨부파일 중 하나가 필요합니다.");
        }
    }
}
