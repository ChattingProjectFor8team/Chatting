package com.example.infinite.domain.payment.service;

import com.example.infinite.domain.payment.client.PortOneClient;
import com.example.infinite.domain.payment.config.JellyProperties;
import com.example.infinite.domain.payment.dto.request.ChargeSettingRequest;
import com.example.infinite.domain.payment.dto.response.AutoChargeHistoryResponse;
import com.example.infinite.domain.payment.dto.response.ChargeSettingResponse;
import com.example.infinite.domain.payment.entity.AutoChargeSetting;
import com.example.infinite.domain.payment.entity.BillingKey;
import com.example.infinite.domain.payment.enums.ReferenceType;
import com.example.infinite.domain.payment.repository.AutoChargeHistoryRepository;
import com.example.infinite.domain.payment.repository.AutoChargeSettingRepository;
import com.example.infinite.domain.payment.repository.BillingKeyRepository;
import com.example.infinite.global.error.ErrorCode;
import com.example.infinite.global.error.PaymentException;
import com.example.infinite.global.lock.RedisLock;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChargeService {

    private final AutoChargeSettingRepository autoChargeSettingRepository;
    private final AutoChargeHistoryRepository autoChargeHistoryRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final JellyService jellyService;
    private final ChargeHistoryService chargeHistoryService;
    private final PortOneClient portOneClient;
    private final JellyProperties jellyProperties;

    // 자동충전 설정 등록 또는 수정
    @Transactional
    public ChargeSettingResponse save(Long userId, ChargeSettingRequest request) {
        BillingKey billingKey = billingKeyRepository.findById(request.billingKeyId())
                .orElseThrow(() -> new PaymentException(ErrorCode.RESOURCE_NOT_FOUND));

        AutoChargeSetting setting = autoChargeSettingRepository.findByUserId(userId)
                .map(existing -> {
                    // 기존 설정 수정
                    existing.update(billingKey, request.jellyAmount(), request.thresholdBalance());
                    return existing;
                })
                .orElse(AutoChargeSetting.builder()
                        .userId(userId)
                        .billingKey(billingKey)
                        .jellyAmount(request.jellyAmount())
                        .thresholdBalance(request.thresholdBalance())
                        .build());

        return ChargeSettingResponse.from(autoChargeSettingRepository.save(setting));
    }

    // 자동충전 설정 조회
    @Transactional(readOnly = true)
    public ChargeSettingResponse getSetting(Long userId) {
        AutoChargeSetting setting = autoChargeSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new PaymentException(ErrorCode.RESOURCE_NOT_FOUND));
        return ChargeSettingResponse.from(setting);
    }

    // 자동충전 설정 해제
    @Transactional
    public void disable(Long userId) {
        AutoChargeSetting setting = autoChargeSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new PaymentException(ErrorCode.RESOURCE_NOT_FOUND));
        setting.disable();
    }

    // 자동충전 실행 (잔액 부족 시 내부 호출)
    // 호출 시점: 젤리 사용 후 잔액이 thresholdBalance 이하로 떨어졌을 때
    // [수정] @RedisLock 추가 - 동일 유저의 동시 호출을 직렬화해 이중 청구 방지
    // waitTime=3s: 락 획득 대기, leaseTime=10s: 락 최대 점유 시간
    @RedisLock(key = "'auto-charge:' + #userId", waitTime = 3, leaseTime = 10)
    @Transactional
    public void execute(Long userId) {
        AutoChargeSetting setting = autoChargeSettingRepository.findByUserId(userId)
                .filter(AutoChargeSetting::isEnabled)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_AUTO_CHARGING_FAILED));

        // [수정] 락 획득 후 잔액 재조회 - 락 대기 중 다른 스레드가 이미 충전했을 수 있으므로 재검증
        // 예: thresholdBalance=10, 현재잔액=15 → 충전하지 않음
        // 예: thresholdBalance=10, 현재잔액=5  → 충전 진행
        int currentBalance = jellyService.getBalance(userId).currentBalance();
        if (currentBalance > setting.getThresholdBalance()) {
            return;
        }

        int chargeAmount = setting.getJellyAmount() * jellyProperties.pricePerUnit();
        BillingKey billingKey = setting.getBillingKey();

        try {
            // PortOne 실결제 후 젤리 지급
            portOneClient.charge(billingKey.getBillingKey(), chargeAmount, userId);
            jellyService.charge(userId, setting.getJellyAmount(),
                    ReferenceType.AUTO_CHARGE, setting.getId());
            chargeHistoryService.saveSuccess(userId, setting);

        } catch (Exception e) {
            chargeHistoryService.saveFailure(userId, setting, e.getMessage());
            throw new PaymentException(ErrorCode.PAYMENT_AUTO_CHARGING_FAILED);
        }
    }

    // 자동충전 이력 조회
    @Transactional(readOnly = true)
    public Page<AutoChargeHistoryResponse> getHistories(Long userId, Pageable pageable) {
        return autoChargeHistoryRepository.findByUserId(userId, pageable)
                .map(AutoChargeHistoryResponse::from);
    }

    // [삭제] saveHistory() private 메서드 → ChargeHistoryService로 이전
}
