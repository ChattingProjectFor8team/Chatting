package com.example.infinite.domain.member.artist.service;

import com.example.infinite.domain.artistcontent.media.service.AssetImageService;
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
import com.example.infinite.domain.member.member.support.MemberInputSupport;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.config.CacheConfig;
import com.example.infinite.global.common.constant.CacheNames;
import com.example.infinite.global.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistService {

    private static final Pattern ARTIST_SLUG_PATTERN = Pattern.compile("^[a-z0-9-]+$");
    private static final int ARTIST_SEARCH_SIZE = 10;
    private static final int CREATOR_SORT_ORDER = 1;

    private final ArtistRepository artistRepository;
    private final ArtistMemberRepository artistMemberRepository;
    private final MemberReader memberReader;
    private final ArtistReader artistReader;
    private final AssetImageService assetImageService;

    public PageResponse<ArtistSearchResponse> searchArtistsV1(String keyword, Integer page) {
        // v1은 과제 요구사항상 캐시를 적용하지 않는 원본 조회 API다.
        return new PageResponse<>(artistRepository.searchArtists(keyword, normalizeSearchPage(page) - 1, ARTIST_SEARCH_SIZE));
    }

    // v2는 동일한 검색 결과를 로컬 캐시에 저장해 반복 조회 비용을 줄인다.
    @Cacheable(
            cacheManager = "caffeineCacheManager",
            value = CacheConfig.ARTIST_SEARCH_V2_CACHE,
            key = "'keyword:' + (#keyword == null ? '' : #keyword.trim().toLowerCase(T(java.util.Locale).ROOT)) + ':page:' + (#page == null || #page < 1 ? 1 : #page)"
    )
    public PageResponse<ArtistSearchResponse> searchArtistsV2(String keyword, Integer page) {
        // 캐시 적용 전까지는 동일한 조회 로직을 재사용한다.
        return searchArtistsV1(keyword, page);
    }

    @Cacheable(
            value = CacheNames.ARTIST_SEARCH_V3,
            key = "'keyword:' + (#keyword == null ? '' : #keyword.trim().toLowerCase(T(java.util.Locale).ROOT)) + ':page:' + (#page == null || #page < 1 ? 1 : #page)"
    )
    public PageResponse<ArtistSearchResponse> searchArtistsV3(String keyword, Integer page) {
        // v3는 실제 서비스 관점의 remote cache 비교 버전이다.
        // 조회 로직 자체는 v1과 같고, 캐시 저장소만 Redis로 바꾼다.
        return searchArtistsV1(keyword, page);
    }

    private int normalizeSearchPage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    @Transactional
    public ArtistResponse createArtist(MemberDetailsImpl memberDetails, ArtistCreateRequest request) {
        // JSON 전용 생성 API 와의 호환을 유지하기 위해
        // 파일 없는 버전은 내부 오버로드 메서드로 단순 위임한다.
        return createArtist(memberDetails, request, null, null);
    }

    @Transactional
    public ArtistResponse createArtist(
            MemberDetailsImpl memberDetails,
            ArtistCreateRequest request,
            MultipartFile profileImageFile,
            MultipartFile coverImageFile
    ) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        validateCreatePermission(member);

        // 생성 직전 입력을 정규화해 빈 문자열/중복 slug를 한 번에 방어한다.
        String normalizedSlug = normalizeRequiredSlug(request.slug());
        if (artistRepository.existsBySlug(normalizedSlug)) {
            throw new ArtistException(ArtistErrorCode.ARTIST_SLUG_DUPLICATED);
        }

        String inputProfileImageUrl = MemberInputSupport.trimToNull(request.profileImageUrl());
        String inputCoverImageUrl = MemberInputSupport.trimToNull(request.coverImageUrl());
        String normalizedIntro = MemberInputSupport.trimToNull(request.intro());

        Artist artist = artistRepository.save(Artist.create(
                MemberInputSupport.requireTrimmed(
                        request.name(),
                        () -> new IllegalArgumentException("아티스트 이름은 필수입니다.")
                ),
                normalizedSlug,
                inputProfileImageUrl,
                inputCoverImageUrl,
                normalizedIntro
        ));

        // artist id 가 생긴 뒤에야 storage key 경로를 안정적으로 만들 수 있으므로
        // 대표 이미지 업로드는 엔티티 저장 직후에 처리한다.
        String finalProfileImageUrl = resolveCreatedArtistProfileImageUrl(artist, inputProfileImageUrl, profileImageFile);
        String finalCoverImageUrl = resolveCreatedArtistCoverImageUrl(artist, inputCoverImageUrl, coverImageFile);
        artist.updateProfile(artist.getName(), artist.getSlug(), finalProfileImageUrl, finalCoverImageUrl, normalizedIntro);

        // 아티스트 생성과 첫 ArtistMember 연결은 반드시 같은 트랜잭션에서 끝나야 한다.
        artistMemberRepository.save(ArtistMember.create(
                artist,
                member,
                MemberInputSupport.requireTrimmed(
                        request.stageName(),
                        () -> new IllegalArgumentException("활동명은 필수입니다.")
                ),
                finalProfileImageUrl,
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
        // 기존 JSON 수정 API 도 그대로 유지하고,
        // multipart 버전은 같은 서비스 로직을 재사용한다.
        return updateArtist(memberDetails, artistId, request, null, null);
    }

    @Transactional
    @CacheEvict(
            value = CacheNames.ARTIST_DETAIL_V2,
            key = "#artistId"
    )
    public ArtistResponse updateArtist(
            MemberDetailsImpl memberDetails,
            Long artistId,
            ArtistUpdateRequest request,
            MultipartFile profileImageFile,
            MultipartFile coverImageFile
    ) {
        Member member = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        Artist artist = artistReader.findArtistByIdOrThrow(artistId);
        validateManagePermission(member, artistId);

        String nextSlug = resolveNextSlug(artist, request.slug());
        String nextName = resolveOptionalValue(request.name(), artist.getName());
        String nextProfileImageUrl = resolveUpdatedArtistProfileImageUrl(artist, request.profileImageUrl(), profileImageFile);
        String nextCoverImageUrl = resolveUpdatedArtistCoverImageUrl(artist, request.coverImageUrl(), coverImageFile);
        String nextIntro = resolveOptionalValue(request.intro(), artist.getIntro());

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
        if (member.getRole() != MemberRole.ARTIST) {
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
        String normalizedSlug = normalizeOptionalSlug(slug);
        if (normalizedSlug == null) {
            return artist.getSlug();
        }

        if (artistRepository.existsBySlugAndIdNot(normalizedSlug, artist.getId())) {
            throw new ArtistException(ArtistErrorCode.ARTIST_SLUG_DUPLICATED);
        }
        return normalizedSlug;
    }

    private String normalizeRequiredSlug(String slug) {
        String normalizedSlug = MemberInputSupport.requireTrimmed(
                slug,
                () -> new IllegalArgumentException("아티스트 slug는 필수입니다.")
        ).toLowerCase(Locale.ROOT);
        validateSlugFormat(normalizedSlug);
        return normalizedSlug;
    }

    private String normalizeOptionalSlug(String slug) {
        String normalizedSlug = MemberInputSupport.trimToNull(slug);
        if (normalizedSlug == null) {
            return null;
        }

        normalizedSlug = normalizedSlug.toLowerCase(Locale.ROOT);
        validateSlugFormat(normalizedSlug);
        return normalizedSlug;
    }

    private void validateSlugFormat(String slug) {
        // slug는 표시용 이름이 아니라 URL 식별자이므로 영문/숫자/하이픈 범위만 허용한다.
        if (!ARTIST_SLUG_PATTERN.matcher(slug).matches()) {
            throw new ArtistException(ArtistErrorCode.ARTIST_SLUG_INVALID);
        }
    }

    private String resolveOptionalValue(String requestedValue, String currentValue) {
        String normalizedValue = MemberInputSupport.trimToNull(requestedValue);
        return normalizedValue != null ? normalizedValue : currentValue;
    }

    private String resolveCreatedArtistProfileImageUrl(Artist artist, String currentValue, MultipartFile profileImageFile) {
        if (profileImageFile == null || profileImageFile.isEmpty()) {
            return currentValue;
        }
        // 생성 시에는 이전 파일이 없으므로 단순 업로드만 수행한다.
        return assetImageService.uploadArtistProfileImage(artist.getId(), profileImageFile);
    }

    private String resolveCreatedArtistCoverImageUrl(Artist artist, String currentValue, MultipartFile coverImageFile) {
        if (coverImageFile == null || coverImageFile.isEmpty()) {
            return currentValue;
        }
        return assetImageService.uploadArtistCoverImage(artist.getId(), coverImageFile);
    }

    private String resolveUpdatedArtistProfileImageUrl(Artist artist, String requestedValue, MultipartFile profileImageFile) {
        if (profileImageFile != null && !profileImageFile.isEmpty()) {
            // 수정 시 새 파일이 오면 "새로 업로드 -> 기존 URL best-effort 삭제" 순서로 교체한다.
            String uploadedImageUrl = assetImageService.uploadArtistProfileImage(artist.getId(), profileImageFile);
            assetImageService.deleteByUrlQuietly(artist.getProfileImageUrl());
            return uploadedImageUrl;
        }
        return resolveOptionalValue(requestedValue, artist.getProfileImageUrl());
    }

    private String resolveUpdatedArtistCoverImageUrl(Artist artist, String requestedValue, MultipartFile coverImageFile) {
        if (coverImageFile != null && !coverImageFile.isEmpty()) {
            // cover 도 profile 과 같은 교체 규칙을 사용한다.
            String uploadedImageUrl = assetImageService.uploadArtistCoverImage(artist.getId(), coverImageFile);
            assetImageService.deleteByUrlQuietly(artist.getCoverImageUrl());
            return uploadedImageUrl;
        }
        return resolveOptionalValue(requestedValue, artist.getCoverImageUrl());
    }

    private ArtistResponse buildArtistResponse(Long artistId) {
        List<ArtistDetailRow> detailRows = artistRepository.findArtistDetailRows(artistId);
        if (detailRows.isEmpty()) {
            throw new ArtistException(ArtistErrorCode.ARTIST_NOT_FOUND);
        }
        return ArtistResponse.from(detailRows);
    }
}
