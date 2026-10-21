package com.mindhaven.repository;

import java.util.Optional;

import com.mindhaven.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findBySessionIdOrderByIdAsc(Long sessionId, Pageable pageable);

    Page<ChatMessage> findBySessionIdOrderByIdDesc(Long sessionId, Pageable pageable);

    long countBySessionId(Long sessionId);

    void deleteBySessionId(Long sessionId);

    Optional<ChatMessage> findBySessionIdAndClientRequestIdAndSenderType(Long sessionId, String clientRequestId, String senderType);
}
