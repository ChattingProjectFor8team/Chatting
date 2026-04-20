package com.example.infinite.domain.realtimelive.repository;

import com.example.infinite.domain.realtimelive.entity.LiveChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiveChatMessageRepository extends JpaRepository<LiveChatMessage, Long> {
}