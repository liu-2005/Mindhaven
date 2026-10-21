package com.mindhaven.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Object>> handleBizException(BizException ex) {
        Object data = null;
        if (!ex.fieldErrors().isEmpty()) {
            data = Map.of("fieldErrors", ex.fieldErrors());
        }
        return ResponseEntity.status(ex.errorCode().httpStatus())
                .body(new ApiResponse<>(ex.errorCode().code(), ex.getMessage(), data, RequestIdHolder.get()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorCode.FieldError> fieldErrors = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(new ErrorCode.FieldError(fe.getField(), fe.getDefaultMessage()));
        }
        return ResponseEntity.status(400)
                .body(new ApiResponse<>("VALIDATION_ERROR", "请求参数不合法",
                        Map.of("fieldErrors", fieldErrors), RequestIdHolder.get()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraint(ConstraintViolationException ex) {
        List<ErrorCode.FieldError> fieldErrors = new ArrayList<>();
        for (ConstraintViolation<?> cv : ex.getConstraintViolations()) {
            fieldErrors.add(new ErrorCode.FieldError(cv.getPropertyPath().toString(), cv.getMessage()));
        }
        return ResponseEntity.status(400)
                .body(new ApiResponse<>("VALIDATION_ERROR", "请求参数不合法",
                        Map.of("fieldErrors", fieldErrors), RequestIdHolder.get()));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(Exception ex) {
        return ResponseEntity.status(400)
                .body(ApiResponse.failure(ErrorCode.VALIDATION_ERROR, "请求参数不合法"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUpload(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(413)
                .body(ApiResponse.failure(ErrorCode.FILE_TOO_LARGE));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(404)
                .body(ApiResponse.failure(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleOther(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(ErrorCode.INTERNAL_ERROR));
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnsupportedMediaType(Exception ex) {
        return ResponseEntity.status(415).body(ApiResponse.failure(ErrorCode.VALIDATION_ERROR, "不支持的请求内容类型"));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConflict(Exception ex) {
        return ResponseEntity.status(409).body(ApiResponse.failure(ErrorCode.RESOURCE_CONFLICT, "数据已存在或已被修改，请刷新后重试"));
    }
}
