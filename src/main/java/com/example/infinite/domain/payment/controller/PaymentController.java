package com.example.infinite.domain.payment.controller;

import com.example.infinite.domain.payment.dto.request.PaymentPrepareRequest;
import com.example.infinite.domain.payment.dto.request.PortOneWebhookRequest;
import com.example.infinite.domain.payment.dto.response.PaymentPrepareResponse;
import com.example.infinite.domain.payment.service.PaymentService;
import com.example.infinite.global.common.dto.ApiResponse;
import com.example.infinite.global.error.ErrorCode;
import com.example.infinite.global.error.PaymentException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    // PortOne 콘솔 → 웹훅 시크릿 값 (application.yml → 환경변수)
    @Value("${portone.webhook-secret}")
    private String webhookSecret;

    // 결제 준비 — paymentId 발급
    // 클라이언트는 응답받은 paymentId와 amount를 PortOne SDK에 전달해 결제 진행
    @PostMapping("/prepare")
    public ApiResponse<PaymentPrepareResponse> prepare(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PaymentPrepareRequest request) {
        return ApiResponse.success(paymentService.prepare(userId, request));
    }

    // PortOne 웹훅 수신 — 결제 완료 시 PortOne 서버에서 직접 호출
    // 1. 요청 본문을 byte[] 로 받아 HMAC-SHA256 서명을 먼저 검증한다 (위변조 방지).
    // 2. 검증 통과 후 byte[] 를 PortOneWebhookRequest 로 역직렬화해 서비스에 전달한다.
    // SecurityConfig 에서 이 경로는 permitAll() 로 열어두었기 때문에
    // JWT 없이 호출하는 PortOne 서버가 401 로 차단되지 않는다.
    @PostMapping("/webhook")
    public ApiResponse<Void> webhook(
            // PortOne이 요청마다 붙이는 HMAC-SHA256 서명 헤더
            @RequestHeader("x-portone-signature") String signature,
            // 서명 검증을 위해 원본 바이트를 그대로 받는다 (Jackson 파싱 전)
            @RequestBody byte[] rawBody) {

        // ── 서명 검증 ──────────────────────────────────────────────────────
        // 서명 불일치 시 공격성 요청으로 간주하고 즉시 거부
        verifySignature(rawBody, signature);

        // ── 역직렬화 ───────────────────────────────────────────────────────
        PortOneWebhookRequest request;
        try {
            request = objectMapper.readValue(rawBody, PortOneWebhookRequest.class);
        } catch (Exception e) {
            log.error("웹훅 본문 역직렬화 실패: {}", e.getMessage());
            throw new PaymentException(ErrorCode.INVALID_REQUEST);
        }

        // paymentId를 파라미터로 분리해서 전달 — RedisLock SpEL 안정성 확보
        paymentService.handleWebhook(request.data().paymentId(), request);
        return ApiResponse.success(null);
    }

    /**
     * PortOne 웹훅 서명 검증 (HMAC-SHA256)
     * <p>
     * PortOne은 웹훅 요청 본문을 webhook-secret 키로 HMAC-SHA256 해시한 뒤
     * 결과를 16진수 문자열로 인코딩해 x-portone-signature 헤더에 담아 보낸다.
     * 동일한 연산을 서버에서 수행해 일치 여부를 확인함으로써
     * 요청이 PortOne 서버에서 온 것임을 보장한다.
     */
    private void verifySignature(byte[] rawBody, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(rawBody));

            if (!expected.equals(signature)) {
                log.warn("웹훅 서명 불일치: expected={}, received={}", expected, signature);
                throw new PaymentException(ErrorCode.PAYMENT_VERIFICATION_FAILED);
            }
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            // HMAC 알고리즘 초기화 실패 등 예상치 못한 오류
            log.error("웹훅 서명 검증 중 오류 발생: {}", e.getMessage());
            throw new PaymentException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
