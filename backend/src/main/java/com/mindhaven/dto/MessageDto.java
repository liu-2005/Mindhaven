package com.mindhaven.dto;

import java.time.LocalDateTime;

import com.mindhaven.entity.ChatMessage;

public record MessageDto(Long id, Long sessionId, String senderType, String content, LocalDateTime createdAt) {

    public static MessageDto of(ChatMessage m) {
        return new MessageDto(m.getId(), m.getSessionId(), m.getSenderType(), m.getContent(), m.getCreatedAt());
    }
}
