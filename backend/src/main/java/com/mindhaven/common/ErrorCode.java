package com.mindhaven.common;

import java.util.List;
import java.util.Map;

public enum ErrorCode {
    VALIDATION_ERROR(400, "VALIDATION_ERROR", "请求参数不合法"),
    UNAUTHENTICATED(401, "UNAUTHENTICATED", "未登录或凭证失效"),
    TOKEN_EXPIRED(401, "TOKEN_EXPIRED", "登录已过期，请重新登录"),
    FORBIDDEN(403, "FORBIDDEN", "权限不足"),
    RESOURCE_NOT_FOUND(404, "RESOURCE_NOT_FOUND", "资源不存在"),
    RESOURCE_CONFLICT(409, "RESOURCE_CONFLICT", "资源冲突"),
    DUPLICATE_REQUEST(409, "DUPLICATE_REQUEST", "重复请求"),
    FILE_TOO_LARGE(413, "FILE_TOO_LARGE", "文件过大"),
    RATE_LIMITED(429, "RATE_LIMITED", "请求过于频繁"),
    AI_PROVIDER_UNAVAILABLE(503, "AI_PROVIDER_UNAVAILABLE", "服务暂不可用，请稍后再试"),
    INTERNAL_ERROR(500, "INTERNAL_ERROR", "服务器内部错误");

    private final int httpStatus;
    private final String code;
    private final String message;

    ErrorCode(int httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    public record FieldError(String field, String message) {
    }

    public static final class Errors {
        private Errors() {
        }
    }

    @SuppressWarnings("unused")
    public static List<Map<String, String>> demoFieldErrors() {
        return List.of();
    }
}
