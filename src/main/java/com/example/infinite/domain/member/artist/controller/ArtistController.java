package com.example.infinite.domain.member.artist.controller;

import com.example.infinite.domain.member.artist.dto.request.ArtistCreateRequest;
import com.example.infinite.domain.member.artist.dto.request.ArtistCreateMultipartRequest;
import com.example.infinite.domain.member.artist.dto.request.ArtistMemberCreateRequest;
import com.example.infinite.domain.member.artist.dto.request.ArtistMemberCreateMultipartRequest;
import com.example.infinite.domain.member.artist.dto.request.ArtistMemberUpdateRequest;
import com.example.infinite.domain.member.artist.dto.request.ArtistMemberUpdateMultipartRequest;
import com.example.infinite.domain.member.artist.dto.request.ArtistUpdateRequest;
import com.example.infinite.domain.member.artist.dto.request.ArtistUpdateMultipartRequest;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/member")
@RestController
@RequiredArgsConstructor
@Tag(name = "Artist", description = "아티스트 및 아티스트 멤버 관리 API")
public class ArtistController {

    private final ArtistMemberService artistMemberService;
    private final ArtistSearchKeywordService artistSearchKeywordService;
    private final ArtistService artistService;

    @Operation(summary = "아티스트 검색 v1", description = "캐시 없이 아티스트를 검색하고 인기 검색어를 집계합니다.")
    @GetMapping("v1/artists/search")
    public ResponseEntity<PageResponse<ArtistSearchResponse>> searchArtistsV1(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @Parameter(description = "검색 키워드", example = "seventeen")
            @RequestParam(required = false) String keyword
    ) {
        // 동일 사용자의 반복 검색은 TTL 동안 한 번만 집계
        artistSearchKeywordService.recordSearchKeyword(memberDetails.getEmail(), keyword);
        return ResponseEntity.ok(artistService.searchArtistsV1(keyword));
    }

    @Operation(summary = "아티스트 검색 v2", description = "로컬 캐시를 적용한 아티스트 검색 버전입니다.")
    @GetMapping("v2/artists/search")
    public ResponseEntity<PageResponse<ArtistSearchResponse>> searchArtistsV2(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @Parameter(description = "검색 키워드", example = "newjeans")
            @RequestParam(required = false) String keyword
    ) {
        // 검색 결과 캐시 hit 여부와 무관하게 사용자 기준 인기검색어는 집계
        artistSearchKeywordService.recordSearchKeyword(memberDetails.getEmail(), keyword);
        return ResponseEntity.ok(artistService.searchArtistsV2(keyword));
    }

