package com.example.infinite.domain.payment.service;

import com.example.infinite.domain.payment.client.PortOneClient;
import com.example.infinite.domain.payment.dto.request.BillingKeyRequest;
import com.example.infinite.domain.payment.dto.response.BillingKeyResponse;
import com.example.infinite.domain.payment.entity.BillingKey;
import com.example.infinite.domain.payment.repository.BillingKeyRepository;
import com.example.infinite.global.error.ErrorCode;
import com.example.infinite.global.error.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillingKeyRepository billingKeyRepository;
    private final PortOneClient portOneClient;

    // 카드 등록
    @Transactional
    public BillingKeyResponse register(Long userId, BillingKeyRequest request) {
        // 대표 카드 등록 시 기존 대표 카드 해제
        if (request.defaultCard()) {
            billingKeyRepository.findByUserIdAndDefaultCardTrue(userId)
                    .ifPresent(existing -> existing.setDefault(false));
        }

        BillingKey billingKey = BillingKey.builder()
                .userId(userId)
                .billingKey(request.billingKey())
                .cardName(request.cardName())
                .cardLast4(request.cardLast4())
                .defaultCard(request.defaultCard())
                .build();

        return BillingKeyResponse.from(billingKeyRepository.save(billingKey));
    }

    // 카드 목록 조회
    @Transactional(readOnly = true)
    public List<BillingKeyResponse> getList(Long userId) {
        return billingKeyRepository.findByUserId(userId).stream()
                .map(BillingKeyResponse::from)
                .toList();
    }

    // 카드 삭제 (soft delete + PortOne 빌링키 해제)
    @Transactional
    public void delete(Long userId, Long billingId) {
        BillingKey billingKey = billingKeyRepository.findById(billingId)
                .orElseThrow(() -> new PaymentException(ErrorCode.RESOURCE_NOT_FOUND));

        // 본인 카드인지 검증
        if (!billingKey.getUserId().equals(userId)) {
            throw new PaymentException(ErrorCode.ACCESS_DENIED);
        }

        // PortOne 빌링키 삭제
        portOneClient.deleteBillingKey(billingKey.getBillingKey());

        // soft delete
        billingKey.delete();
    }
}