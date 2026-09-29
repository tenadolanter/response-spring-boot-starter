package com.tenadolanter.response.web;

import com.tenadolanter.response.ResponseProperties;
import com.tenadolanter.response.Result;
import com.tenadolanter.response.compat.ServletApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

/**
 * Formats errors that never reach a controller, such as an unmapped 404, using the same body as the global exception handler.
 */
@RestController
@RequestMapping("${server.error.path:${error.path:/error}}")
public class ResultErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(ResultErrorController.class);

    private static final String[] ERROR_EXCEPTION = ServletApi.errorAttributes("exception");

    private static final String[] ERROR_STATUS = ServletApi.errorAttributes("status_code");

    private static final String[] ERROR_URI = ServletApi.errorAttributes("request_uri");

    private final ResponseProperties properties;

    private final ResultExceptionTranslator translator;

    public ResultErrorController(ResponseProperties properties) {
        this.properties = properties;
        this.translator = new ResultExceptionTranslator(properties);
    }

    @RequestMapping
    public ResponseEntity<Result<?>> error(WebRequest request) {
        int status = status(request);
        if (Boolean.TRUE.equals(request.getAttribute(ControllerScopes.RAW_ERROR, RequestAttributes.SCOPE_REQUEST))) {
            return ResponseEntity.status(status).build();
        }
        Throwable exception = (Throwable) first(request, ERROR_EXCEPTION);
        Object path = first(request, ERROR_URI);
        TranslatedError error;
        if (exception == null) {
            error = translator.translateStatus(status);
        }
        else {
            TranslatedError known = translator.translateKnown(exception);
            error = known != null ? known : translator.translateStatus(status);
        }
        if (status >= 500) {
            log.error("Error response status={} path={}", status, path, exception);
        }
        else {
            log.warn("Error response status={} path={}", status, path);
        }
        return Responses.of(properties, error);
    }

    private int status(WebRequest request) {
        Object value = first(request, ERROR_STATUS);
        if (value instanceof Integer) {
            return ((Integer) value);
        }
        return 500;
    }

    private Object first(WebRequest request, String[] names) {
        for (String name : names) {
            Object value = request.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
