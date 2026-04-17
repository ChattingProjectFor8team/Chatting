package com.example.infinite.domain.payment.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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