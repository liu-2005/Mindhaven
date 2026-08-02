package com.mindhaven.dto;

import java.time.LocalDateTime;

import com.mindhaven.entity.ChatSession;

public record SessionDto(Long id, String title, String status, Integer messageCount, LocalDateTime lastMessageAt,
                         LocalDateTime startedAt, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static SessionDto of(ChatSession s) {
        return new SessionDto(s.getId(), s.getTitle(), s.getStatus(), s.getMessageCount(), s.getLastMessageAt(),
                s.getStartedAt(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
