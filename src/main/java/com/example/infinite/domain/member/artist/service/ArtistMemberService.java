package com.example.infinite.domain.member.artist.service;

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
import com.example.infinite.domain.member.support.MemberInputSupport;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.constant.CacheNames;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistMemberService {

    private final ArtistMemberRepository artistMemberRepository;
    private final MemberReader memberReader;
    private final ArtistReader artistReader;

    @Transactional
    @CacheEvict(value = CacheNames.ARTIST_DETAIL_V2, key = "#artistId")
    public ArtistMemberResponse createArtistMember(
            MemberDetailsImpl memberDetails,
            Long artistId,
            ArtistMemberCreateRequest request
    ) {
        Member actor = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        validateArtistMemberManagePermission(actor.getId(), artistId);

        Artist artist = artistReader.findArtistByIdOrThrow(artistId);
        Member targetMember = memberReader.findByIdOrThrow(request.memberId());
        validateCreatableMember(targetMember);

        ArtistMember artistMember = artistMemberRepository.save(ArtistMember.create(
                artist,
                targetMember,
                MemberInputSupport.requireTrimmed(
                        request.stageName(),
                        () -> new IllegalArgumentException("아티스트 멤버 활동명은 필수입니다.")
                ),
                MemberInputSupport.requireTrimmed(
                        request.profileImageUrl(),
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
        Member actor = memberReader.findByEmailOrThrow(MemberInputSupport.extractEmail(memberDetails));
        validateArtistMemberManagePermission(actor.getId(), artistId);

        ArtistMember artistMember = artistReader.findArtistMemberByIdAndArtistIdOrThrow(artistMemberId, artistId);
        artistMember.updateProfile(
                resolveOptionalValue(request.stageName(), artistMember.getStageName()),
                resolveOptionalValue(request.profileImageUrl(), artistMember.getProfileImageUrl()),
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
        if (targetMember.getRole() != MemberRole.ARTIST_ADMIN) {
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
}
