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
import org.springframework.dao.DataIntegrityViolationException;
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
        ArtistYoutubeVideo saved;
        try {
            /*
             * 왜 save 가 아니라 saveAndFlush 인가?
             *
             * 이 메서드는 위에서 "이미 같은 영상이 있는지"를 한 번 먼저 확인한다.
             * 하지만 그 확인과 insert 사이에는 아주 짧은 틈이 있고,
             * 동시에 두 요청이 들어오면 둘 다 "없음"을 보고 save 를 시도할 수 있다.
             *
             * 이 경쟁 상황의 최종 방어선은 DB unique 제약이다.
             * 그래서 flush 시점을 뒤로 미루지 않고 여기서 바로 강제해,
             * 중복이면 지금 이 메서드 안에서 감지하고 우리가 의도한 도메인 예외로 바꾼다.
             */
            saved = artistYoutubeVideoRepository.saveAndFlush(ArtistYoutubeVideo.create(
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
        } catch (DataIntegrityViolationException exception) {
            /*
             * 왜 예외 메시지 문자열을 바로 믿지 않고 다시 exists 조회를 하는가?
             *
             * DataIntegrityViolationException 은 "DB 무결성 오류"라는 큰 범주 예외라서
             * 실행 환경/DB 벤더/드라이버에 따라 메시지 형식이 달라질 수 있다.
             * 예를 들어 로컬 MySQL, 테스트 DB, 운영 DB 의 메시지가 완전히 같다고 보장할 수 없다.
             *
             * 여기서 우리가 정말 알고 싶은 것은
             * "지금 실패 원인이 artist + youtubeVideoId 중복인가?" 이다.
             *
             * 그래서 문자열 파싱 대신, 실패 직후 같은 키가 실제로 존재하는지 다시 확인한다.
             * 존재한다면 우리가 기대한 중복 경쟁으로 보고
             * 클라이언트가 이해할 수 있는 MEDIA_YOUTUBE_VIDEO_DUPLICATED 로 변환한다.
             * 반대로 존재하지 않는다면 다른 무결성 오류일 수 있으므로 원래 예외를 다시 던진다.
             */
            boolean duplicated = artistYoutubeVideoRepository.existsByArtistIdAndYoutubeVideoId(
                    artistId,
                    metadata.youtubeVideoId()
            );
            if (duplicated) {
                throw new ArtistContentException(ArtistContentErrorCode.MEDIA_YOUTUBE_VIDEO_DUPLICATED);
            }
            throw exception;
        }

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
