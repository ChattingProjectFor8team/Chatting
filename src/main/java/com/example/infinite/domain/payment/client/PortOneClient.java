package com.example.infinite.domain.payment.client;

import com.example.infinite.domain.payment.dto.response.PortOnePaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class PortOneClient {

    private final RestClient restClient;
    private final String apiSecret; // [수정] final 추가 - 불변성 보장

    // [수정] 필드 주입(@Value) → 생성자 주입으로 변경
    // 불변성 보장, 테스트 용이성, Spring 공식 권장 방식
    // [추가] connectTimeout/readTimeout 설정 - PortOne API 무응답 시 스레드 무한 대기 방지
    // connectTimeout: 서버 연결까지 대기 시간 (3초)
    // readTimeout: 응답 데이터 수신까지 대기 시간 (10초)
    public PortOneClient(@Value("${portone.api-secret}") String apiSecret) {
        this.apiSecret = apiSecret;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);  // 연결 타임아웃 3초
        factory.setReadTimeout(10000);    // 읽기 타임아웃 10초

        this.restClient = RestClient.builder()
                .baseUrl("https://api.portone.io")
                .requestFactory(factory)
                .build();
    }

    // PortOne 빌링키로 결제 (자동충전)
    // paymentId를 반환해 호출부에서 저장할 수 있도록 한다 — 환불 시 취소 API 호출에 필요
    public String charge(String billingKey, int amount, Long userId) {
        String paymentId = "auto-charge-" + UUID.randomUUID();
        Map<String, Object> body = Map.of(
                "billingKey", billingKey,
                "orderName", "젤리 자동충전",
                "amount", Map.of("total", amount),
                "currency", "KRW",
                "customer", Map.of("id", String.valueOf(userId))
        );
        try {
            restClient.post()
                    .uri("/payments/{paymentId}/billing-key", paymentId)
                    .header("Authorization", "PortOne " + apiSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return paymentId;
        } catch (Exception e) {
            log.error("PortOne 자동충전 결제 실패: userId={}, amount={}, error={}", userId, amount, e.getMessage());
            throw e;
        }
    }

    // PortOne 결제 취소 (환불) — 수동결제·자동충전 환불 시 호출
    public void cancelPayment(String paymentId, String reason) {
        try {
            restClient.post()
                    .uri("/payments/{paymentId}/cancel", paymentId)
                    .header("Authorization", "PortOne " + apiSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("reason", reason))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("PortOne 결제 취소 실패: paymentId={}, error={}", paymentId, e.getMessage());
            throw e;
        }
    }

    // PortOne 결제 단건 조회 — 웹훅 수신 후 결제 상태·금액 검증에 사용
    public PortOnePaymentResponse getPayment(String paymentId) {
        try {
            return restClient.get()
                    .uri("/payments/{paymentId}", paymentId)
                    .header("Authorization", "PortOne " + apiSecret)
                    .retrieve()
                    .body(PortOnePaymentResponse.class);
        } catch (Exception e) {
            log.error("PortOne 결제 조회 실패: paymentId={}, error={}", paymentId, e.getMessage());
            throw e;
        }
    }

    // PortOne 빌링키 삭제 (카드 등록 해제)
    public void deleteBillingKey(String billingKey) {
        try {
            restClient.delete()
                    .uri("/billing-keys/{billingKey}", billingKey)
                    .header("Authorization", "PortOne " + apiSecret)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("PortOne 빌링키 삭제 실패: billingKey={}, error={}", billingKey, e.getMessage());
            throw e;
        }
    }
}