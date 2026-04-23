package com.example.infinite.domain.artistcontent.post.fanletter.controller;

import com.example.infinite.domain.artistcontent.interaction.dto.response.InteractionResponse;
import com.example.infinite.domain.artistcontent.interaction.service.InteractionService;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.request.FanLetterCreateRequest;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.request.FanLetterUpdateRequest;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterCreateResponse;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterHotResponse;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterListResponse;
import com.example.infinite.domain.artistcontent.post.fanletter.dto.response.FanLetterResponse;
import com.example.infinite.domain.artistcontent.post.fanletter.service.FanLetterService;
import com.example.infinite.global.auth.MemberDetailsImpl;
import com.example.infinite.global.common.dto.ApiResponse;
import com.example.infinite.global.common.dto.CursorSliceResponse;
import com.example.infinite.global.common.dto.OffsetSliceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
// 팬레터는 multipart 기반이라 @RequestBody 대신 @ModelAttribute 로 받는다.
public class FanLetterController {

    private final FanLetterService fanLetterService;
    private final InteractionService interactionService;

    @PostMapping(
            value = "/v1/artists/{artistId}/fan-letters",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<FanLetterCreateResponse>> createFanLetter(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @Valid @ModelAttribute FanLetterCreateRequest request
    ) {
        // 생성 응답은 식별자만 반환하고, 목록/상세 API가 카드 렌더링 정보를 책임진다.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(fanLetterService.create(memberDetails, artistId, request)));
    }

    @GetMapping("/v1/artists/{artistId}/fan-letters")
    public ResponseEntity<ApiResponse<CursorSliceResponse<FanLetterListResponse>>> getFanLetters(
            @PathVariable Long artistId,
            @RequestParam(required = false) Long cursor
    ) {
        // 목록은 카드 렌더링에 필요한 최소 정보만 내려준다.
        return ResponseEntity.ok(ApiResponse.success(fanLetterService.getFanLetters(artistId, cursor)));
    }

    @GetMapping("/v1/artists/{artistId}/fan-letters/hot")
    public ResponseEntity<ApiResponse<OffsetSliceResponse<FanLetterHotResponse>>> getHotFanLetters(
            @PathVariable Long artistId,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer size
    ) {
        // FanLetter HOT은 FanPost와 다르게 offset slice 를 사용해
        // "복합커서 vs 오프셋" 비교 학습이 가능하게 둔다.
        return ResponseEntity.ok(ApiResponse.success(
                fanLetterService.getHotFanLetters(artistId, offset, size)
        ));
    }

    @GetMapping("/v1/artists/{artistId}/fan-letters/{fanLetterId}")
    public ResponseEntity<ApiResponse<FanLetterResponse>> getFanLetter(
            @PathVariable Long artistId,
            @PathVariable Long fanLetterId
    ) {
        // 상세는 목록 카드 정보에 더해 작성자 프로필/배지까지 포함해 내려준다.
        return ResponseEntity.ok(ApiResponse.success(fanLetterService.getFanLetter(artistId, fanLetterId)));
    }

    @PatchMapping(
            value = "/v1/artists/{artistId}/fan-letters/{fanLetterId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<FanLetterResponse>> updateFanLetter(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long fanLetterId,
            @Valid @ModelAttribute FanLetterUpdateRequest request
    ) {
        // 수정도 생성과 같은 multipart 계약을 유지해 프론트가 같은 form-data 흐름을 재사용할 수 있게 한다.
        return ResponseEntity.ok(ApiResponse.success(
                fanLetterService.update(memberDetails, artistId, fanLetterId, request)
        ));
    }

    @DeleteMapping("/v1/artists/{artistId}/fan-letters/{fanLetterId}")
    public ResponseEntity<ApiResponse<Void>> deleteFanLetter(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long fanLetterId
    ) {
        // soft delete 와 media 정리는 service 에 위임하고, 컨트롤러는 HTTP 계약만 유지한다.
        fanLetterService.delete(memberDetails, artistId, fanLetterId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/v1/artists/{artistId}/fan-letters/{fanLetterId}/likes/toggle")
    public ResponseEntity<ApiResponse<InteractionResponse>> toggleFanLetterLike(
            @AuthenticationPrincipal MemberDetailsImpl memberDetails,
            @PathVariable Long artistId,
            @PathVariable Long fanLetterId
    ) {
        // 일반 좋아요 토글은 공통 interaction service를 재사용하되,
        // fan letter 전용 target type 검증은 service 쪽에서 수행한다.
        return ResponseEntity.ok(ApiResponse.success(
                interactionService.toggleFanLetterLike(memberDetails, artistId, fanLetterId)
        ));
    }
}
