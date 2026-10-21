package com.mindhaven.controller;

import com.mindhaven.common.ApiResponse;
import com.mindhaven.common.BizException;
import com.mindhaven.common.ErrorCode;
import com.mindhaven.dto.LoginRequest;
import com.mindhaven.dto.LoginResponse;
import com.mindhaven.dto.RegisterRequest;
import com.mindhaven.dto.UserDto;
import com.mindhaven.entity.User;
import com.mindhaven.security.AuthContext;
import com.mindhaven.security.AuthContextHolder;
import com.mindhaven.security.LoginSessionService;
import com.mindhaven.security.RequestLimits;
import java.time.Duration;
import jakarta.servlet.http.HttpServletRequest;
import com.mindhaven.service.AuthService;
import com.mindhaven.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final LoginSessionService sessions;
    private final RequestLimits limits;

    public AuthController(AuthService authService, UserService userService, LoginSessionService sessions, RequestLimits limits) {
        this.authService = authService;
        this.userService = userService;
        this.sessions = sessions;
        this.limits = limits;
    }

    @PostMapping("/register")
    public ApiResponse<UserDto> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        limits.check("register:" + http.getRemoteAddr(), 10, Duration.ofHours(1));
        User user = userService.register(request.username(), request.email(), request.password(),
                request.nickname(), request.phone(), request.privacyConsentVersion());
        return ApiResponse.success(UserDto.of(user, userService.rolesOf(user.getId()), authService.permissionsOf(user.getId())));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        limits.check("login:" + http.getRemoteAddr(), 60, Duration.ofMinutes(5));
        limits.check("login-account:" + request.username().trim().toLowerCase(java.util.Locale.ROOT), 15, Duration.ofMinutes(5));
        AuthContext ctx = authService.login(request.username(), request.password());
        User user = userService.getById(ctx.userId());
        String token = authService.issueToken(ctx);
        return ApiResponse.success(LoginResponse.of(token, user, ctx.roles(), authService.permissionsOf(ctx.userId())));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        sessions.revoke(request.getHeader("Authorization").substring(7).trim());
        return ApiResponse.success(null);
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(HttpServletRequest request) {
        AuthContext ctx = AuthContextHolder.require();
        String token = sessions.rotate(request.getHeader("Authorization").substring(7).trim(), ctx.userId());
        User user = userService.getById(ctx.userId());
        return ApiResponse.success(LoginResponse.of(token, user, ctx.roles(), authService.permissionsOf(ctx.userId())));
    }
}
