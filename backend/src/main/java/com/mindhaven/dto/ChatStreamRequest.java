package com.mindhaven.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatStreamRequest(@NotNull Long sessionId, @NotBlank @Size(max = 64) String requestId,
                                @NotBlank @Size(max = 4000) String message) {
}
