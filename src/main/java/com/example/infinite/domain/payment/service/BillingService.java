package com.example.infinite.domain.payment.service;

import com.example.infinite.domain.payment.client.PortOneClient;
import com.example.infinite.domain.payment.dto.request.BillingKeyRequest;
import com.example.infinite.domain.payment.dto.response.BillingKeyResponse;
import com.example.infinite.domain.payment.entity.BillingKey;
import com.example.infinite.domain.payment.repository.BillingKeyRepository;
import com.example.infinite.global.error.ErrorCode;
import com.example.infinite.global.error.PaymentException;
import com.example.infinite.global.lock.RedisLock; // [추가] 대표 카드 동시 등록 방지용 분산락 import
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillingKeyRepository billingKeyRepository;
    private final PortOneClient portOneClient;

    // 카드 등록
    // [수정] @RedisLock 추가 - 동일 유저가 두 창에서 동시에 대표 카드 등록 시 대표 카드가 2개 생기는 문제 방지
    @RedisLock(key = "'billing:default:' + #userId", waitTime = 3, leaseTime = 10)
    @Transactional
    public BillingKeyResponse register(Long userId, BillingKeyRequest request) {
        // 대표 카드 등록 시 기존 대표 카드 해제
        if (request.defaultCard()) {
            billingKeyRepository.findByUserIdAndDefaultCardTrue(userId)
                    .ifPresent(existing -> existing.unsetDefault()); // [수정] setDefault(false) → unsetDefault() (의도 명확한 메서드명)
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
    // [수정] PortOne API 호출을 트랜잭션 커밋 후로 분리 (부분 반영)
    // 이유: @Transactional 안에 외부 API 호출이 있으면 두 가지 문제가 생김
    //   1) PortOne API가 느릴 경우 DB 커넥션이 계속 점유되어 커넥션 풀 고갈 위험
    //   2) PortOne 성공 → DB 실패 시 외부는 삭제됐는데 DB엔 남는 데이터 불일치 발생
    // afterCommit 훅으로 DB 커밋 이후에 PortOne API를 호출해 위 문제를 해결함
    // 완전한 해결(PortOne 실패 시 보상 로직)은 Saga 패턴이지만 현재 팀 규모에서 과한 구현이라 판단해 부분 반영
    @Transactional
    public void delete(Long userId, Long billingId) {
        BillingKey billingKey = billingKeyRepository.findById(billingId)
                .orElseThrow(() -> new PaymentException(ErrorCode.BILLING_KEY_NOT_FOUND)); // [수정] RESOURCE_NOT_FOUND → BILLING_KEY_NOT_FOUND

        // 본인 카드인지 검증
        if (!billingKey.getUserId().equals(userId)) {
            throw new PaymentException(ErrorCode.ACCESS_DENIED);
        }

        // DB soft delete (트랜잭션 커밋 시 반영)
        billingKey.delete();

        // [수정] DB 커밋 후 PortOne API 호출 - afterCommit 훅 사용
        // 이렇게 하면 DB 커밋이 완료된 이후에만 외부 API가 호출되어
        // DB 커넥션 점유 시간을 최소화하고 트랜잭션 범위를 DB 작업만으로 한정함
        String portOneBillingKey = billingKey.getBillingKey();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                portOneClient.deleteBillingKey(portOneBillingKey);
            }
        });
    }
}
