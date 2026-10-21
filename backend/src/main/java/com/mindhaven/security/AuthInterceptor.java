package com.mindhaven.security;

import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final PermissionService permissionService;
    private final LoginSessionService sessions;

    public AuthInterceptor(PermissionService permissionService, LoginSessionService sessions) {
        this.permissionService = permissionService;
        this.sessions = sessions;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (isPublic(path, method)) {
            return true;
        }

        String token = extractToken(request);
        if (token == null) {
            writeError(response, 401, "UNAUTHENTICATED", "未登录或凭证缺失");
            return false;
        }

        AuthContext ctx = sessions.authenticate(token);
        if (ctx == null) {
            writeError(response, 401, "TOKEN_EXPIRED", "登录已过期，请重新登录");
            return false;
        }

        if (handler instanceof HandlerMethod hm) {
            RequirePermission rp = hm.getMethodAnnotation(RequirePermission.class);
            if (rp != null && rp.value().length > 0 && !permissionService.hasAnyPermission(ctx.userId(), rp.value())) {
                writeError(response, 403, "FORBIDDEN", "权限不足");
                return false;
            }
        }
        AuthContextHolder.set(ctx);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContextHolder.clear();
    }

    private boolean isPublic(String path, String method) {
        if ("/api/health".equals(path)) {
            return true;
        }
        if (!path.startsWith("/api/v1/")) {
            return false;
        }
        String p = path.substring("/api/v1".length());
        if (p.equals("/health")) {
            return true;
        }
        if ("POST".equals(method) && (p.equals("/auth/login") || p.equals("/auth/register"))) {
            return true;
        }
        if ("GET".equals(method)) {
            if (p.equals("/knowledge/categories")) {
                return true;
            }
            if (p.equals("/knowledge/articles") || p.startsWith("/knowledge/articles/")) {
                return true;
            }
            if (p.startsWith("/files/")) {
                return true;
            }
        }
        return false;
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"code\":\"%s\",\"message\":\"%s\",\"data\":null,\"requestId\":\"%s\"}"
                .formatted(code, message, com.mindhaven.common.RequestIdHolder.get());
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
    }
}
