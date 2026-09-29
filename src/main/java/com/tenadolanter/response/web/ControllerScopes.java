package com.tenadolanter.response.web;

import com.tenadolanter.response.ResponseMode;
import com.tenadolanter.response.ResponseProperties;
import com.tenadolanter.response.annotation.RawResponse;
import com.tenadolanter.response.annotation.ResultResponse;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

/**
 * Decides whether the current endpoint uses the unified response, based on {@link ResponseMode}.
 * {@link RawResponse} marks a server-facing endpoint: success returns the raw data and failure keeps the HTTP status.
 */
final class ControllerScopes {

    static final String RAW_ERROR = "com.tenadolanter.response.rawError";

    private ControllerScopes() {
    }

    static boolean unified(ResponseProperties properties, HandlerMethod handlerMethod) {
        if (raw(handlerMethod.getMethod(), handlerMethod.getBeanType())) {
            return false;
        }
        return optedIn(properties, handlerMethod);
    }

    private static boolean raw(Method method, Class<?> type) {
        if (method != null && AnnotatedElementUtils.hasAnnotation(method, RawResponse.class)) {
            return true;
        }
        return type != null && AnnotatedElementUtils.hasAnnotation(type, RawResponse.class);
    }

    static boolean optedIn(ResponseProperties properties, MethodParameter returnType) {
        return optedIn(properties, returnType.getMethod(), returnType.getContainingClass());
    }

    static boolean optedIn(ResponseProperties properties, HandlerMethod handlerMethod) {
        return optedIn(properties, handlerMethod.getMethod(), handlerMethod.getBeanType());
    }

    private static boolean optedIn(ResponseProperties properties, Method method, Class<?> type) {
        if (properties.getMode() == ResponseMode.ALL) {
            return true;
        }
        if (method != null && AnnotatedElementUtils.hasAnnotation(method, ResultResponse.class)) {
            return true;
        }
        return type != null && AnnotatedElementUtils.hasAnnotation(type, ResultResponse.class);
    }
}
