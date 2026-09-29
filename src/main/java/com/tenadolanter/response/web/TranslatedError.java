package com.tenadolanter.response.web;

import com.tenadolanter.response.Result;

final class TranslatedError {

    private final int httpStatus;

    private final Result<?> body;

    TranslatedError(int httpStatus, Result<?> body) {
        this.httpStatus = httpStatus;
        this.body = body;
    }

    int httpStatus() {
        return httpStatus;
    }

    Result<?> body() {
        return body;
    }
}
