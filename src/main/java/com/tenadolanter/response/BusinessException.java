package com.tenadolanter.response;

/**
 * An expected business failure. The message is returned to the caller.
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCode resultCode) {
        this(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public BusinessException(ResultCode resultCode, String message) {
        this(resultCode.getCode(), message, null);
    }

    public BusinessException(int code, String message) {
        this(code, message, null);
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message == null ? "" : message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
