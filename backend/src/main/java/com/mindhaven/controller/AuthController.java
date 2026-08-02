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

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<UserDto> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.username(), request.email(), request.password(),
                request.nickname(), request.phone(), request.privacyConsentVersion());
        return ApiResponse.success(UserDto.of(user, userService.rolesOf(user.getId()), authService.permissionsOf(user.getId())));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthContext ctx = authService.login(request.username(), request.password());
        User user = userService.getById(ctx.userId());
        String token = authService.issueToken(ctx);
        return ApiResponse.success(LoginResponse.of(token, user, ctx.roles(), authService.permissionsOf(ctx.userId())));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // 本地实现：客户端清理凭证即可；生产环境应撤销服务端会话。
        return ApiResponse.success(null);
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh() {
        throw new BizException(ErrorCode.UNAUTHENTICATED, "当前实现未启用令牌刷新，请重新登录");
    }
}
