package com.example.infinite.domain.payment.service;

import com.example.infinite.domain.payment.dto.response.JellyBalance;
import com.example.infinite.domain.payment.dto.response.JellyHistory;
import com.example.infinite.domain.payment.entity.JellyTransaction;
import com.example.infinite.domain.payment.entity.UserJellyBalance;
import com.example.infinite.domain.payment.enums.ReferenceType;
import com.example.infinite.domain.payment.enums.TransactionType;
import com.example.infinite.domain.payment.repository.JellyTransactionRepository;
import com.example.infinite.domain.payment.repository.UserJellyBalanceRepository;
import com.example.infinite.global.error.ErrorCode;
import com.example.infinite.global.error.PaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class JellyService {

    private final UserJellyBalanceRepository jellyWalletRepository;
    private final JellyTransactionRepository jellyTransactionRepository;
    private final AutoChargeService autoChargeService;

    // AutoChargeService 와 순환 참조가 발생하므로 @Lazy 로 프록시 주입
    // (AutoChargeService → JellyService → AutoChargeService)
    public JellyService(UserJellyBalanceRepository jellyWalletRepository,
                        JellyTransactionRepository jellyTransactionRepository,
                        @Lazy AutoChargeService autoChargeService) {
        this.jellyWalletRepository = jellyWalletRepository;
        this.jellyTransactionRepository = jellyTransactionRepository;
        this.autoChargeService = autoChargeService;
    }

    // 잔액 조회 (읽기 전용)
    @Transactional(readOnly = true)
    public JellyBalance getBalance(Long userId) {
        UserJellyBalance wallet = jellyWalletRepository.findById(userId)
                .orElseThrow(() -> new PaymentException(ErrorCode.JELLY_WALLET_NOT_FOUND));
        return JellyBalance.from(wallet);
    }

    // 거래 이력 페이징 조회 (읽기 전용)
    @Transactional(readOnly = true)
    public Page<JellyHistory> getHistory(Long userId, Pageable pageable) {
        return jellyTransactionRepository.findByUserId(userId, pageable)
                .map(JellyHistory::from);
    }

    // 젤리 충전 - 비관적 락 적용 (외부: Webhook, 내부: 자동충전)
    @Transactional
    public void charge(Long userId, int amount, ReferenceType referenceType, Long relatedId) {
        UserJellyBalance wallet = findWalletForUpdate(userId);
        wallet.charge(amount);

        saveTransaction(userId, TransactionType.CHARGE, referenceType, relatedId,
                amount, wallet.getCurrentBalance());
    }

    // 젤리 사용 - 비관적 락 적용 (내부: 구독/멤버십 결제 시 호출)
    @Transactional
    public void use(Long userId, int amount, ReferenceType referenceType, Long relatedId) {
        UserJellyBalance wallet = findWalletForUpdate(userId);
        wallet.use(amount); // 잔액 부족 시 PaymentException(JELLY_INSUFFICIENT_BALANCE) 발생

        saveTransaction(userId, TransactionType.USE, referenceType, relatedId,
                amount, wallet.getCurrentBalance());

        // 자동충전 트리거 — REQUIRES_NEW 트랜잭션으로 독립 실행되므로
        // 자동충전이 실패해도 여기서 예외를 잡아 젤리 사용 트랜잭션은 롤백되지 않음
        try {
            autoChargeService.execute(userId);
        } catch (Exception e) {
            log.warn("자동충전 트리거 실패 (무시): userId={}, error={}", userId, e.getMessage());
        }
    }

    // 젤리 환불 (잔액 반환) — DM 구독권처럼 젤리로 결제한 항목 환불 시 호출
    // 결제 시 차감된 젤리를 다시 지급한다.
    @Transactional
    public void refund(Long userId, int amount, ReferenceType referenceType, Long relatedId) {
        UserJellyBalance wallet = findWalletForUpdate(userId);
        wallet.charge(amount); // 환불 = 잔액 재충전

        saveTransaction(userId, TransactionType.REFUND, referenceType, relatedId,
                amount, wallet.getCurrentBalance());
    }

    // 현금 결제 환불 시 젤리 회수 — 수동결제·자동충전처럼 현금으로 결제한 항목 환불 시 호출
    // PortOne이 현금을 돌려주므로 지급됐던 젤리는 다시 차감한다.
    // use()와 달리 자동충전을 트리거하지 않는다 — 환불로 인한 차감이므로 재충전 불필요
    @Transactional
    public void reclaimForCashRefund(Long userId, int amount, ReferenceType referenceType, Long relatedId) {
        UserJellyBalance wallet = findWalletForUpdate(userId);
        wallet.use(amount); // 지급됐던 젤리 회수

        saveTransaction(userId, TransactionType.REFUND, referenceType, relatedId,
                amount, wallet.getCurrentBalance());
    }

    // 신규 유저 지갑 생성 (회원가입 시 호출)
    @Transactional
    public void createWallet(Long userId) {
        if (jellyWalletRepository.existsById(userId)) {
            return; // 이미 지갑이 있으면 생성하지 않음
        }
        jellyWalletRepository.save(new UserJellyBalance(userId));
    }

    // 비관적 락으로 지갑 조회 (공통 메서드)
    private UserJellyBalance findWalletForUpdate(Long userId) {
        return jellyWalletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new PaymentException(ErrorCode.JELLY_WALLET_NOT_FOUND));
    }

    // 거래 이력 저장 (공통 메서드)
    private void saveTransaction(Long userId, TransactionType type,
                                 ReferenceType referenceType, Long relatedId,
                                 int amount, int balanceAfter) {
        jellyTransactionRepository.save(JellyTransaction.builder()
                .userId(userId)
                .type(type)
                .referenceType(referenceType)
                .relatedId(relatedId)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .build());
    }
}