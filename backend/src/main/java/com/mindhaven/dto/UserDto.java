package com.mindhaven.dto;

import java.util.List;

import com.mindhaven.entity.User;

public record UserDto(Long id, String username, String nickname, String email, String phone,
                      String avatarUrl, String status, List<String> roles, List<String> permissions,
                      String createdAt) {

    public static UserDto of(User user, List<String> roles, List<String> permissions) {
        return new UserDto(user.getId(), user.getUsername(), user.getNickname(), user.getEmail(), user.getPhone(),
                null, user.getStatus(), roles, permissions,
                user.getCreatedAt() == null ? null : user.getCreatedAt().toString());
    }
}
