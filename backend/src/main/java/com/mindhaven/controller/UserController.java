package com.mindhaven.controller;

import com.mindhaven.common.ApiResponse;
import com.mindhaven.dto.UpdateProfileRequest;
import com.mindhaven.dto.UserDto;
import com.mindhaven.entity.User;
import com.mindhaven.security.AuthContextHolder;
import com.mindhaven.service.AuthService;
import com.mindhaven.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<UserDto> me() {
        Long userId = AuthContextHolder.userId();
        User user = userService.getById(userId);
        return ApiResponse.success(UserDto.of(user, authService.rolesOf(userId), authService.permissionsOf(userId)));
    }

    @PatchMapping
    public ApiResponse<UserDto> updateProfile(@RequestBody UpdateProfileRequest request) {
        Long userId = AuthContextHolder.userId();
        User user = userService.updateProfile(userId, request.nickname(), request.phone());
        return ApiResponse.success(UserDto.of(user, authService.rolesOf(userId), authService.permissionsOf(userId)));
    }
}
