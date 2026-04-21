package com.example.infinite.domain.member.artist.service;

import com.example.infinite.domain.artistcontent.media.service.AssetImageService;
import com.example.infinite.domain.member.artist.dto.request.ArtistMemberCreateRequest;
import com.example.infinite.domain.member.artist.dto.request.ArtistMemberUpdateRequest;
import com.example.infinite.domain.member.artist.dto.response.ArtistMemberResponse;
import com.example.infinite.domain.member.artist.entity.Artist;
import com.example.infinite.domain.member.artist.entity.ArtistMember;
import com.example.infinite.domain.member.artist.error.ArtistErrorCode;
import com.example.infinite.domain.member.artist.error.ArtistException;
import com.example.infinite.domain.member.artist.repository.ArtistMemberRepository;
import com.example.infinite.domain.member.artist.support.ArtistReader;
import com.example.infinite.domain.member.member.entity.Member;
import com.example.infinite.domain.member.member.enums.MemberRole;
import com.example.infinite.domain.member.member.support.MemberReader;
import com.example.infinite.domain.member.member.support.MemberInputSupport;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.constant.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistMemberService {

    private final ArtistMemberRepository artistMemberRepository;
    private final MemberReader memberReader;
    private final ArtistReader artistReader;
    private final AssetImageService assetImageService;

    @Transactional
    @CacheEvict(value = CacheNames.ARTIST_DETAIL_V2, key = "#artistId")
    public ArtistMemberResponse createArtistMember(
            MemberDetailsImpl memberDetails,
            Long artistId,
            ArtistMemberCreateRequest request
    ) {
        // JSON 전용 경로는 파일 없이 기존 URL 문자열 방식만 쓸 수 있도록 오버로드에 위임한다.
        return createArtistMember(memberDetails, artistId, request, null);
    }

    @Transactional
    @CacheEvict(value = CacheNames.ARTIST_DETAIL_V2, key = "#artistId")
    public ArtistMemberResponse createArtistMember(
            MemberDetailsImpl memberDetails,
            Long artistId,
            ArtistMemberCreateRequest request,
            MultipartFile profileImageFile
    ) {
        Member actor = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        validateArtistMemberManagePermission(actor.getId(), artistId);

        Artist artist = artistReader.findArtistByIdOrThrow(artistId);
        Member targetMember = memberReader.findByIdOrThrow(request.memberId());
        validateCreatableMember(targetMember);
        // 파일이 오면 storage 업로드 결과 URL 을 우선 쓰고,
        // 아니면 기존 문자열 URL 입력을 사용한다.
        String profileImageUrl = resolveCreateProfileImageUrl(artistId, targetMember.getId(), request.profileImageUrl(), profileImageFile);

        ArtistMember artistMember = artistMemberRepository.save(ArtistMember.create(
                artist,
                targetMember,
                MemberInputSupport.requireTrimmed(
                        request.stageName(),
                        () -> new IllegalArgumentException("아티스트 멤버 활동명은 필수입니다.")
                ),
                MemberInputSupport.requireTrimmed(
                        profileImageUrl,
                        () -> new ArtistException(ArtistErrorCode.MEDIA_PROFILE_REQUIRED)
                ),
                validateSortOrder(request.sortOrder())
        ));

        return ArtistMemberResponse.from(artistMember);
    }

    @Transactional
    @CacheEvict(value = CacheNames.ARTIST_DETAIL_V2, key = "#artistId")
    public ArtistMemberResponse updateArtistMember(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long artistMemberId,
            ArtistMemberUpdateRequest request
    ) {
        // 수정도 create 와 같은 이유로 파일 없는 버전은 내부 공통 오버로드로 위임한다.
        return updateArtistMember(memberDetails, artistId, artistMemberId, request, null);
    }

    @Transactional
    @CacheEvict(value = CacheNames.ARTIST_DETAIL_V2, key = "#artistId")
    public ArtistMemberResponse updateArtistMember(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long artistMemberId,
            ArtistMemberUpdateRequest request,
            MultipartFile profileImageFile
    ) {
        Member actor = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        validateArtistMemberManagePermission(actor.getId(), artistId);

        ArtistMember artistMember = artistReader.findArtistMemberByIdAndArtistIdOrThrow(artistMemberId, artistId);
        String nextProfileImageUrl = resolveUpdatedProfileImageUrl(artistId, artistMember, request.profileImageUrl(), profileImageFile);
        artistMember.updateProfile(
                resolveOptionalValue(request.stageName(), artistMember.getStageName()),
                nextProfileImageUrl,
                request.status() != null ? request.status() : artistMember.getStatus(),
                resolveSortOrder(request.sortOrder(), artistMember.getSortOrder())
        );

        return ArtistMemberResponse.from(artistMember);
    }

    @Transactional
    @CacheEvict(value = CacheNames.ARTIST_DETAIL_V2, key = "#artistId")
    public void deleteArtistMember(
            MemberDetailsImpl memberDetails,
            Long artistId,
            Long artistMemberId
    ) {
        Member actor = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        validateArtistMemberManagePermission(actor.getId(), artistId);

        ArtistMember artistMember = artistReader.findArtistMemberByIdAndArtistIdOrThrow(artistMemberId, artistId);
        validateDeletableArtistMember(artistId);
        artistMember.delete();
    }

    private void validateArtistMemberManagePermission(Long actorMemberId, Long artistId) {
        // 같은 아티스트 소속 멤버만 멤버 추가/수정/삭제를 관리할 수 있다.
        if (!artistMemberRepository.existsByArtistIdAndMemberId(artistId, actorMemberId)) {
            throw new ArtistException(ArtistErrorCode.ARTIST_MANAGE_DENIED);
        }
    }

    private void validateCreatableMember(Member targetMember) {
        if (targetMember.getRole() != MemberRole.ARTIST) {
            throw new ArtistException(ArtistErrorCode.ARTIST_MEMBER_ROLE_REQUIRED);
        }

        if (artistMemberRepository.existsByMemberId(targetMember.getId())) {
            throw new ArtistException(ArtistErrorCode.ARTIST_MEMBER_ALREADY_LINKED);
        }
    }

    private String resolveOptionalValue(String requestedValue, String currentValue) {
        String normalized = MemberInputSupport.trimToNull(requestedValue);
        return normalized != null ? normalized : currentValue;
    }

    private int resolveSortOrder(Integer requestedSortOrder, int currentSortOrder) {
        return requestedSortOrder != null ? validateSortOrder(requestedSortOrder) : currentSortOrder;
    }

    private int validateSortOrder(Integer sortOrder) {
        if (sortOrder == null || sortOrder < 1) {
            throw new ArtistException(ArtistErrorCode.ARTIST_MEMBER_SORT_ORDER_INVALID);
        }
        return sortOrder;
    }

    private void validateDeletableArtistMember(Long artistId) {
        // 아티스트를 고아 상태로 두지 않기 위해 마지막 멤버 삭제는 막는다.
        if (artistMemberRepository.countByArtistId(artistId) <= 1) {
            throw new ArtistException(ArtistErrorCode.ARTIST_MEMBER_LAST_DELETE_NOT_ALLOWED);
        }
    }

    private String resolveCreateProfileImageUrl(
            Long artistId,
            Long memberId,
            String requestedValue,
            MultipartFile profileImageFile
    ) {
        if (profileImageFile != null && !profileImageFile.isEmpty()) {
            return assetImageService.uploadArtistMemberProfileImage(artistId, memberId, profileImageFile);
        }
        return MemberInputSupport.trimToNull(requestedValue);
    }

    private String resolveUpdatedProfileImageUrl(
            Long artistId,
            ArtistMember artistMember,
            String requestedValue,
            MultipartFile profileImageFile
    ) {
        if (profileImageFile != null && !profileImageFile.isEmpty()) {
            // artistMember profile 교체 시에도 새 URL 을 먼저 확보한 뒤 기존 파일을 정리한다.
            String uploadedImageUrl = assetImageService.uploadArtistMemberProfileImage(
                    artistId,
                    artistMember.getMember().getId(),
                    profileImageFile
            );
            assetImageService.deleteByUrlQuietly(artistMember.getProfileImageUrl());
            return uploadedImageUrl;
        }
        // 파일이 없을 때만 기존 URL 문자열 갱신 경로를 허용한다.
        return resolveOptionalValue(requestedValue, artistMember.getProfileImageUrl());
    }
}
