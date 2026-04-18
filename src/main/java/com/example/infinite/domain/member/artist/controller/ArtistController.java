package com.example.infinite.domain.member.artist.controller;

import com.example.infinite.domain.member.artist.dto.request.ArtistCreateRequest;
import com.example.infinite.domain.member.artist.dto.request.ArtistMemberCreateRequest;
import com.example.infinite.domain.member.artist.dto.request.ArtistMemberUpdateRequest;
import com.example.infinite.domain.member.artist.dto.request.ArtistUpdateRequest;
import com.example.infinite.domain.member.artist.dto.response.ArtistMemberResponse;
import com.example.infinite.domain.member.artist.dto.response.ArtistPopularSearchResponse;
import com.example.infinite.domain.member.artist.dto.response.ArtistResponse;
import com.example.infinite.domain.member.artist.dto.response.ArtistSearchResponse;
import com.example.infinite.domain.member.artist.service.ArtistMemberService;
import com.example.infinite.domain.member.artist.service.ArtistSearchKeywordService;
import com.example.infinite.domain.member.artist.service.ArtistService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.dto.ApiResponse;
import com.example.infinite.global.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/member")
@RestController
@RequiredArgsConstructor
public class ArtistController {

    private final ArtistMemberService artistMemberService;
    private final ArtistSearchKeywordService artistSearchKeywordService;
    private final ArtistService artistService;

    @GetMapping("v1/artists/search")
    public ResponseEntity<PageResponse<ArtistSearchResponse>> searchArtistsV1(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @RequestParam(required = false) String keyword
    ) {
        // 동일 사용자의 반복 검색은 TTL 동안 한 번만 집계
        artistSearchKeywordService.recordSearchKeyword(memberDetails.getEmail(), keyword);
        return ResponseEntity.ok(artistService.searchArtistsV1(keyword));
    }

    @GetMapping("v2/artists/search")
    public ResponseEntity<PageResponse<ArtistSearchResponse>> searchArtistsV2(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @RequestParam(required = false) String keyword
    ) {
        // 검색 결과 캐시 hit 여부와 무관하게 사용자 기준 인기검색어는 집계
        artistSearchKeywordService.recordSearchKeyword(memberDetails.getEmail(), keyword);
        return ResponseEntity.ok(artistService.searchArtistsV2(keyword));
    }

    @GetMapping("v1/artists/search/popular")
    public ResponseEntity<ApiResponse<List<ArtistPopularSearchResponse>>> getPopularArtistSearchKeywords(
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(artistSearchKeywordService.getPopularKeywords(limit)));
    }

    @PostMapping("/v1/artists")
    public ResponseEntity<ApiResponse<ArtistResponse>> createArtist(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @Valid @RequestBody ArtistCreateRequest request
    ) {
        // 아티스트 권한을 가진 미소속 회원만 자신의 아티스트를 최초 생성
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(artistService.createArtist(memberDetails, request)));
    }

    @GetMapping("v1/artists/{artistId}")
    public ResponseEntity<ApiResponse<ArtistResponse>> getArtist(
            @PathVariable Long artistId
    ) {
        // 아티스트 상세는 비회원도 볼 수 있으므로 artist-member 전체 목록만 공개 조회
        return ResponseEntity.ok(ApiResponse.success(artistService.getArtist(artistId)));
    }

    @GetMapping("v2/artists/{artistId}")
    public ResponseEntity<ApiResponse<ArtistResponse>> getArtistV2(
            @PathVariable Long artistId
    ) {
        // v2는 Redis Cache-aside 전략을 적용한 도전과제용 상세 조회 버전
        return ResponseEntity.ok(ApiResponse.success(artistService.getArtistV2(artistId)));
    }

    @PatchMapping("v1/artists/{artistId}")
    public ResponseEntity<ApiResponse<ArtistResponse>> updateArtist(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @Valid @RequestBody ArtistUpdateRequest request
    ) {
        // 수정은 SUPER_ADMIN 또는 해당 artist에 연결된 ArtistMember만 가능
        return ResponseEntity.ok(ApiResponse.success(artistService.updateArtist(memberDetails, artistId, request)));
    }

    @DeleteMapping("v1/artists/{artistId}")
    public ResponseEntity<ApiResponse<Void>> deleteArtist(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId
    ) {
        artistService.deleteArtist(memberDetails, artistId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("v1/artists/{artistId}/members")
    public ResponseEntity<ApiResponse<ArtistMemberResponse>> createArtistMember(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @Valid @RequestBody ArtistMemberCreateRequest request
    ) {
        // 같은 아티스트에 이미 소속된 멤버만 새 아티스트 멤버를 추가할 수 있다.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(artistMemberService.createArtistMember(memberDetails, artistId, request)));
    }

    @PatchMapping("v1/artists/{artistId}/members/{artistMemberId}")
    public ResponseEntity<ApiResponse<ArtistMemberResponse>> updateArtistMember(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long artistMemberId,
            @Valid @RequestBody ArtistMemberUpdateRequest request
    ) {
        // 아티스트 멤버 수정 시 artist detail v2 캐시를 함께 비운다.
        return ResponseEntity.ok(ApiResponse.success(
                artistMemberService.updateArtistMember(memberDetails, artistId, artistMemberId, request)
        ));
    }

    @DeleteMapping("v1/artists/{artistId}/members/{artistMemberId}")
    public ResponseEntity<ApiResponse<Void>> deleteArtistMember(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long artistMemberId
    ) {
        // 아티스트 멤버 삭제 시 artist detail v2 캐시를 함께 비운다.
        artistMemberService.deleteArtistMember(memberDetails, artistId, artistMemberId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
