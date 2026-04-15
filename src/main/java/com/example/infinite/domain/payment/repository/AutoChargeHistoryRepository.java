package com.example.infinite.domain.payment.repository;

import com.example.infinite.domain.payment.entity.AutoChargeHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutoChargeHistoryRepository extends JpaRepository<AutoChargeHistory, Long> {

    // 유저의 자동충전 실행 이력 페이징 조회
    Page<AutoChargeHistory> findByUserId(Long userId, Pageable pageable);
}