package com.tenadolanter.response.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenadolanter.response.ResponseProperties;
import com.tenadolanter.response.Result;
import com.tenadolanter.response.TraceIds;
import com.tenadolanter.response.annotation.RawResponse;
import com.tenadolanter.response.compat.Calls;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.ResourceRegionHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wraps return values of controllers marked with {@link com.tenadolanter.response.annotation.ResultResponse} as {@link Result}.
 * Values that are already a Result, marked with {@link RawResponse}, or are downloads, streams, or error statuses are left unchanged.
 */
@ControllerAdvice
@Order
public class ResponseWrapAdvice implements ResponseBodyAdvice<Object> {

    private static final MediaType APPLICATION_JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);

    private static final MediaType PROBLEM_JSON = MediaType.parseMediaType("application/problem+json");

    private static final String PROBLEM_DETAIL = "org.springframework.http.ProblemDetail";

    private final ResponseProperties properties;

    private final ObjectMapper objectMapper;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final List<String> excludePaths;

    public ResponseWrapAdvice(ResponseProperties properties, ObjectProvider<ObjectMapper> objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper.getIfAvailable(ObjectMapper::new);
        List<String> configured = properties.getExcludePaths();
        if (configured == null) {
            this.excludePaths = Collections.emptyList();
        }
        else {
            this.excludePaths = Collections.unmodifiableList(new ArrayList<>(configured));
        }
    }

    @Override
    public boolean supports(MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        if (returnType.hasMethodAnnotation(RawResponse.class)
                || AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), RawResponse.class)) {
            return false;
        }
        if (!ControllerScopes.optedIn(properties, returnType)) {
            return false;
        }
        return !isSkippedConverter(converterType) && !isSkippedType(returnType.getParameterType());
    }

    @Override
    public Object beforeBodyWrite(Object body, @NonNull MethodParameter returnType, @NonNull MediaType selectedContentType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response) {
        if (excluded(request) || skippedContent(selectedContentType) || alreadyWrapped(body) || notSuccessStatus(response)) {
            return body;
        }
        Result<Object> result = Result.ok(body);
        fillTraceId(result, request);
        if (StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
            response.getHeaders().setContentType(APPLICATION_JSON_UTF8);
            try {
                return objectMapper.writeValueAsString(result);
            }
            catch (JsonProcessingException exception) {
                throw new IllegalStateException("Failed to serialize unified response", exception);
            }
        }
        return result;
    }

    private boolean excluded(ServerHttpRequest request) {
        if (!(request instanceof ServletServerHttpRequest) || excludePaths.isEmpty()) {
            return false;
        }
        String path = pathWithinApplication(Calls.invoke(request, "getServletRequest"));
        for (String pattern : excludePaths) {
            if (pattern != null && !pattern.trim().isEmpty() && pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private String pathWithinApplication(Object request) {
        String uri = (String) Calls.invoke(request, "getRequestURI");
        String contextPath = (String) Calls.invoke(request, "getContextPath");
        if (contextPath != null && !contextPath.isEmpty() && uri != null && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        if (uri == null || uri.isEmpty()) {
            return "/";
        }
        return uri;
    }

    private boolean skippedContent(MediaType mediaType) {
        if (mediaType == null) {
            return false;
        }
        return MediaType.TEXT_EVENT_STREAM.includes(mediaType)
                || MediaType.APPLICATION_OCTET_STREAM.includes(mediaType)
                || MediaType.MULTIPART_FORM_DATA.includes(mediaType)
                || PROBLEM_JSON.includes(mediaType)
                || "image".equals(mediaType.getType());
    }

    private boolean alreadyWrapped(Object body) {
        return body instanceof Result
                || body instanceof Resource
                || body instanceof byte[]
                || body instanceof StreamingResponseBody
                || isProblemDetail(body == null ? null : body.getClass());
    }

    private boolean notSuccessStatus(ServerHttpResponse response) {
        if (!(response instanceof ServletServerHttpResponse)) {
            return false;
        }
        Object servletResponse = Calls.invoke(response, "getServletResponse");
        int status = ((Number) Calls.invoke(servletResponse, "getStatus")).intValue();
        return status >= 300;
    }

    private void fillTraceId(Result<?> result, ServerHttpRequest request) {
        if (!properties.isTraceEnabled() || hasText(result.getTraceId())) {
            return;
        }
        String traceId = MDC.get(properties.getTraceMdcKey());
        if (!hasText(traceId) && request instanceof ServletServerHttpRequest) {
            Object servletRequest = Calls.invoke(request, "getServletRequest");
            Object value = Calls.invoke(servletRequest, "getAttribute", TraceIds.ATTRIBUTE);
            if (value instanceof String) {
                traceId = (String) value;
            }
        }
        if (hasText(traceId)) {
            result.setTraceId(traceId);
        }
    }

    private boolean isSkippedConverter(Class<? extends HttpMessageConverter<?>> converterType) {
        return ByteArrayHttpMessageConverter.class.isAssignableFrom(converterType)
                || ResourceHttpMessageConverter.class.isAssignableFrom(converterType)
                || ResourceRegionHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    private boolean isSkippedType(Class<?> type) {
        return Result.class.isAssignableFrom(type)
                || Resource.class.isAssignableFrom(type)
                || byte[].class.equals(type)
                || StreamingResponseBody.class.isAssignableFrom(type)
                || isProblemDetail(type);
    }

    private boolean isProblemDetail(Class<?> type) {
        Class<?> current = type;
        while (current != null) {
            if (PROBLEM_DETAIL.equals(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
