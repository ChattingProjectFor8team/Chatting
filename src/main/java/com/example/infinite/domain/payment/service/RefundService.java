package com.example.infinite.domain.payment.service;

import com.example.infinite.domain.dm.service.DmService;
import com.example.infinite.domain.payment.client.PortOneClient;
import com.example.infinite.domain.payment.entity.AutoChargeHistory;
import com.example.infinite.domain.payment.entity.PaymentOrder;
import com.example.infinite.domain.payment.enums.PaymentStatus;
import com.example.infinite.domain.payment.enums.ReferenceType;
import com.example.infinite.domain.payment.enums.TransactionType;
import com.example.infinite.domain.payment.repository.AutoChargeHistoryRepository;
import com.example.infinite.domain.payment.repository.JellyTransactionRepository;
import com.example.infinite.domain.payment.repository.PaymentOrderRepository;
import com.example.infinite.domain.subscriptionmembership.entity.DmSubscription;
import com.example.infinite.domain.subscriptionmembership.enums.SubscriptionStatus;
import com.example.infinite.domain.subscriptionmembership.repository.DmSubscriptionRepository;
import com.example.infinite.global.error.ErrorCode;
import com.example.infinite.global.error.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    // 환불 가능 기간: 결제(구독) 시점으로부터 7일
    private static final int REFUND_PERIOD_DAYS = 7;

    private final PaymentOrderRepository paymentOrderRepository;
    private final AutoChargeHistoryRepository autoChargeHistoryRepository;
    private final DmSubscriptionRepository dmSubscriptionRepository;
    private final JellyTransactionRepository jellyTransactionRepository;
    private final JellyService jellyService;
    private final PortOneClient portOneClient;
    private final DmService dmService;

    // ────────────────────────────────────────────────────────────
    // ⚠️ 트랜잭션 & 외부 API 호출 경계에 대한 설계 주석
    //
    // 현재 구조: @Transactional 안에서 portOneClient.cancelPayment()를 호출한다.
    // 이는 PortOne API가 지연될 경우 DB 커넥션을 점유한 채 대기하게 되어
    // 커넥션 풀 고갈로 이어질 수 있다는 잠재적 위험이 있다.
    //
    // 이상적인 해결책: PortOne 취소를 트랜잭션 밖으로 분리하거나 이벤트 기반 비동기 처리.
    // 현재는 프로젝트 규모와 일정을 고려해 동기 처리를 유지하되,
    // PortOne 호출에 connectTimeout(3s)/readTimeout(10s)이 설정되어 있어
    // 무한 대기는 방지되어 있다. (PortOneClient 생성자 참고)
    // ────────────────────────────────────────────────────────────

    /**
     * 수동결제 환불 — PortOne 결제 취소 + 지급된 젤리 회수
     *
     * ─── 환불 조건 (모두 충족해야 함) ───
     * 1. 결제 완료(PAID) 상태일 것
     * 2. 결제일(paidAt)로부터 7일 이내일 것
     * 3. 결제 이후 젤리를 단 1개도 사용하지 않았을 것
     *    → 젤리는 fungible(대체 가능)하여 특정 결제의 젤리를 추적할 수 없으므로,
     *       결제 이후 USE 트랜잭션이 하나라도 있으면 보수적으로 환불 불가 처리
     *
     * ─── 처리 순서 ───
     * 1) 조건 검증 → 2) PortOne 취소 → 3) 젤리 회수 → 4) 주문 상태 REFUNDED
     * PortOne 취소가 성공해야 젤리 회수를 진행한다.
     * PortOne 취소 실패 시 트랜잭션 롤백으로 DB 상태는 원복된다.
     *
     * @param userId    환불 요청 유저 ID (JWT에서 추출된 인증된 사용자)
     * @param paymentId PortOne 결제 식별자 (PaymentOrder.paymentId)
     */
    @Transactional
    public void refundPayment(Long userId, String paymentId) {
        // paymentId + userId 동시 검증으로 타인의 결제 환불 시도 차단
        PaymentOrder order = paymentOrderRepository.findByPaymentId(paymentId)
                .filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new PaymentException(ErrorCode.REFUND_NOT_FOUND));

        // 이미 환불 완료된 주문은 중복 환불 방지
        if (order.getStatus() == PaymentStatus.REFUNDED) {
            throw new PaymentException(ErrorCode.REFUND_ALREADY_PROCESSED);
        }
        // PENDING/FAILED 상태는 실제 결제가 완료되지 않은 건이므로 환불 대상 아님
        if (order.getStatus() != PaymentStatus.PAID) {
            throw new PaymentException(ErrorCode.REFUND_NOT_FOUND);
        }

        // 환불 가능 기간(7일) 검증
        if (order.getPaidAt().plusDays(REFUND_PERIOD_DAYS).isBefore(LocalDateTime.now())) {
            throw new PaymentException(ErrorCode.REFUND_PERIOD_EXPIRED);
        }

        // 결제 이후 젤리 사용 이력 검증
        // 젤리는 대체 가능 자산이라 특정 충전분을 추적할 수 없으므로,
        // 결제 이후 USE 트랜잭션이 하나라도 존재하면 보수적으로 환불 불가 처리
        if (jellyTransactionRepository.existsByUserIdAndTypeAndCreatedAtAfter(
                userId, TransactionType.USE, order.getPaidAt())) {
            throw new PaymentException(ErrorCode.REFUND_ALREADY_USED);
        }

        // PortOne 결제 취소 — 외부 API 호출 (트랜잭션 내 존재, 위 설계 주석 참고)
        try {
            portOneClient.cancelPayment(paymentId, "고객 환불 요청");
        } catch (Exception e) {
            // PortOne 취소 실패 시 젤리 회수 없이 예외를 던진다.
            // 트랜잭션이 롤백되어 DB 상태는 변경되지 않는다.
            log.error("PortOne 수동결제 취소 실패: paymentId={}, error={}", paymentId, e.getMessage());
            throw new PaymentException(ErrorCode.REFUND_PROCESSING_ERROR);
        }

        // 지급됐던 젤리 회수 — PortOne이 현금을 환불하므로 젤리는 다시 차감
        jellyService.reclaimForCashRefund(
                userId,
                order.getJellyProduct().getJellyAmount(),
                ReferenceType.PAYMENT,
                order.getId()
        );

        // 주문 상태를 REFUNDED로 변경하여 이중 환불 방지
        order.refund();
        log.info("수동결제 환불 완료: userId={}, paymentId={}, jellies={}",
                userId, paymentId, order.getJellyProduct().getJellyAmount());
    }

    /**
     * 자동충전 환불 — PortOne 결제 취소 + 충전된 젤리 회수
     *
     * ─── 환불 조건 (모두 충족해야 함) ───
     * 1. 성공(success=true)한 충전 이력일 것 — 실패 이력은 실제 결제 없음
     * 2. portOnePaymentId가 저장된 이력일 것 — 이 기능 도입 전 데이터는 취소 불가
     * 3. 충전일(createdAt)로부터 7일 이내일 것
     * 4. 충전 이후 젤리를 단 1개도 사용하지 않았을 것
     *
     * ─── 처리 순서 ───
     * 1) 조건 검증 → 2) PortOne 취소 → 3) 젤리 회수 → 4) 이력 환불 완료 표시
     *
     * @param userId    환불 요청 유저 ID (JWT에서 추출된 인증된 사용자)
     * @param historyId AutoChargeHistory ID
     */
    @Transactional
    public void refundAutoCharge(Long userId, Long historyId) {
        // id + userId 동시 검증으로 타인의 충전 이력 환불 시도 차단
        AutoChargeHistory history = autoChargeHistoryRepository.findByIdAndUserId(historyId, userId)
                .orElseThrow(() -> new PaymentException(ErrorCode.REFUND_NOT_FOUND));

        // 실패한 충전 이력은 PortOne에서 실제 결제가 이루어지지 않았으므로 환불 대상 아님
        if (!history.isSuccess()) {
            throw new PaymentException(ErrorCode.REFUND_NOT_FOUND);
        }

        // portOnePaymentId 없는 이력 = 이 기능 도입 전 데이터 → PortOne 취소 불가
        if (history.getPortOnePaymentId() == null) {
            throw new PaymentException(ErrorCode.REFUND_NOT_ELIGIBLE);
        }

        // 이미 환불된 이력의 중복 환불 방지
        if (history.isRefunded()) {
            throw new PaymentException(ErrorCode.REFUND_ALREADY_PROCESSED);
        }

        // 환불 가능 기간(7일) 검증 — createdAt은 BaseEntity의 자동 설정 값
        if (history.getCreatedAt().plusDays(REFUND_PERIOD_DAYS).isBefore(LocalDateTime.now())) {
            throw new PaymentException(ErrorCode.REFUND_PERIOD_EXPIRED);
        }

        // 충전 이후 젤리 사용 이력 검증
        if (jellyTransactionRepository.existsByUserIdAndTypeAndCreatedAtAfter(
                userId, TransactionType.USE, history.getCreatedAt())) {
            throw new PaymentException(ErrorCode.REFUND_ALREADY_USED);
        }

        // PortOne 결제 취소 — 외부 API 호출 (트랜잭션 내 존재, 위 설계 주석 참고)
        try {
            portOneClient.cancelPayment(history.getPortOnePaymentId(), "고객 환불 요청");
        } catch (Exception e) {
            log.error("PortOne 자동충전 취소 실패: historyId={}, error={}", historyId, e.getMessage());
            throw new PaymentException(ErrorCode.REFUND_PROCESSING_ERROR);
        }

        // 충전됐던 젤리 회수 — PortOne이 현금을 환불하므로 젤리는 다시 차감
        jellyService.reclaimForCashRefund(
                userId,
                history.getJellyAmount(),
                ReferenceType.AUTO_CHARGE,
                historyId
        );

        // 이력에 환불 완료 표시 — 운영 추적 및 이중 환불 방지
        history.markRefunded();
        log.info("자동충전 환불 완료: userId={}, historyId={}, jellies={}",
                userId, historyId, history.getJellyAmount());
    }

    /**
     * DM 구독권 환불 — 차감됐던 젤리 반환 + 구독 CANCELLED 처리
     *
     * ─── 환불 조건 (모두 충족해야 함) ───
     * 1. ACTIVE 상태(만료 전)인 구독일 것
     * 2. 구독 시작일(startedAt)로부터 7일 이내일 것
     * 3. 구독 이후 해당 아티스트에게 DM을 단 한 번도 보내지 않았을 것
     *    → DmService.hasUserSentMessageAfter() 를 통해 DM 도메인에 위임 (결합도 최소화)
     *
     * ─── 처리 순서 ───
     * 1) 조건 검증 → 2) 젤리 반환 → 3) 구독 CANCELLED
     * DM 구독은 젤리로 결제했으므로 PortOne 취소 없이 젤리만 반환한다.
     *
     * @param userId         환불 요청 유저 ID (JWT에서 추출된 인증된 사용자)
     * @param subscriptionId DmSubscription ID
     */
    @Transactional
    public void refundDmSubscription(Long userId, Long subscriptionId) {
        // id + userId 동시 검증으로 타인의 구독 환불 시도 차단
        DmSubscription subscription = dmSubscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new PaymentException(ErrorCode.REFUND_NOT_FOUND));

        // 이미 취소(환불)된 구독의 중복 환불 방지
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new PaymentException(ErrorCode.REFUND_ALREADY_PROCESSED);
        }
        // 만료(EXPIRED) 상태는 이미 서비스를 사용한 것으로 간주 → 환불 불가
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new PaymentException(ErrorCode.REFUND_NOT_ELIGIBLE);
        }

        // 환불 가능 기간(7일) 검증
        if (subscription.getStartedAt().plusDays(REFUND_PERIOD_DAYS).isBefore(LocalDateTime.now())) {
            throw new PaymentException(ErrorCode.REFUND_PERIOD_EXPIRED);
        }

        // DM 발송 이력 검증 — DM 도메인에 위임하여 도메인 간 결합도 최소화
        // 구독 이후 단 한 번이라도 DM을 보냈다면 서비스를 사용한 것으로 간주 → 환불 불가
        if (dmService.hasUserSentMessageAfter(userId, subscription.getArtistId(), subscription.getStartedAt())) {
            throw new PaymentException(ErrorCode.REFUND_ALREADY_USED);
        }

        // 구독 시 차감됐던 젤리 반환 (DM 구독은 젤리로 결제했으므로 PortOne 취소 불필요)
        jellyService.refund(
                userId,
                subscription.getJellyAmount(),
                ReferenceType.DM_SUB,
                subscriptionId
        );

        // 구독 상태를 CANCELLED로 변경하여 서비스 이용 불가 + 이중 환불 방지
        subscription.cancel();
        log.info("DM 구독권 환불 완료: userId={}, subscriptionId={}, jellies={}",
                userId, subscriptionId, subscription.getJellyAmount());
    }
}
