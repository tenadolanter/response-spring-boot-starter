package com.tenadolanter.response;

/**
 * Shared status codes. Success defaults to 0. Codes 4xx and 5xx describe request or system errors.
 */
public enum CommonResultCode implements ResultCode {

    SUCCESS(0, "OK"),
    BAD_REQUEST(400, "Bad request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not found"),
    METHOD_NOT_ALLOWED(405, "Method not allowed"),
    NOT_ACCEPTABLE(406, "Not acceptable"),
    CONFLICT(409, "Conflict"),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported media type"),
    TOO_MANY_REQUESTS(429, "Too many requests"),
    INTERNAL_ERROR(500, "Internal server error");

    private final int code;

    private final String message;

    CommonResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
