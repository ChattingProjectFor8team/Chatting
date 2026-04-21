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

    private static final int REFUND_PERIOD_DAYS = 7; // 환불 가능 기간

    private final PaymentOrderRepository paymentOrderRepository;
    private final AutoChargeHistoryRepository autoChargeHistoryRepository;
    private final DmSubscriptionRepository dmSubscriptionRepository;
    private final JellyTransactionRepository jellyTransactionRepository;
    private final JellyService jellyService;
    private final PortOneClient portOneClient;
    private final DmService dmService;

    /**
     * 수동결제 환불 — PortOne 결제 취소 + 지급된 젤리 회수
     *
     * 환불 조건:
     * 1. 결제 완료(PAID) 상태일 것
     * 2. 결제일로부터 7일 이내일 것
     * 3. 결제 이후 젤리를 단 1개도 사용하지 않았을 것
     *
     * @param userId    환불 요청 유저 ID
     * @param paymentId PortOne 결제 식별자 (PaymentOrder.paymentId)
     */
    @Transactional
    public void refundPayment(Long userId, String paymentId) {
        PaymentOrder order = paymentOrderRepository.findByPaymentId(paymentId)
                .filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new PaymentException(ErrorCode.REFUND_NOT_FOUND));

        // 이미 환불됐거나 미결제 상태는 거부
        if (order.getStatus() == PaymentStatus.REFUNDED) {
            throw new PaymentException(ErrorCode.REFUND_ALREADY_PROCESSED);
        }
        if (order.getStatus() != PaymentStatus.PAID) {
            throw new PaymentException(ErrorCode.REFUND_NOT_FOUND);
        }

        // 7일 이내 검증
        if (order.getPaidAt().plusDays(REFUND_PERIOD_DAYS).isBefore(LocalDateTime.now())) {
            throw new PaymentException(ErrorCode.REFUND_PERIOD_EXPIRED);
        }

        // 결제 이후 젤리 사용 이력 검증 — 1개라도 사용했으면 환불 불가
        if (jellyTransactionRepository.existsByUserIdAndTypeAndCreatedAtAfter(
                userId, TransactionType.USE, order.getPaidAt())) {
            throw new PaymentException(ErrorCode.REFUND_ALREADY_USED);
        }

        // PortOne 결제 취소 요청
        try {
            portOneClient.cancelPayment(paymentId, "고객 환불 요청");
        } catch (Exception e) {
            log.error("PortOne 결제 취소 실패: paymentId={}, error={}", paymentId, e.getMessage());
            throw new PaymentException(ErrorCode.REFUND_PROCESSING_ERROR);
        }

        // 지급된 젤리 회수 (현금이 카드로 환불되므로 젤리는 다시 차감)
        jellyService.reclaimForCashRefund(
                userId,
                order.getJellyProduct().getJellyAmount(),
                ReferenceType.PAYMENT,
                order.getId()
        );

        order.refund();
        log.info("수동결제 환불 완료: userId={}, paymentId={}", userId, paymentId);
    }

    /**
     * 자동충전 환불 — PortOne 결제 취소 + 충전된 젤리 회수
     *
     * 환불 조건:
     * 1. 성공한 충전 이력일 것
     * 2. portOnePaymentId가 저장된 이력일 것 (이전 데이터는 환불 불가)
     * 3. 충전일로부터 7일 이내일 것
     * 4. 충전 이후 젤리를 단 1개도 사용하지 않았을 것
     *
     * @param userId    환불 요청 유저 ID
     * @param historyId AutoChargeHistory ID
     */
    @Transactional
    public void refundAutoCharge(Long userId, Long historyId) {
        AutoChargeHistory history = autoChargeHistoryRepository.findByIdAndUserId(historyId, userId)
                .orElseThrow(() -> new PaymentException(ErrorCode.REFUND_NOT_FOUND));

        // 실패한 충전 이력은 실제 결제가 없으므로 환불 대상 아님
        if (!history.isSuccess()) {
            throw new PaymentException(ErrorCode.REFUND_NOT_FOUND);
        }

        // portOnePaymentId가 없는 이력(이 기능 도입 전 데이터)은 환불 불가
        if (history.getPortOnePaymentId() == null) {
            throw new PaymentException(ErrorCode.REFUND_NOT_ELIGIBLE);
        }

        // 7일 이내 검증
        if (history.getCreatedAt().plusDays(REFUND_PERIOD_DAYS).isBefore(LocalDateTime.now())) {
            throw new PaymentException(ErrorCode.REFUND_PERIOD_EXPIRED);
        }

        // 충전 이후 젤리 사용 이력 검증
        if (jellyTransactionRepository.existsByUserIdAndTypeAndCreatedAtAfter(
                userId, TransactionType.USE, history.getCreatedAt())) {
            throw new PaymentException(ErrorCode.REFUND_ALREADY_USED);
        }

        // PortOne 결제 취소
        try {
            portOneClient.cancelPayment(history.getPortOnePaymentId(), "고객 환불 요청");
        } catch (Exception e) {
            log.error("PortOne 자동충전 취소 실패: historyId={}, error={}", historyId, e.getMessage());
            throw new PaymentException(ErrorCode.REFUND_PROCESSING_ERROR);
        }

        // 충전된 젤리 회수
        jellyService.reclaimForCashRefund(
                userId,
                history.getJellyAmount(),
                ReferenceType.AUTO_CHARGE,
                historyId
        );

        log.info("자동충전 환불 완료: userId={}, historyId={}", userId, historyId);
    }

    /**
     * DM 구독권 환불 — 차감됐던 젤리 반환 + 구독 CANCELLED 처리
     *
     * 환불 조건:
     * 1. ACTIVE 상태(만료 전)인 구독일 것
     * 2. 구독 시작일로부터 7일 이내일 것
     * 3. 구독 이후 해당 아티스트에게 DM을 단 한 번도 보내지 않았을 것
     *
     * @param userId         환불 요청 유저 ID
     * @param subscriptionId DmSubscription ID
     */
    @Transactional
    public void refundDmSubscription(Long userId, Long subscriptionId) {
        DmSubscription subscription = dmSubscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new PaymentException(ErrorCode.REFUND_NOT_FOUND));

        // 이미 취소됐거나 만료된 구독은 환불 불가
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new PaymentException(ErrorCode.REFUND_ALREADY_PROCESSED);
        }
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new PaymentException(ErrorCode.REFUND_NOT_ELIGIBLE);
        }

        // 7일 이내 검증
        if (subscription.getStartedAt().plusDays(REFUND_PERIOD_DAYS).isBefore(LocalDateTime.now())) {
            throw new PaymentException(ErrorCode.REFUND_PERIOD_EXPIRED);
        }

        // DM 발송 이력 검증 — 구독 이후 메시지를 보냈으면 환불 불가
        if (dmService.hasUserSentMessageAfter(userId, subscription.getArtistId(), subscription.getStartedAt())) {
            throw new PaymentException(ErrorCode.REFUND_ALREADY_USED);
        }

        // 구독 시 차감된 젤리 반환
        jellyService.refund(
                userId,
                subscription.getJellyAmount(),
                ReferenceType.DM_SUB,
                subscriptionId
        );

        // 구독 취소 처리
        subscription.cancel();
        log.info("DM 구독권 환불 완료: userId={}, subscriptionId={}", userId, subscriptionId);
    }

    /**
     * 팬 멤버십 환불 — 정책상 환불 불가
     * 엔드포인트를 열어두어 클라이언트에 명확한 에러 메시지를 전달한다.
     */
    public void refundFanMembership() {
        throw new PaymentException(ErrorCode.REFUND_NOT_ELIGIBLE);
    }
}
