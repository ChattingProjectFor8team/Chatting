package com.example.infinite.domain.artistcontent.media.service;

import com.example.infinite.domain.artistcontent.media.dto.request.ArtistYoutubeVideoImportRequest;
import com.example.infinite.domain.artistcontent.media.dto.response.ArtistYoutubeVideoResponse;
import com.example.infinite.domain.artistcontent.media.entity.ArtistYoutubeVideo;
import com.example.infinite.domain.artistcontent.media.repository.ArtistYoutubeVideoRepository;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import com.example.infinite.domain.member.artist.entity.ArtistMember;
import com.example.infinite.domain.member.artist.repository.ArtistMemberRepository;
import com.example.infinite.domain.member.artist.support.ArtistReader;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.support.MemberInputSupport;
import com.example.infinite.domain.member.member.support.MemberReader;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
// 유튜브 미디어 탭은 "링크 등록 -> 외부 메타데이터 조회 -> 카드 메타 저장" 흐름을 담당한다.
// 기존 MediaService가 파일 업로드용이라면, 이 서비스는 외부 링크 아카이브 전용 서비스다.
public class ArtistYoutubeVideoService {

    private static final int DEFAULT_LIST_SIZE = 12;
    private static final int MAX_LIST_SIZE = 50;

    private final ArtistYoutubeVideoRepository artistYoutubeVideoRepository;
    private final ArtistReader artistReader;
    private final ArtistMemberRepository artistMemberRepository;
    private final MemberReader memberReader;
    private final YoutubeMetadataClient youtubeMetadataClient;

    @Transactional
    public ArtistYoutubeVideoResponse importYoutubeVideo(
            MemberDetailsImpl memberDetails,
            Long artistId,
            ArtistYoutubeVideoImportRequest request
    ) {
        // 공개 탭이라도 등록은 특정 artist 소속 멤버만 가능하게 막는다.
        artistReader.findArtistByIdOrThrow(artistId);

        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        ArtistMember artistMember = artistMemberRepository.findByArtistIdAndMemberId(artistId, member.getId())
                .orElseThrow(() -> new ArtistContentException(ArtistContentErrorCode.POST_PERMISSION_DENIED));

        // 1) 링크에서 영상 식별자 추출
        // 2) 같은 artist 안에서 중복 등록 방지
        // 3) 유튜브 API로 카드 메타데이터 조회
        String youtubeVideoId = YoutubeUrlParser.extractVideoId(request.youtubeUrl());
        if (artistYoutubeVideoRepository.existsByArtistIdAndYoutubeVideoId(artistId, youtubeVideoId)) {
            throw new ArtistContentException(ArtistContentErrorCode.MEDIA_YOUTUBE_VIDEO_DUPLICATED);
        }

        YoutubeVideoMetadata metadata = youtubeMetadataClient.fetchVideoMetadata(youtubeVideoId);
        // 작성자 활동명/프로필을 스냅샷으로 저장해 이후 ArtistMember 프로필이 바뀌어도
        // 과거에 등록한 영상 카드 표시가 흔들리지 않게 한다.
        ArtistYoutubeVideo saved = artistYoutubeVideoRepository.save(ArtistYoutubeVideo.create(
                artistId,
                member.getId(),
                artistMember.getStageName(),
                artistMember.getProfileImageUrl(),
                metadata.youtubeVideoId(),
                metadata.youtubeUrl(),
                metadata.title(),
                metadata.thumbnailUrl(),
                metadata.durationSeconds(),
                metadata.publishedAt()
        ));

        return ArtistYoutubeVideoResponse.from(saved);
    }

    public CursorSliceResponse<ArtistYoutubeVideoResponse> getYoutubeVideos(Long artistId, Long cursor, Integer size) {
        artistReader.findArtistByIdOrThrow(artistId);

        int effectiveSize = resolveSize(size);
        PageRequest pageRequest = PageRequest.of(0, effectiveSize + 1);

        // 미디어 탭은 최신 등록순만 필요하므로 publishedAt 복합정렬 대신
        // 구현이 단순한 id DESC 커서를 먼저 적용한다.
        List<ArtistYoutubeVideoResponse> rows = (cursor == null
                ? artistYoutubeVideoRepository.findByArtistIdOrderByIdDesc(artistId, pageRequest)
                : artistYoutubeVideoRepository.findByArtistIdAndIdLessThanOrderByIdDesc(artistId, cursor, pageRequest))
                .stream()
                .map(ArtistYoutubeVideoResponse::from)
                .toList();

        return CursorSliceResponse.of(rows, effectiveSize, ArtistYoutubeVideoResponse::id);
    }

    private int resolveSize(Integer size) {
        if (size == null) {
            return DEFAULT_LIST_SIZE;
        }
        return Math.min(Math.max(size, 1), MAX_LIST_SIZE);
    }
}
