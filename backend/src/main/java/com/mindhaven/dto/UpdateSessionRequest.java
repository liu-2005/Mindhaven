package com.mindhaven.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSessionRequest(String title, String status) {
}
