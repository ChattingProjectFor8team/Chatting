package com.example.infinite.domain.artistcontent.post.artistpost.support;

import com.example.infinite.domain.artistcontent.post.artistpost.entity.ArtistPost;
import com.example.infinite.domain.artistcontent.post.artistpost.repository.ArtistPostRepository;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
// ArtistPostReader는 "artist 소속까지 확인된 게시글 1건 조회"를 공통화한 read helper다.
public class ArtistPostReader {

    private final ArtistPostRepository artistPostRepository;

    public ArtistPost findByIdAndArtistIdOrThrow(Long artistPostId, Long artistId) {
        // 아티스트 게시글도 artist 소속까지 함께 검증해 다른 커뮤니티 글 접근을 막는다.
        return artistPostRepository.findByIdAndArtistId(artistPostId, artistId)
                .orElseThrow(() -> new ArtistContentException(ArtistContentErrorCode.POST_NOT_FOUND));
    }
}