    @Operation(summary = "인기 아티스트 검색어 조회", description = "Redis ZSet 기준 인기 검색어 랭킹을 조회합니다.")
    @GetMapping("v1/artists/search/popular")
    public ResponseEntity<ApiResponse<List<ArtistPopularSearchResponse>>> getPopularArtistSearchKeywords(
            @Parameter(description = "조회할 인기 검색어 개수", example = "10")
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(artistSearchKeywordService.getPopularKeywords(limit)));
    }

    @Operation(summary = "아티스트 생성", description = "아티스트 권한을 가진 미소속 회원이 본인 아티스트를 생성합니다.")
    @PostMapping(value = "/v1/artists", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ArtistResponse>> createArtist(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "아티스트 생성 요청 예시",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ArtistCreateRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "name": "SEVENTEEN",
                                      "slug": "seventeen",
                                      "stageName": "S.COUPS",
                                      "profileImageUrl": "https://cdn.infinite.com/artists/seventeen/profile.jpg",
                                      "coverImageUrl": "https://cdn.infinite.com/artists/seventeen/cover.jpg",
                                      "intro": "SEVENTEEN 공식 커뮤니티입니다."
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody ArtistCreateRequest request
    ) {
        // 아티스트 권한을 가진 미소속 회원만 자신의 아티스트를 최초 생성
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(artistService.createArtist(memberDetails, request)));
    }

    @PostMapping(value = "/v1/artists", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ArtistResponse>> createArtistMultipart(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @Valid @ModelAttribute ArtistCreateMultipartRequest request
    ) {
        // multipart 전용 DTO 로 받은 값을 기존 ArtistCreateRequest 로 다시 묶어
        // JSON / multipart 경로가 같은 서비스 로직을 타게 만든다.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(artistService.createArtist(
                        memberDetails,
                        new ArtistCreateRequest(
                                request.getName(),
                                request.getSlug(),
                                request.getStageName(),
                                request.getProfileImageUrl(),
                                request.getCoverImageUrl(),
                                request.getIntro()
                        ),
                        request.getProfileImageFile(),
                        request.getCoverImageFile()
                )));
    }

    @Operation(summary = "아티스트 상세 조회 v1", description = "캐시 없이 아티스트와 전체 아티스트 멤버 목록을 조회합니다.")
    @GetMapping("v1/artists/{artistId}")
    public ResponseEntity<ApiResponse<ArtistResponse>> getArtist(
            @Parameter(description = "조회할 아티스트 ID", example = "1")
            @PathVariable Long artistId
    ) {
        // 아티스트 상세는 비회원도 볼 수 있으므로 artist-member 전체 목록만 공개 조회
        return ResponseEntity.ok(ApiResponse.success(artistService.getArtist(artistId)));
    }

    @Operation(summary = "아티스트 상세 조회 v2", description = "Redis Cache-aside 전략을 적용한 아티스트 상세 조회입니다.")
    @GetMapping("v2/artists/{artistId}")
    public ResponseEntity<ApiResponse<ArtistResponse>> getArtistV2(
            @Parameter(description = "조회할 아티스트 ID", example = "1")
            @PathVariable Long artistId
    ) {
        // v2는 Redis Cache-aside 전략을 적용한 도전과제용 상세 조회 버전
        return ResponseEntity.ok(ApiResponse.success(artistService.getArtistV2(artistId)));
    }

    @Operation(summary = "아티스트 수정", description = "해당 아티스트 소속 멤버 또는 SUPER_ADMIN이 아티스트 정보를 수정합니다.")
    @PatchMapping(value = "v1/artists/{artistId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ArtistResponse>> updateArtist(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @Parameter(description = "수정할 아티스트 ID", example = "1")
            @PathVariable Long artistId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "아티스트 수정 요청 예시",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ArtistUpdateRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "name": "SEVENTEEN",
                                      "slug": "seventeen",
                                      "profileImageUrl": "https://cdn.infinite.com/artists/seventeen/profile-v2.jpg",
                                      "coverImageUrl": "https://cdn.infinite.com/artists/seventeen/cover-v2.jpg",
                                      "intro": "SEVENTEEN 공식 커뮤니티와 최신 소식을 확인하세요."
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody ArtistUpdateRequest request
    ) {
        // 수정은 SUPER_ADMIN 또는 해당 artist에 연결된 ArtistMember만 가능
        return ResponseEntity.ok(ApiResponse.success(artistService.updateArtist(memberDetails, artistId, request)));
    }

    @PatchMapping(value = "v1/artists/{artistId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ArtistResponse>> updateArtistMultipart(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @Valid @ModelAttribute ArtistUpdateMultipartRequest request
    ) {
        // 수정도 동일하게 문자열 필드와 파일 필드를 분리 바인딩한 뒤,
        // 서비스에서는 기존 ArtistUpdateRequest 와 파일 파라미터를 함께 받는다.
        return ResponseEntity.ok(ApiResponse.success(artistService.updateArtist(
                memberDetails,
                artistId,
                new ArtistUpdateRequest(
                        request.getName(),
                        request.getSlug(),
                        request.getProfileImageUrl(),
                        request.getCoverImageUrl(),
                        request.getIntro()
                ),
                request.getProfileImageFile(),
                request.getCoverImageFile()
        )));
    }

    @Operation(summary = "아티스트 삭제", description = "아티스트와 연결된 아티스트 멤버를 함께 soft delete 처리합니다.")
    @DeleteMapping("v1/artists/{artistId}")
    public ResponseEntity<ApiResponse<Void>> deleteArtist(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @Parameter(description = "삭제할 아티스트 ID", example = "1")
            @PathVariable Long artistId
    ) {
        artistService.deleteArtist(memberDetails, artistId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "아티스트 멤버 생성", description = "같은 아티스트 소속 멤버가 새 아티스트 멤버를 추가합니다.")
    @PostMapping(value = "v1/artists/{artistId}/members", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ArtistMemberResponse>> createArtistMember(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @Parameter(description = "멤버를 추가할 아티스트 ID", example = "1")
            @PathVariable Long artistId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "아티스트 멤버 생성 요청 예시",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ArtistMemberCreateRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "memberId": 12,
                                      "stageName": "JEONGHAN",
                                      "profileImageUrl": "https://cdn.infinite.com/artists/seventeen/jeonghan.jpg",
                                      "sortOrder": 2
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody ArtistMemberCreateRequest request
    ) {
        // 같은 아티스트에 이미 소속된 멤버만 새 아티스트 멤버를 추가할 수 있다.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(artistMemberService.createArtistMember(memberDetails, artistId, request)));
    }

    @PostMapping(value = "v1/artists/{artistId}/members", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ArtistMemberResponse>> createArtistMemberMultipart(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @Valid @ModelAttribute ArtistMemberCreateMultipartRequest request
    ) {
        // 아티스트 멤버 생성도 JSON DTO 를 그대로 재사용해
        // 검증/권한/응답 조립 로직이 두 갈래로 갈라지지 않게 유지한다.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(artistMemberService.createArtistMember(
                        memberDetails,
                        artistId,
                        new ArtistMemberCreateRequest(
                                request.getMemberId(),
                                request.getStageName(),
                                request.getProfileImageUrl(),
                                request.getSortOrder()
                        ),
                        request.getProfileImageFile()
                )));
    }

    @Operation(summary = "아티스트 멤버 수정", description = "같은 아티스트 소속 멤버가 아티스트 멤버 정보를 수정합니다.")
    @PatchMapping(value = "v1/artists/{artistId}/members/{artistMemberId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ArtistMemberResponse>> updateArtistMember(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @Parameter(description = "수정할 아티스트 ID", example = "1")
            @PathVariable Long artistId,
            @Parameter(description = "수정할 아티스트 멤버 ID", example = "5")
            @PathVariable Long artistMemberId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "아티스트 멤버 수정 요청 예시",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ArtistMemberUpdateRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "stageName": "JEONGHAN",
                                      "profileImageUrl": "https://cdn.infinite.com/artists/seventeen/jeonghan-v2.jpg",
                                      "status": "ACTIVE",
                                      "sortOrder": 3
                                    }
                                    """)
                    )
            )
            @Valid @RequestBody ArtistMemberUpdateRequest request
    ) {
        // 아티스트 멤버 수정 시 artist detail v2 캐시를 함께 비운다.
        return ResponseEntity.ok(ApiResponse.success(
                artistMemberService.updateArtistMember(memberDetails, artistId, artistMemberId, request)
        ));
    }

    @PatchMapping(value = "v1/artists/{artistId}/members/{artistMemberId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ArtistMemberResponse>> updateArtistMemberMultipart(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long artistMemberId,
            @Valid @ModelAttribute ArtistMemberUpdateMultipartRequest request
    ) {
        // multipart 수정 요청도 기존 ArtistMemberUpdateRequest 와 파일 파라미터 조합으로 변환한다.
        return ResponseEntity.ok(ApiResponse.success(
                artistMemberService.updateArtistMember(
                        memberDetails,
                        artistId,
                        artistMemberId,
                        new ArtistMemberUpdateRequest(
                                request.getStageName(),
                                request.getProfileImageUrl(),
                                request.getStatus(),
                                request.getSortOrder()
                        ),
                        request.getProfileImageFile()
                )
        ));
    }

    @Operation(summary = "아티스트 멤버 삭제", description = "같은 아티스트 소속 멤버가 아티스트 멤버를 삭제합니다. 마지막 멤버는 삭제할 수 없습니다.")
    @DeleteMapping("v1/artists/{artistId}/members/{artistMemberId}")
    public ResponseEntity<ApiResponse<Void>> deleteArtistMember(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @Parameter(description = "멤버를 삭제할 아티스트 ID", example = "1")
            @PathVariable Long artistId,
            @Parameter(description = "삭제할 아티스트 멤버 ID", example = "5")
            @PathVariable Long artistMemberId
    ) {
        // 아티스트 멤버 삭제 시 artist detail v2 캐시를 함께 비운다.
        artistMemberService.deleteArtistMember(memberDetails, artistId, artistMemberId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
