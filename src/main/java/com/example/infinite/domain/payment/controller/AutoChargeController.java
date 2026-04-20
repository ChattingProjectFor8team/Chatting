package com.example.infinite.domain.payment.controller;

import com.example.infinite.domain.payment.dto.request.ChargeSettingRequest;
import com.example.infinite.domain.payment.dto.response.AutoChargeHistoryResponse;
import com.example.infinite.domain.payment.dto.response.AutoChargeSettingResponse;
import com.example.infinite.domain.payment.service.AutoChargeService;
import com.example.infinite.global.common.dto.ApiResponse;
import com.example.infinite.global.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * 자동충전 API 컨트롤러
 *
 * Base URL: /api/v1/auto-charge
 *
 * 모든 엔드포인트는 게이트웨이에서 주입되는 X-User-Id 헤더로 사용자를 식별한다.
 * (JWT 인증은 게이트웨이에서 처리되므로 이 서비스는 별도 인증 로직 없음)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auto-charge")
public class AutoChargeController {

    private final AutoChargeService autoChargeService;

    /**
     * 자동충전 설정 등록 / 수정
     * - 최초 호출: 신규 설정 생성
     * - 재호출: 카드·젤리 수량·임계치 수정, 비활성 상태라면 재활성화
     *
     * POST /api/v1/auto-charge/setting
     */
    @PostMapping("/setting")
    public ApiResponse<AutoChargeSettingResponse> registerOrUpdate(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ChargeSettingRequest request) {
        return ApiResponse.success(autoChargeService.registerOrUpdate(userId, request));
    }

    /**
     * 자동충전 비활성화
     * - 설정 데이터는 유지하고 enabled = false 로 변경
     * - 재활성화는 POST /setting 재호출로 가능
     *
     * DELETE /api/v1/auto-charge/setting
     */
    @DeleteMapping("/setting")
    public ApiResponse<Void> disable(
            @RequestHeader("X-User-Id") Long userId) {
        autoChargeService.disable(userId);
        return ApiResponse.success(null);
    }

    /**
     * 자동충전 설정 조회
     * - 등록된 카드 정보(카드사명, 끝 4자리), 젤리 수량, 임계 잔액, 활성 여부 반환
     *
     * GET /api/v1/auto-charge/setting
     */
    @GetMapping("/setting")
    public ApiResponse<AutoChargeSettingResponse> getSetting(
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(autoChargeService.getSetting(userId));
    }

    /**
     * 자동충전 실행 이력 조회 (페이징)
     * - 기본: 최신순 20건
     * - 쿼리 파라미터로 page, size, sort 조정 가능
     *
     * GET /api/v1/auto-charge/histories
     */
    @GetMapping("/histories")
    public ApiResponse<PageResponse<AutoChargeHistoryResponse>> getHistories(
            @RequestHeader("X-User-Id") Long userId,
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(new PageResponse<>(autoChargeService.getHistories(userId, pageable)));
    }
}
