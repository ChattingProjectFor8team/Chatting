package com.example.infinite.domain.member.artist.service;

import com.example.infinite.domain.member.artist.dto.request.ArtistCreateRequest;
import com.example.infinite.domain.member.artist.dto.request.ArtistUpdateRequest;
import com.example.infinite.domain.member.artist.dto.response.ArtistDetailRow;
import com.example.infinite.domain.member.artist.dto.response.ArtistResponse;
import com.example.infinite.domain.member.artist.dto.response.ArtistSearchResponse;
import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.artist.entity.ArtistMember;
import com.example.infinite.domain.member.artist.error.ArtistErrorCode;
import com.example.infinite.domain.member.artist.error.ArtistException;
import com.example.infinite.domain.member.artist.repository.ArtistMemberRepository;
import com.example.infinite.domain.member.artist.repository.ArtistRepository;
import com.example.infinite.domain.member.artist.support.ArtistReader;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.enums.MemberRole;
import com.example.infinite.domain.member.member.support.MemberReader;
import com.example.infinite.domain.member.support.MemberInputSupport;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.config.CacheConfig;
import com.example.infinite.global.common.constant.CacheNames;
import com.example.infinite.global.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistService {

    private static final int ARTIST_SEARCH_SIZE = 10;
    private static final int CREATOR_SORT_ORDER = 1;

    private final ArtistRepository artistRepository;
    private final ArtistMemberRepository artistMemberRepository;
    private final MemberReader memberReader;
    private final ArtistReader artistReader;

    public PageResponse<ArtistSearchResponse> searchArtistsV1(String keyword) {
        // v1은 과제 요구사항상 캐시를 적용하지 않는 원본 조회 API다.
        return new PageResponse<>(artistRepository.searchArtists(keyword, ARTIST_SEARCH_SIZE));
    }

    // v2는 동일한 검색 결과를 로컬 캐시에 저장해 반복 조회 비용을 줄인다.
    @Cacheable(
            cacheManager = "caffeineCacheManager",
            value = CacheConfig.ARTIST_SEARCH_V2_CACHE,
            key = "'keyword:' + (#keyword == null ? '' : #keyword.trim().toLowerCase(T(java.util.Locale).ROOT))"
    )
    public PageResponse<ArtistSearchResponse> searchArtistsV2(String keyword) {
        // 캐시 적용 전까지는 동일한 조회 로직을 재사용한다.
        return searchArtistsV1(keyword);
    }

    @Transactional
    public ArtistResponse createArtist(MemberDetailsImpl memberDetails, ArtistCreateRequest request) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        validateCreatePermission(member);

        // 생성 직전 입력을 정규화해 빈 문자열/중복 slug를 한 번에 방어한다.
        String normalizedProfileImageUrl = MemberInputSupport.requireTrimmed(
                request.profileImageUrl(),
                () -> new ArtistException(ArtistErrorCode.MEDIA_PROFILE_REQUIRED)
        );
        String normalizedSlug = MemberInputSupport.requireTrimmed(
                request.slug(),
                () -> new IllegalArgumentException("아티스트 slug는 필수입니다.")
        );
        if (artistRepository.existsBySlug(normalizedSlug)) {
            throw new ArtistException(ArtistErrorCode.ARTIST_SLUG_DUPLICATED);
        }

        Artist artist = artistRepository.save(Artist.create(
                MemberInputSupport.requireTrimmed(
                        request.name(),
                        () -> new IllegalArgumentException("아티스트 이름은 필수입니다.")
                ),
                normalizedSlug,
                normalizedProfileImageUrl,
                MemberInputSupport.trimToNull(request.coverImageUrl()),
                MemberInputSupport.trimToNull(request.intro())
        ));

        // 아티스트 생성과 첫 ArtistMember 연결은 반드시 같은 트랜잭션에서 끝나야 한다.
        ArtistMember artistMember = artistMemberRepository.save(ArtistMember.create(
                artist,
                member,
                MemberInputSupport.requireTrimmed(
                        request.stageName(),
                        () -> new IllegalArgumentException("활동명은 필수입니다.")
                ),
                normalizedProfileImageUrl,
                CREATOR_SORT_ORDER
        ));

        // 생성 직후에도 상세 응답은 전체 artist-member 목록 기준 형태로 통일한다.
        return buildArtistResponse(artist.getId());
    }

    public ArtistResponse getArtist(Long artistId) {
        // 상세 조회는 공개 API로 열고, artist-member 전체 목록만 단일 쿼리로 가져온다.
        return buildArtistResponse(artistId);
    }

    @Cacheable(
            value = CacheNames.ARTIST_DETAIL_V2,
            key = "#artistId"
    )
    public ArtistResponse getArtistV2(Long artistId) {
        // v2는 Cache-aside 전략으로 Redis 캐시 miss 때만 원본 조회를 수행한다.
        return getArtist(artistId);
    }

    @Transactional
    @CacheEvict(
            value = CacheNames.ARTIST_DETAIL_V2,
            key = "#artistId"
    )
    public ArtistResponse updateArtist(MemberDetailsImpl memberDetails, Long artistId, ArtistUpdateRequest request) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        Artist artist = artistReader.findArtistByIdOrThrow(artistId);
        validateManagePermission(member, artistId);

        String nextSlug = resolveNextSlug(artist, request.slug());
        String nextName = resolveOptionalValue(request.name(), artist.getName());
        String nextProfileImageUrl = resolveOptionalValue(request.profileImageUrl(), artist.getProfileImageUrl());
        String nextCoverImageUrl = resolveOptionalValue(request.coverImageUrl(), artist.getCoverImageUrl());
        String nextIntro = resolveOptionalValue(request.intro(), artist.getIntro());

        // 수정 시에도 핵심 프로필 이미지는 유지해야 하므로 null로 비워지지 않게 막는다.
        if (nextProfileImageUrl == null) {
            throw new ArtistException(ArtistErrorCode.MEDIA_PROFILE_REQUIRED);
        }

        artist.updateProfile(nextName, nextSlug, nextProfileImageUrl, nextCoverImageUrl, nextIntro);
        return buildArtistResponse(artistId);
    }

    @Transactional
    @CacheEvict(
            value = CacheNames.ARTIST_DETAIL_V2,
            key = "#artistId"
    )
    public void deleteArtist(MemberDetailsImpl memberDetails, Long artistId) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        Artist artist = artistReader.findArtistByIdOrThrow(artistId);
        validateManagePermission(member, artistId);

        // 삭제 후 재생성이 막히지 않도록 연결된 ArtistMember도 함께 soft delete 처리한다.
        artistMemberRepository.findAllByArtistId(artistId)
                .forEach(ArtistMember::delete);
        artist.delete();
    }

    private void validateCreatePermission(Member member) {
        if (member.getRole() != MemberRole.ARTIST_ADMIN) {
            throw new ArtistException(ArtistErrorCode.ARTIST_CREATE_ROLE_REQUIRED);
        }

        // 현재 정책상 한 회원은 어느 한 아티스트에만 소속될 수 있다고 본다.
        if (artistMemberRepository.existsByMemberId(member.getId())) {
            throw new ArtistException(ArtistErrorCode.ARTIST_MEMBER_ALREADY_LINKED);
        }
    }

    private void validateManagePermission(Member member, Long artistId) {
        if (member.getRole() == MemberRole.SUPER_ADMIN) {
            return;
        }

        if (!artistMemberRepository.existsByArtistIdAndMemberId(artistId, member.getId())) {
            throw new ArtistException(ArtistErrorCode.ARTIST_MANAGE_DENIED);
        }
    }

    private String resolveNextSlug(Artist artist, String slug) {
        String normalizedSlug = MemberInputSupport.trimToNull(slug);
        if (normalizedSlug == null) {
            return artist.getSlug();
        }

        if (artistRepository.existsBySlugAndIdNot(normalizedSlug, artist.getId())) {
            throw new ArtistException(ArtistErrorCode.ARTIST_SLUG_DUPLICATED);
        }
        return normalizedSlug;
    }

    private String resolveOptionalValue(String requestedValue, String currentValue) {
        String normalizedValue = MemberInputSupport.trimToNull(requestedValue);
        return normalizedValue != null ? normalizedValue : currentValue;
    }

    private ArtistResponse buildArtistResponse(Long artistId) {
        List<ArtistDetailRow> detailRows = artistRepository.findArtistDetailRows(artistId);
        if (detailRows.isEmpty()) {
            throw new ArtistException(ArtistErrorCode.ARTIST_NOT_FOUND);
        }
        return ArtistResponse.from(detailRows);
    }
}
