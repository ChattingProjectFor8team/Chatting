package com.example.infinite.domain.member.artist.service;

import com.example.infinite.domain.member.artist.dto.response.ArtistSearchResponse;
import com.example.infinite.domain.member.artist.repository.ArtistRepository;
import com.example.infinite.global.common.config.CacheConfig;
import com.example.infinite.global.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistService {

    private static final int ARTIST_SEARCH_SIZE = 10;

    private final ArtistRepository artistRepository;

    public PageResponse<ArtistSearchResponse> searchArtistsV1(String keyword) {
        // v1은 과제 요구사항상 캐시를 적용하지 않는 원본 조회 API다.
        return new PageResponse<>(artistRepository.searchArtists(keyword, ARTIST_SEARCH_SIZE));
    }

    // v2는 동일한 검색 결과를 로컬 캐시에 저장해 반복 조회 비용을 줄인다.
    @Cacheable(
            value = CacheConfig.ARTIST_SEARCH_V2_CACHE,
            key = "'keyword:' + (#keyword == null ? '' : #keyword.trim().toLowerCase(T(java.util.Locale).ROOT))"
    )
    public PageResponse<ArtistSearchResponse> searchArtistsV2(String keyword) {
        // 캐시 적용 전까지는 동일한 조회 로직을 재사용한다.
        return searchArtistsV1(keyword);
    }
}
