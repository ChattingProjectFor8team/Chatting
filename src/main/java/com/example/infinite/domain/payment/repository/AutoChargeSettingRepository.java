package com.example.infinite.domain.payment.repository;

import com.example.infinite.domain.payment.entity.AutoChargeSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AutoChargeSettingRepository extends JpaRepository<AutoChargeSetting, Long> {

    Optional<AutoChargeSetting> findByUserId(Long userId);
}