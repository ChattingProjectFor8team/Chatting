package com.example.infinite.domain.artistcontent.media.service;

import com.example.infinite.domain.artistcontent.media.dto.request.ArtistYoutubeVideoImportRequest;
import com.example.infinite.domain.artistcontent.media.entity.ArtistYoutubeVideo;
import com.example.infinite.domain.artistcontent.media.repository.ArtistYoutubeVideoRepository;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.artist.entity.ArtistMember;
import com.example.infinite.domain.member.artist.repository.ArtistMemberRepository;
import com.example.infinite.domain.member.artist.support.ArtistReader;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.support.MemberReader;
import com.example.infinite.global.auth.MemberDetailsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtistYoutubeVideoServiceTest {

    @Mock
    private ArtistYoutubeVideoRepository artistYoutubeVideoRepository;

    @Mock
    private ArtistReader artistReader;

    @Mock
    private ArtistMemberRepository artistMemberRepository;

    @Mock
    private MemberReader memberReader;

    @Mock
    private YoutubeMetadataClient youtubeMetadataClient;

    @InjectMocks
    private ArtistYoutubeVideoService artistYoutubeVideoService;

    @Test
    void 동시중복저장경쟁은도메인중복에러로변환한다() {
        Long artistId = 1L;
        String email = "artist@example.com";
        String youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        String youtubeVideoId = "dQw4w9WgXcQ";

        Member member = mock(Member.class);
        when(member.getId()).thenReturn(10L);

        ArtistMember artistMember = mock(ArtistMember.class);
        when(artistMember.getStageName()).thenReturn("stage");
        when(artistMember.getProfileImageUrl()).thenReturn("profile");

        when(artistReader.findArtistByIdOrThrow(artistId)).thenReturn(mock(Artist.class));
        when(memberReader.findByEmailOrThrow(email)).thenReturn(member);
        when(artistMemberRepository.findByArtistIdAndMemberId(artistId, 10L)).thenReturn(Optional.of(artistMember));
        when(artistYoutubeVideoRepository.existsByArtistIdAndYoutubeVideoId(artistId, youtubeVideoId))
                .thenReturn(false, true);
        when(youtubeMetadataClient.fetchVideoMetadata(youtubeVideoId)).thenReturn(new YoutubeVideoMetadata(
                youtubeVideoId,
                youtubeUrl,
                "title",
                "thumb",
                120L,
                LocalDateTime.of(2024, 1, 1, 0, 0)
        ));
        when(artistYoutubeVideoRepository.saveAndFlush(any(ArtistYoutubeVideo.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> artistYoutubeVideoService.importYoutubeVideo(
                MemberDetailsImpl.fromToken(email, "ARTIST", 10L),
                artistId,
                new ArtistYoutubeVideoImportRequest(youtubeUrl)
        ))
                .isInstanceOf(ArtistContentException.class)
                .satisfies(exception -> assertThat(((ArtistContentException) exception).getErrorCode())
                        .isEqualTo(ArtistContentErrorCode.MEDIA_YOUTUBE_VIDEO_DUPLICATED));
    }
}
