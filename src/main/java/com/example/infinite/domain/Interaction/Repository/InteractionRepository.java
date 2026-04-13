package com.example.infinite.domain.Interaction.Repository;

import com.example.infinite.domain.Interaction.Entity.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionRepository extends JpaRepository<Interaction, Long>, InteractionRepositoryCustom {
}
