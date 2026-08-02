package com.mindhaven.common;

import java.util.List;

public class BizException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<ErrorCode.FieldError> fieldErrors;

    public BizException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
        this.fieldErrors = List.of();
    }

    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.fieldErrors = List.of();
    }

    public BizException(ErrorCode errorCode, List<ErrorCode.FieldError> fieldErrors) {
        super(errorCode.message());
        this.errorCode = errorCode;
        this.fieldErrors = fieldErrors == null ? List.of() : fieldErrors;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public List<ErrorCode.FieldError> fieldErrors() {
        return fieldErrors;
    }
}
