package com.example.infinite.domain.payment.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class PortOneClient {

    private final RestClient restClient;

    @Value("${portone.api-secret}")
    private String apiSecret;

    public PortOneClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.portone.io")
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