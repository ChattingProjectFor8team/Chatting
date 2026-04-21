package com.example.infinite.domain.payment.service;

import com.example.infinite.domain.payment.client.PortOneClient;
import com.example.infinite.domain.payment.config.JellyProperties;
import com.example.infinite.domain.payment.dto.request.ChargeSettingRequest;
import com.example.infinite.domain.payment.dto.response.AutoChargeHistoryResponse;
import com.example.infinite.domain.payment.dto.response.AutoChargeSettingResponse;
import com.example.infinite.domain.payment.entity.AutoChargeSetting;
import com.example.infinite.domain.payment.entity.BillingKey;
import com.example.infinite.domain.payment.enums.ReferenceType;
import com.example.infinite.domain.payment.repository.AutoChargeHistoryRepository;
import com.example.infinite.domain.payment.repository.AutoChargeSettingRepository;
import com.example.infinite.domain.payment.repository.BillingKeyRepository;
import com.example.infinite.global.error.ErrorCode;
import com.example.infinite.global.error.PaymentException;
import com.example.infinite.global.lock.RedisLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 자동충전 서비스
 *
 * ─── 자동충전 전체 흐름 ───
 * 1. 사용자가 등록 API를 호출 → registerOrUpdate() 로 AutoChargeSetting 저장
 * 2. 젤리 사용(JellyService.use()) 완료 후 잔액이 thresholdBalance 이하로 떨어지면
 *    JellyService 가 execute() 를 호출
 * 3. execute() 는 Redis 분산락 획득 → 잔액 재검증 → PortOne 결제 → 젤리 지급 → 이력 저장
 *
 * ─── 순환 참조 해소 ───
 * JellyService → AutoChargeService → JellyService 순환이 발생한다.
 * JellyService 측에서 AutoChargeService 를 @Lazy 로 주입해 스프링 초기화 시 순환을 끊는다.
 * (AutoChargeService 는 JellyService 를 정상 주입해도 됨)
 */
@Slf4j
@Service
public class AutoChargeService {

    private final AutoChargeSettingRepository autoChargeSettingRepository;
    private final AutoChargeHistoryRepository autoChargeHistoryRepository;
    private final BillingKeyRepository billingKeyRepository;
    private final PortOneClient portOneClient;
    private final JellyProperties jellyProperties;
    private final JellyService jellyService;
    private final ChargeHistoryService chargeHistoryService;

    // JellyService 는 AutoChargeService 를 @Lazy 로 주입하므로 이 쪽은 일반 주입
    public AutoChargeService(
            AutoChargeSettingRepository autoChargeSettingRepository,
            AutoChargeHistoryRepository autoChargeHistoryRepository,
            BillingKeyRepository billingKeyRepository,
            PortOneClient portOneClient,
            JellyProperties jellyProperties,
            @Lazy JellyService jellyService,
            ChargeHistoryService chargeHistoryService) {
        this.autoChargeSettingRepository = autoChargeSettingRepository;
        this.autoChargeHistoryRepository = autoChargeHistoryRepository;
        this.billingKeyRepository = billingKeyRepository;
        this.portOneClient = portOneClient;
        this.jellyProperties = jellyProperties;
        this.jellyService = jellyService;
        this.chargeHistoryService = chargeHistoryService;
    }

    // ───────────────────────────────────────────
    // 설정 관리 (사용자 API)
    // ───────────────────────────────────────────

    /**
     * 자동충전 설정 등록 또는 수정
     * - 설정이 없으면 신규 생성 (enabled = true)
     * - 설정이 이미 있으면 카드·젤리 수량·임계치를 수정, 비활성 상태였다면 함께 재활성화
     *   (disable 후 새 설정으로 재등록하는 케이스를 하나의 엔드포인트로 처리)
     */
    @Transactional
    public AutoChargeSettingResponse registerOrUpdate(Long userId, ChargeSettingRequest request) {
        BillingKey billingKey = findBillingKey(request.billingKeyId(), userId);

        AutoChargeSetting setting = autoChargeSettingRepository.findByUserId(userId)
                .map(existing -> {
                    existing.update(billingKey, request.jellyAmount(), request.thresholdBalance());
                    if (!existing.isEnabled()) {
                        existing.enable(); // 비활성 상태에서 재설정 시 자동 재활성화
                    }
                    return existing;
                })
                .orElseGet(() -> autoChargeSettingRepository.save(
                        AutoChargeSetting.builder()
                                .userId(userId)
                                .billingKey(billingKey)
                                .jellyAmount(request.jellyAmount())
                                .thresholdBalance(request.thresholdBalance())
                                .build()
                ));

        return AutoChargeSettingResponse.from(setting);
    }

