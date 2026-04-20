package com.example.infinite.domain.artistcontent.post.fanpost.service;

import com.example.infinite.domain.artistcontent.media.entity.Media;
import com.example.infinite.domain.artistcontent.media.repository.MediaRepository;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import com.example.infinite.domain.artistcontent.post.eunms.PostType;
import com.example.infinite.domain.artistcontent.hashtag.service.HashtagService;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.request.FanPostCreateRequest;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.request.FanPostUpdateRequest;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostCreateResponse;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostMediaResponse;
import com.example.infinite.domain.artistcontent.post.fanpost.dto.response.FanPostReadRow;
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
public class FanPostService {

    // 팬 게시글 목록은 10개 고정 cursor slice 정책을 사용한다.
    private static final int FAN_POST_SLICE_SIZE = 10;

    private final FanPostRepository fanPostRepository;
    private final MediaRepository mediaRepository;
    private final MemberReader memberReader;
    private final ArtistReader artistReader;
    private final FanPostReader fanPostReader;
    private final HashtagService hashtagService;

    @Transactional
    public FanPostCreateResponse create(MemberDetailsImpl memberDetails, Long artistId, FanPostCreateRequest request) {
        // 로그인 작성자와 대상 artist를 먼저 확정한 뒤 게시글을 저장한다.
        Member writer = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        Artist artist = artistReader.findArtistByIdOrThrow(artistId);

        FanPost fanPost = fanPostRepository.save(FanPost.create(
                artist,
                writer,
                normalizeContent(request.content())
        ));
        // 팬포스트 id가 생성된 뒤에야 content_hashtag.target_id를 채울 수 있으므로 저장 직후 sync를 호출한다.
        hashtagService.syncHashtags(PostType.FAN_POST, fanPost.getId(), fanPost.getContent());

        return FanPostCreateResponse.from(fanPost);
    }

    public CursorSliceResponse<FanPostResponse> getFanPosts(Long artistId, Long cursor) {
        // 잘못된 artistId에 대한 조회를 초기에 차단한다.
        artistReader.findArtistByIdOrThrow(artistId);

        // limit + 1 방식으로 다음 slice 존재 여부를 계산한다.
        List<FanPostReadRow> rows = fanPostRepository.findSliceRowsByArtistId(
                artistId,
                cursor,
                FAN_POST_SLICE_SIZE + 1
        );
        // 11번째 row는 hasNext 판단용으로만 쓰고, 실제 media 조회와 DTO 조립은 노출할 10개에만 수행한다.
        boolean hasNext = rows.size() > FAN_POST_SLICE_SIZE;
        List<FanPostReadRow> visibleRows = hasNext ? rows.subList(0, FAN_POST_SLICE_SIZE) : rows;

        // 본문 row와 media를 분리 조회한 뒤 서비스에서 합쳐 N+1 없이 응답을 조립한다.
        return toCursorSliceResponse(visibleRows, hasNext);
    }

    public FanPostResponse getFanPost(Long artistId, Long fanPostId) {
        // 상세 조회도 artist 소속을 함께 확인해 다른 커뮤니티 글을 잘못 읽지 않게 한다.
        artistReader.findArtistByIdOrThrow(artistId);

        FanPostReadRow row = fanPostRepository.findDetailRowByArtistIdAndFanPostId(artistId, fanPostId)
                .orElseThrow(() -> new ArtistContentException(ArtistContentErrorCode.POST_NOT_FOUND));
        // 상세는 단건이지만 media 조합 방식은 목록과 동일하게 유지해 응답 구조를 맞춘다.
        List<FanPostMediaResponse> media = loadMediaMap(List.of(fanPostId)).getOrDefault(fanPostId, List.of());
        List<String> hashtags = hashtagService.findHashtagNamesByTargetIds(PostType.FAN_POST, List.of(fanPostId))
                .getOrDefault(fanPostId, List.of());

        return FanPostResponse.from(row, media, hashtags);
    }

    @Transactional
    public FanPostResponse update(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long fanPostId,
            FanPostUpdateRequest request
    ) {
        // 수정은 작성자 본인 글인지 먼저 확인한 뒤 본문만 변경한다.
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        FanPost fanPost = findOwnedFanPost(member.getId(), artistId, fanPostId);

        fanPost.update(resolveUpdatedContent(request.content(), fanPost.getContent()));
        // 수정은 "기존 매핑 제거 -> 수정된 본문 기준 재생성" 규칙을 그대로 따른다.
        hashtagService.syncHashtags(PostType.FAN_POST, fanPost.getId(), fanPost.getContent());

        return getFanPost(artistId, fanPostId);
    }

