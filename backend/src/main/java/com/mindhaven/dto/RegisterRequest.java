package com.mindhaven.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String username,
        String email,
        @NotBlank String password,
        String nickname,
        String phone,
        String privacyConsentVersion) {
}
