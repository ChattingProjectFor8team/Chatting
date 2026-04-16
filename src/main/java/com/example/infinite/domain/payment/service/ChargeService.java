package com.example.infinite.domain.payment.service;

import com.example.infinite.domain.payment.dto.request.ChargeSettingRequest;
import com.example.infinite.domain.payment.dto.response.AutoChargeHistoryResponse;
import com.example.infinite.domain.payment.dto.response.ChargeSettingResponse;
import com.example.infinite.domain.payment.entity.AutoChargeSetting;
import com.example.infinite.domain.payment.entity.AutoChargeHistory;
import com.example.infinite.domain.payment.entity.BillingKey;
import com.example.infinite.domain.payment.enums.ReferenceType;
import com.example.infinite.domain.payment.repository.AutoChargeHistoryRepository;
import com.example.infinite.domain.payment.repository.AutoChargeSettingRepository;
import com.example.infinite.domain.payment.repository.BillingKeyRepository;
import com.example.infinite.global.error.ErrorCode;
import com.example.infinite.global.error.PaymentException;
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
    @Transactional
    public void execute(Long userId) {
        AutoChargeSetting setting = autoChargeSettingRepository.findByUserId(userId)
                .filter(AutoChargeSetting::isEnabled)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_AUTO_CHARGING_FAILED));

        try {
            // 젤리 충전 (JellyService 연동)
            jellyService.charge(userId, setting.getJellyAmount(),
                    ReferenceType.PAYMENT, setting.getId());

            // 성공 이력 저장
            saveHistory(userId, setting, true, null);

        } catch (Exception e) {
            // 실패 이력 저장
            saveHistory(userId, setting, false, e.getMessage());
            throw new PaymentException(ErrorCode.PAYMENT_AUTO_CHARGING_FAILED);
        }
    }

    // 자동충전 이력 조회
    @Transactional(readOnly = true)
    public Page<AutoChargeHistoryResponse> getHistories(Long userId, Pageable pageable) {
        return autoChargeHistoryRepository.findByUserId(userId, pageable)
                .map(AutoChargeHistoryResponse::from);
    }

    // 이력 저장 (공통 메서드)
    private void saveHistory(Long userId, AutoChargeSetting setting,
                             boolean success, String failReason) {
        autoChargeHistoryRepository.save(AutoChargeHistory.builder()
                .userId(userId)
                .billingKey(setting.getBillingKey())
                .jellyAmount(setting.getJellyAmount())
                .success(success)
                .failReason(failReason)
                .build());
    }
}