    @Transactional
    public void delete(MemberDetailsImpl memberDetails, Long artistId, Long fanPostId) {
        // 삭제도 동일한 소유자 검증 흐름을 재사용한다.
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        FanPost fanPost = findOwnedFanPost(member.getId(), artistId, fanPostId);
        // soft delete 전에 매핑을 먼저 비워야 usageCount와 hashtag 검색 결과가 즉시 일치한다.
        hashtagService.syncHashtags(PostType.FAN_POST, fanPost.getId(), null);
        fanPost.delete();
    }

    private FanPost findOwnedFanPost(Long memberId, Long artistId, Long fanPostId) {
        FanPost fanPost = fanPostReader.findByIdAndArtistIdOrThrow(fanPostId, artistId);

        // 팬 게시글 수정/삭제는 작성자 본인만 허용한다.
        if (!fanPost.getWriter().getId().equals(memberId)) {
            throw new ArtistContentException(ArtistContentErrorCode.POST_PERMISSION_DENIED);
        }
        return fanPost;
    }

    private String resolveUpdatedContent(String requestedContent, String currentContent) {
        // null이면 부분 수정 요청으로 보고 기존 본문을 유지한다.
        if (requestedContent == null) {
            return currentContent;
        }
        return normalizeContent(requestedContent);
    }

    private String normalizeContent(String content) {
        // 사진/영상만 있는 글을 허용하기 위해 본문은 선택값으로 두고, 없으면 빈 문자열로 저장한다.
        // TODO: media write path가 붙으면 "본문 또는 미디어 중 하나는 반드시 존재" 규칙으로 검증한다.
        return content == null ? "" : content;
    }

    private List<Long> extractPostIds(List<FanPostReadRow> rows) {
        // media batch 조회를 위해 현재 slice에 포함된 게시글 id만 추출한다.
        return rows.stream()
                .map(FanPostReadRow::fanPostId)
                .toList();
    }

    private CursorSliceResponse<FanPostResponse> toCursorSliceResponse(List<FanPostReadRow> visibleRows, boolean hasNext) {
        List<Long> postIds = extractPostIds(visibleRows);
        // 응답 조립은 "본문 row / media / hashtag"를 각각 배치 조회한 뒤 메모리에서 합친다.
        // 이렇게 해야 게시글별 media/hashtag 개별 조회로 인한 N+1을 피할 수 있다.
        Map<Long, List<FanPostMediaResponse>> mediaMap = loadMediaMap(postIds);
        Map<Long, List<String>> hashtagMap = hashtagService.findHashtagNamesByTargetIds(PostType.FAN_POST, postIds);

        List<FanPostResponse> responses = visibleRows.stream()
                .map(row -> FanPostResponse.from(
                        row,
                        mediaMap.getOrDefault(row.fanPostId(), List.of()),
                        hashtagMap.getOrDefault(row.fanPostId(), List.of())
                ))
                .toList();
        // nextCursor는 실제 응답에 포함된 마지막 fanPostId를 기준으로 만든다.
        Long nextCursor = hasNext && !visibleRows.isEmpty()
                ? visibleRows.get(visibleRows.size() - 1).fanPostId()
                : null;

        return new CursorSliceResponse<>(responses, nextCursor, hasNext, FAN_POST_SLICE_SIZE);
    }

    private Map<Long, List<FanPostMediaResponse>> loadMediaMap(Collection<Long> fanPostIds) {
        if (fanPostIds.isEmpty()) {
            return Map.of();
        }

        // 미디어는 targetId 묶음 조회 후 게시글별로 그룹핑해 목록 조회의 N+1을 방지한다.
        return mediaRepository.findByTargetTypeAndTargetIdInOrderByTargetIdAscSortOrderAsc(PostType.FAN_POST, fanPostIds)
                .stream()
                .collect(Collectors.groupingBy(
                        Media::getTargetId,
                        Collectors.mapping(FanPostMediaResponse::from, Collectors.toList())
                ));
    }
}
