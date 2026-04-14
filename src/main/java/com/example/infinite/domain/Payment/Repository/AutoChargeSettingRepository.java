package com.example.infinite.domain.Payment.Repository;

import com.example.infinite.domain.Payment.Entity.AutoChargeSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AutoChargeSettingRepository extends JpaRepository<AutoChargeSetting, Long> {

    Optional<AutoChargeSetting> findByUserId(Long userId);
}