    /**
     * 자동충전 비활성화
     * - 설정 레코드는 유지하고 enabled = false 로만 변경
     * - 재활성화는 registerOrUpdate() 재호출로 가능
     */
    @Transactional
    public void disable(Long userId) {
        AutoChargeSetting setting = autoChargeSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new PaymentException(ErrorCode.AUTO_CHARGE_SETTING_NOT_FOUND));
        setting.disable();
    }

    /**
     * 자동충전 설정 조회
     */
    @Transactional(readOnly = true)
    public AutoChargeSettingResponse getSetting(Long userId) {
        AutoChargeSetting setting = autoChargeSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new PaymentException(ErrorCode.AUTO_CHARGE_SETTING_NOT_FOUND));
        return AutoChargeSettingResponse.from(setting);
    }

    /**
     * 자동충전 실행 이력 페이징 조회
     * - 최신순 정렬은 Controller 의 PageableDefault 에서 지정
     */
    @Transactional(readOnly = true)
    public Page<AutoChargeHistoryResponse> getHistories(Long userId, Pageable pageable) {
        return autoChargeHistoryRepository.findByUserId(userId, pageable)
                .map(AutoChargeHistoryResponse::from);
    }

    // ───────────────────────────────────────────
    // 자동충전 실행 (내부 호출 전용)
    // ───────────────────────────────────────────

    /**
     * 자동충전 실행 — JellyService.use() 완료 후 내부에서 호출
     *
     * [트랜잭션 격리 — REQUIRES_NEW]
     * JellyService.use() 와 별개 트랜잭션으로 실행되므로,
     * 자동충전이 실패해도 앞서 처리된 젤리 사용이 롤백되지 않는다.
     *
     * [동시성 제어 — @RedisLock]
     * 같은 userId 로 동시에 execute() 가 호출되면 직렬화하여 이중 청구를 막는다.
     * - waitTime=3s : 락 획득 최대 대기 시간
     * - leaseTime=10s : 락 최대 점유 시간 (PortOne 호출 포함 충분한 여유)
     *
     * [잔액 재검증]
     * 락 대기 중 다른 스레드가 이미 자동충전을 완료했을 수 있으므로,
     * 락 획득 직후 잔액을 다시 조회해 임계치 초과 여부를 재확인한다.
     *
     * @param userId 자동충전 대상 사용자 ID
     */
    @RedisLock(key = "'auto-charge:' + #userId", waitTime = 3, leaseTime = 10)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(Long userId) {
        // 활성 상태 설정이 없으면 조용히 종료 (JellyService 에서 예외를 무시하므로 throw 가능)
        AutoChargeSetting setting = autoChargeSettingRepository.findByUserId(userId)
                .filter(AutoChargeSetting::isEnabled)
                .orElseThrow(() -> new PaymentException(ErrorCode.AUTO_CHARGE_SETTING_NOT_FOUND));

        // 락 획득 후 잔액 재검증 — 이미 다른 스레드가 충전을 완료했다면 스킵
        int currentBalance = jellyService.getBalance(userId).currentBalance();
        if (currentBalance > setting.getThresholdBalance()) {
            log.debug("자동충전 스킵 (잔액 충분): userId={}, balance={}, threshold={}",
                    userId, currentBalance, setting.getThresholdBalance());
            return;
        }

        BillingKey billingKey = setting.getBillingKey();
        int chargeAmount = setting.getJellyAmount() * jellyProperties.pricePerUnit();

        try {
            // 1) PortOne 빌링키 결제 — paymentId를 반환받아 이력에 저장 (환불 시 취소 API 호출에 필요)
            String portOnePaymentId = portOneClient.charge(billingKey.getBillingKey(), chargeAmount, userId);
            // 2) 젤리 지급 (CHARGE 거래 이력도 함께 저장됨)
            jellyService.charge(userId, setting.getJellyAmount(), ReferenceType.AUTO_CHARGE, setting.getId());
            // 3) 성공 이력 저장 — 현재 트랜잭션(REQUIRES_NEW)에 참여
            chargeHistoryService.saveSuccess(userId, setting, portOnePaymentId);
            log.info("자동충전 성공: userId={}, jellies={}", userId, setting.getJellyAmount());
        } catch (Exception e) {
            // 실패 이력은 ChargeHistoryService.saveFailure() 의 REQUIRES_NEW 트랜잭션으로 독립 저장
            // → 이 메서드의 트랜잭션이 롤백돼도 실패 이력은 DB에 남아 운영 추적 가능
            chargeHistoryService.saveFailure(userId, setting, e.getMessage());
            log.error("자동충전 실패: userId={}, error={}", userId, e.getMessage());
            throw new PaymentException(ErrorCode.PAYMENT_AUTO_CHARGING_FAILED);
        }
    }

    // ───────────────────────────────────────────
    // private 헬퍼
    // ───────────────────────────────────────────

    /**
     * 빌링키 소유자 검증
     * - 존재하지 않거나 본인 소유가 아니면 예외
     */
    private BillingKey findBillingKey(Long billingKeyId, Long userId) {
        BillingKey billingKey = billingKeyRepository.findById(billingKeyId)
                .orElseThrow(() -> new PaymentException(ErrorCode.BILLING_KEY_NOT_FOUND));
        if (!billingKey.getUserId().equals(userId)) {
            throw new PaymentException(ErrorCode.ACCESS_DENIED);
        }
        return billingKey;
    }
}
