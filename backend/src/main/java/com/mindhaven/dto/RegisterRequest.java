package com.mindhaven.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank @jakarta.validation.constraints.Size(min = 3, max = 50) String username,
        @jakarta.validation.constraints.Email @jakarta.validation.constraints.Size(max = 254) String email,
        @NotBlank @jakarta.validation.constraints.Size(min = 6, max = 64) String password,
        String nickname,
        String phone,
        @NotBlank @jakarta.validation.constraints.Pattern(regexp = "2026-10-21", message = "请阅读并同意当前隐私说明") String privacyConsentVersion) {
}
