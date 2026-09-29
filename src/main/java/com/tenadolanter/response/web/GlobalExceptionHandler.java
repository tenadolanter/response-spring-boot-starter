package com.tenadolanter.response.web;

import com.tenadolanter.response.BusinessException;
import com.tenadolanter.response.ResponseProperties;
import com.tenadolanter.response.Result;
import com.tenadolanter.response.compat.Calls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Converts controller exceptions into the unified response.
 * Lowest precedence, so an application's own {@code @RestControllerAdvice} can handle the exceptions it declares first.
 */
@RestControllerAdvice
@Order
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ResponseProperties properties;

    private final ResultExceptionTranslator translator;

    public GlobalExceptionHandler(ResponseProperties properties) {
        this.properties = properties;
        this.translator = new ResultExceptionTranslator(properties);
    }

    @ExceptionHandler(Exception.class)
    @SuppressWarnings("unused")
    public ResponseEntity<Result<?>> handle(Exception exception) throws Exception {
        HandlerMethod handler = currentHandler();
        if (handler == null || !ControllerScopes.unified(properties, handler)) {
            if (handler != null) {
                markRawError();
            }
            throw exception;
        }
        TranslatedError error = translator.translate(exception);
        if (exception instanceof BusinessException) {
            BusinessException business = (BusinessException) exception;
            log.warn("Business exception traceId={} code={} message={}", traceId(), business.getCode(), business.getMessage());
        }
        else if (error.httpStatus() >= 500 && error.httpStatus() <= 599) {
            log.error("Unhandled exception traceId={}", traceId(), exception);
        }
        else {
            log.warn("Request error traceId={} message={}", traceId(), exception.getMessage());
        }
        return Responses.of(properties, error);
    }

    /**
     * Only client-facing endpoints are rewritten. Server-facing and unadopted endpoints rethrow so the HTTP status is preserved.
     */
    private HandlerMethod currentHandler() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return null;
        }
        Object request = Calls.invoke(attributes, "getRequest");
        Object handler = Calls.invoke(request, "getAttribute", HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (handler instanceof HandlerMethod) {
            return (HandlerMethod) handler;
        }
        return null;
    }

    private void markRawError() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.setAttribute(ControllerScopes.RAW_ERROR, Boolean.TRUE, RequestAttributes.SCOPE_REQUEST);
        }
    }

    private String traceId() {
        return MDC.get(properties.getTraceMdcKey());
    }
}
