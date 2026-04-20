package com.example.infinite.domain.payment.service;

import com.example.infinite.domain.payment.client.PortOneClient;
import com.example.infinite.domain.payment.config.JellyProperties;
import com.example.infinite.domain.payment.dto.request.PaymentPrepareRequest;
import com.example.infinite.domain.payment.dto.request.PortOneWebhookRequest;
import com.example.infinite.domain.payment.dto.response.PaymentPrepareResponse;
import com.example.infinite.domain.payment.dto.response.PortOnePaymentResponse;
import com.example.infinite.domain.payment.entity.PaymentOrder;
import com.example.infinite.domain.payment.enums.PaymentStatus;
import com.example.infinite.domain.payment.enums.ReferenceType;
import com.example.infinite.domain.payment.repository.PaymentOrderRepository;
import com.example.infinite.global.error.ErrorCode;
import com.example.infinite.global.error.PaymentException;
import com.example.infinite.global.lock.RedisLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PortOneClient portOneClient;
    private final JellyService jellyService;
    private final JellyProperties jellyProperties;

    // 결제 준비 — paymentId 발급 후 클라이언트에 반환
    // 클라이언트는 이 paymentId를 PortOne SDK에 전달해 결제 진행
    @Transactional
    public PaymentPrepareResponse prepare(Long userId, PaymentPrepareRequest request) {
        int amount = request.jellyProduct().getPrice(jellyProperties.pricePerUnit());
        String paymentId = UUID.randomUUID().toString();

        paymentOrderRepository.save(PaymentOrder.builder()
                .paymentId(paymentId)
                .userId(userId)
                .jellyProduct(request.jellyProduct())
                .amount(amount)
                .build());

        return new PaymentPrepareResponse(
                paymentId,
                request.jellyProduct().getOrderName(),
                request.jellyProduct().getJellyAmount(),
                amount
        );
    }

    // 웹훅 처리 — PortOne 결제 완료 시 호출
    // @RedisLock: 동일 paymentId 웹훅 중복 수신 시 직렬화하여 이중 지급 방지
    @RedisLock(key = "'payment:webhook:' + #request.data().paymentId()", waitTime = 3, leaseTime = 10)
    @Transactional
    public void handleWebhook(PortOneWebhookRequest request) {
        // Transaction.Paid 타입만 처리, 그 외 이벤트(취소 등)는 무시
        if (!"Transaction.Paid".equals(request.type())) {
            return;
        }

        String paymentId = request.data().paymentId();
        PaymentOrder order = paymentOrderRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));

        // 이미 처리된 주문 — 웹훅 중복 수신 방어 (idempotency)
        if (order.getStatus() != PaymentStatus.PENDING) {
            log.info("이미 처리된 결제 웹훅 무시: paymentId={}, status={}", paymentId, order.getStatus());
            return;
        }

        // PortOne API로 결제 상태 검증
        PortOnePaymentResponse portOnePayment = portOneClient.getPayment(paymentId);
        if (!portOnePayment.isPaid()) {
            order.fail();
            log.warn("결제 미완료 웹훅 수신: paymentId={}, portOneStatus={}", paymentId, portOnePayment.status());
            return;
        }

        // 금액 검증 — 클라이언트 위변조 방지
        if (portOnePayment.amount().total() != order.getAmount()) {
            order.fail();
            log.error("결제 금액 불일치: paymentId={}, expected={}, actual={}",
                    paymentId, order.getAmount(), portOnePayment.amount().total());
            throw new PaymentException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 젤리 지급 후 주문 완료 처리
        jellyService.charge(order.getUserId(), order.getJellyProduct().getJellyAmount(),
                ReferenceType.PAYMENT, order.getId());
        order.complete();
        log.info("수동 결제 젤리 지급 완료: userId={}, jellies={}, paymentId={}",
                order.getUserId(), order.getJellyProduct().getJellyAmount(), paymentId);
    }
}
