package com.mindhaven.common;

import java.time.Instant;

/**
 * 统一响应结构：{code, message, data, requestId}。
 * code 为业务码，成功时为 "OK"。
 */
public record ApiResponse<T>(String code, String message, T data, String requestId) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("OK", "success", data, RequestIdHolder.get());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("OK", message, data, RequestIdHolder.get());
    }

    public static <T> ApiResponse<T> failure(int httpStatus, String code, String message) {
        return new ApiResponse<>(code, message, null, RequestIdHolder.get());
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
        return failure(errorCode.httpStatus(), errorCode.code(), errorCode.message());
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
        return failure(errorCode.httpStatus(), errorCode.code(), message);
    }

    public static ApiResponse<Object> failure(ErrorCode errorCode, Object data) {
        return new ApiResponse<>(errorCode.code(), errorCode.message(), data, RequestIdHolder.get());
    }

    @SuppressWarnings("unused")
    public Instant timestamp() {
        return Instant.now();
    }
}
