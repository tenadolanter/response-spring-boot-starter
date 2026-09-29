package com.tenadolanter.response.web;

import com.tenadolanter.response.ResponseProperties;
import com.tenadolanter.response.Result;
import org.springframework.http.ResponseEntity;

final class Responses {

    private Responses() {
    }

    static ResponseEntity<Result<?>> of(ResponseProperties properties, TranslatedError error) {
        int status = properties.isAlwaysOk() ? 200 : normalize(error.httpStatus());
        return ResponseEntity.status(status).body(error.body());
    }

    /**
     * A business code is not always a valid HTTP status, for example 40401. The HTTP status stays 200 and the result is {@code body.code}.
     */
    private static int normalize(int status) {
        if (status >= 100 && status <= 599) {
            return status;
        }
        return 200;
    }
}
