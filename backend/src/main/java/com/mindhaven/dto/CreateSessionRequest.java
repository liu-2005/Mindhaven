package com.mindhaven.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSessionRequest(String title, String initialMessage) {
}
