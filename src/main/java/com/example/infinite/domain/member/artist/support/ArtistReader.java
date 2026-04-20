package com.example.infinite.domain.member.artist.support;

import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.artist.entity.ArtistMember;
import com.example.infinite.domain.member.artist.error.ArtistErrorCode;
import com.example.infinite.domain.member.artist.error.ArtistException;
import com.example.infinite.domain.member.artist.repository.ArtistMemberRepository;
import com.example.infinite.domain.member.artist.repository.ArtistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArtistReader {

    private final ArtistRepository artistRepository;
    private final ArtistMemberRepository artistMemberRepository;

    // 아티스트 조회 실패 시 artist 도메인의 not-found 규칙을 공통 적용한다.
    public Artist findArtistByIdOrThrow(Long artistId) {
        return artistRepository.findById(artistId)
                .orElseThrow(() -> new ArtistException(ArtistErrorCode.ARTIST_NOT_FOUND));
    }

    // 아티스트-멤버 복합 조회도 공통 예외 규칙으로 묶는다.
    public ArtistMember findArtistMemberByIdAndArtistIdOrThrow(Long artistMemberId, Long artistId) {
        return artistMemberRepository.findByIdAndArtistId(artistMemberId, artistId)
                .orElseThrow(() -> new ArtistException(ArtistErrorCode.ARTIST_MEMBER_NOT_FOUND));
    }
}
