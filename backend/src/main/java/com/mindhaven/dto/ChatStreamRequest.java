package com.mindhaven.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatStreamRequest(Long sessionId, @NotBlank String requestId, @NotBlank String message) {
}
