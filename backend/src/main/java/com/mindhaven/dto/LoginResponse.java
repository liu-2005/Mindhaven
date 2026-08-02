package com.mindhaven.dto;

import java.util.List;

import com.mindhaven.entity.User;

public record LoginResponse(String token, UserDto user) {

    public static LoginResponse of(String token, User user, List<String> roles, List<String> permissions) {
        return new LoginResponse(token, UserDto.of(user, roles, permissions));
    }
}
