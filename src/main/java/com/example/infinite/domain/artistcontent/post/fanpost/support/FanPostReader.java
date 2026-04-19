package com.example.infinite.domain.artistcontent.post.fanpost.support;

import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import com.example.infinite.domain.artistcontent.post.fanpost.entity.FanPost;
import com.example.infinite.domain.artistcontent.post.fanpost.repository.FanPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FanPostReader {

    private final FanPostRepository fanPostRepository;

    public FanPost findByIdAndArtistIdOrThrow(Long fanPostId, Long artistId) {
        // 팬포스트는 항상 artist 소속까지 함께 검증해 다른 커뮤니티 게시글 접근을 막는다.
        return fanPostRepository.findByIdAndArtistId(fanPostId, artistId)
                .orElseThrow(() -> new ArtistContentException(ArtistContentErrorCode.POST_NOT_FOUND));
    }
}
