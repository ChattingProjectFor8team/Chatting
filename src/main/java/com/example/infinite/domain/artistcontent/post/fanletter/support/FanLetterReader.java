package com.example.infinite.domain.artistcontent.post.fanletter.support;

import com.example.infinite.domain.artistcontent.post.error.ArtistContentErrorCode;
import com.example.infinite.domain.artistcontent.post.error.ArtistContentException;
import com.example.infinite.domain.artistcontent.post.fanletter.entity.FanLetter;
import com.example.infinite.domain.artistcontent.post.fanletter.repository.FanLetterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FanLetterReader {

    private final FanLetterRepository fanLetterRepository;

    public FanLetter findByIdAndArtistIdOrThrow(Long fanLetterId, Long artistId) {
        // 팬레터도 artist scope 안에서 읽어야 URL 상의 artistId 와 실제 데이터가 일치한다.
        return fanLetterRepository.findByIdAndArtistId(fanLetterId, artistId)
                .orElseThrow(() -> new ArtistContentException(ArtistContentErrorCode.POST_NOT_FOUND));
    }
}
