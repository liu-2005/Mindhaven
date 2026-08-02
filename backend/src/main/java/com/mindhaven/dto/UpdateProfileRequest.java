package com.mindhaven.dto;

import java.time.LocalDateTime;

import com.mindhaven.entity.User;

public record UpdateProfileRequest(String nickname, String phone) {
}
