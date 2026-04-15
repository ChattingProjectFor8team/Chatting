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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JellyService {

    private final UserJellyBalanceRepository jellyWalletRepository;
    private final JellyTransactionRepository jellyTransactionRepository;

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
    }

    // 젤리 환불 - 비관적 락 적용 (내부: 환불 서비스에서 호출)
    @Transactional
    public void refund(Long userId, int amount, ReferenceType referenceType, Long relatedId) {
        UserJellyBalance wallet = findWalletForUpdate(userId);
        wallet.charge(amount); // 환불은 잔액 재충전

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