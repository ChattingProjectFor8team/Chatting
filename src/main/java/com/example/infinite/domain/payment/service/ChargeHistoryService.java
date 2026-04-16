package com.example.infinite.domain.payment.service;

import com.example.infinite.domain.payment.entity.AutoChargeHistory;
import com.example.infinite.domain.payment.entity.AutoChargeSetting;
import com.example.infinite.domain.payment.repository.AutoChargeHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// [추가] ChargeService에서 분리된 이력 저장 전담 서비스
// 실패 이력이 트랜잭션 롤백으로 유실되는 문제를 해결하기 위해 분리
@Service
@RequiredArgsConstructor
public class ChargeHistoryService {

    private final AutoChargeHistoryRepository autoChargeHistoryRepository;

    // 성공 이력 저장 - 기존 트랜잭션에 참여 (충전과 같은 트랜잭션으로 묶임)
    @Transactional
    public void saveSuccess(Long userId, AutoChargeSetting setting) {
        save(userId, setting, true, null);
    }

    // [추가] 실패 이력 저장 - REQUIRES_NEW로 독립 트랜잭션 사용
    // 바깥 트랜잭션(jellyService.charge)이 롤백돼도 이 커밋은 유지됨
    // → 운영 중 실패 추적 및 결제 감사 로그 보장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailure(Long userId, AutoChargeSetting setting, String reason) {
        save(userId, setting, false, reason);
    }

    // 이력 저장 공통 메서드
    private void save(Long userId, AutoChargeSetting setting, boolean success, String failReason) {
        autoChargeHistoryRepository.save(AutoChargeHistory.builder()
                .userId(userId)
                .billingKey(setting.getBillingKey())
                .jellyAmount(setting.getJellyAmount())
                .success(success)
                .failReason(failReason)
                .build());
    }
}
