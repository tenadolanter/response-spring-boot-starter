package com.tenadolanter.response.web;

import com.tenadolanter.response.BusinessException;
import com.tenadolanter.response.CommonResultCode;
import com.tenadolanter.response.ResponseProperties;
import com.tenadolanter.response.Result;
import com.tenadolanter.response.compat.Calls;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * Identifies exception types that exist on only Boot 2 or Boot 3 by class name, so shared code does not reference a missing class.
 */
final class ResultExceptionTranslator {

    private static final String NO_RESOURCE_FOUND = "org.springframework.web.servlet.resource.NoResourceFoundException";

    private static final String HANDLER_METHOD_VALIDATION = "org.springframework.web.method.annotation.HandlerMethodValidationException";

    private static final String[] CONSTRAINT_VIOLATION = {
            "jakarta.validation.ConstraintViolationException",
            "javax.validation.ConstraintViolationException"
    };

    private final ResponseProperties properties;

    ResultExceptionTranslator(ResponseProperties properties) {
        this.properties = properties;
    }

    TranslatedError translate(Throwable exception) {
        TranslatedError known = translateKnown(exception);
        if (known != null) {
            return known;
        }
        return internalError(exception);
    }

    TranslatedError translateKnown(Throwable exception) {
        if (exception instanceof BusinessException) {
            BusinessException business = (BusinessException) exception;
            return new TranslatedError(business.getCode(), Result.fail(business.getCode(), business.getMessage()));
        }
        if (exception instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException invalid = (MethodArgumentNotValidException) exception;
            return client(CommonResultCode.BAD_REQUEST.getCode(), fieldErrors(invalid.getBindingResult()));
        }
        if (exception instanceof BindException) {
            BindException bind = (BindException) exception;
            return client(CommonResultCode.BAD_REQUEST.getCode(), fieldErrors(bind));
        }
        if (isType(exception, CONSTRAINT_VIOLATION)) {
            return client(CommonResultCode.BAD_REQUEST.getCode(), constraintViolations(exception));
        }
        if (isType(exception, HANDLER_METHOD_VALIDATION)) {
            return client(CommonResultCode.BAD_REQUEST.getCode(), methodValidationErrors(exception));
        }
        if (exception instanceof MissingServletRequestParameterException) {
            MissingServletRequestParameterException missing = (MissingServletRequestParameterException) exception;
            return client(CommonResultCode.BAD_REQUEST.getCode(), "Missing parameter " + missing.getParameterName());
        }
        if (exception instanceof MissingServletRequestPartException) {
            MissingServletRequestPartException missing = (MissingServletRequestPartException) exception;
            return client(CommonResultCode.BAD_REQUEST.getCode(), "Missing request part " + missing.getRequestPartName());
        }
        if (exception instanceof MissingPathVariableException) {
            MissingPathVariableException missing = (MissingPathVariableException) exception;
            return client(CommonResultCode.BAD_REQUEST.getCode(), "Missing path variable " + missing.getVariableName());
        }
        if (exception instanceof MethodArgumentTypeMismatchException) {
            MethodArgumentTypeMismatchException mismatch = (MethodArgumentTypeMismatchException) exception;
            return client(CommonResultCode.BAD_REQUEST.getCode(), "Invalid type for parameter " + mismatch.getName());
        }
        if (exception instanceof HttpMessageNotReadableException) {
            return client(CommonResultCode.BAD_REQUEST.getCode(), "Malformed request body");
        }
        if (exception instanceof MultipartException) {
            return client(CommonResultCode.BAD_REQUEST.getCode(), "Invalid or oversized upload");
        }
        if (exception instanceof HttpRequestMethodNotSupportedException) {
            return client(CommonResultCode.METHOD_NOT_ALLOWED.getCode(), CommonResultCode.METHOD_NOT_ALLOWED.getMessage());
        }
        if (exception instanceof HttpMediaTypeNotSupportedException) {
            return client(CommonResultCode.UNSUPPORTED_MEDIA_TYPE.getCode(), CommonResultCode.UNSUPPORTED_MEDIA_TYPE.getMessage());
        }
        if (exception instanceof HttpMediaTypeNotAcceptableException) {
            return client(CommonResultCode.NOT_ACCEPTABLE.getCode(), CommonResultCode.NOT_ACCEPTABLE.getMessage());
        }
        if (exception instanceof NoHandlerFoundException || isType(exception, NO_RESOURCE_FOUND)) {
            return client(CommonResultCode.NOT_FOUND.getCode(), CommonResultCode.NOT_FOUND.getMessage());
        }
        if (exception instanceof ResponseStatusException) {
            ResponseStatusException statusException = (ResponseStatusException) exception;
            int status = responseStatus(statusException);
            String reason = statusException.getReason();
            if (reason == null || reason.trim().isEmpty()) {
                return translateStatus(status);
            }
            return client(status, reason);
        }
        return null;
    }

    TranslatedError translateStatus(int status) {
        CommonResultCode resultCode = resultCode(status);
        if (resultCode != null) {
            return client(resultCode.getCode(), resultCode.getMessage());
        }
        if (status >= 500) {
            return new TranslatedError(status, Result.fail(status, properties.getDefaultErrorMessage()));
        }
        return client(status, CommonResultCode.BAD_REQUEST.getMessage());
    }

    TranslatedError internalError(Throwable exception) {
        String message = properties.getDefaultErrorMessage();
        if (properties.isExposeExceptionMessage() && exception.getMessage() != null && !exception.getMessage().trim().isEmpty()) {
            message = exception.getMessage();
        }
        return new TranslatedError(CommonResultCode.INTERNAL_ERROR.getCode(), Result.fail(CommonResultCode.INTERNAL_ERROR.getCode(), message));
    }

    private CommonResultCode resultCode(int status) {
        switch (status) {
            case 400:
                return CommonResultCode.BAD_REQUEST;
            case 401:
                return CommonResultCode.UNAUTHORIZED;
            case 403:
                return CommonResultCode.FORBIDDEN;
            case 404:
                return CommonResultCode.NOT_FOUND;
            case 405:
                return CommonResultCode.METHOD_NOT_ALLOWED;
            case 406:
                return CommonResultCode.NOT_ACCEPTABLE;
            case 409:
                return CommonResultCode.CONFLICT;
            case 415:
                return CommonResultCode.UNSUPPORTED_MEDIA_TYPE;
            case 429:
                return CommonResultCode.TOO_MANY_REQUESTS;
            default:
                return null;
        }
    }

    private TranslatedError client(int code, String message) {
        return new TranslatedError(code, Result.fail(code, message));
    }

    private String fieldErrors(BindingResult bindingResult) {
        List<String> messages = new ArrayList<>();
        for (FieldError error : bindingResult.getFieldErrors()) {
            messages.add(error.getField() + " " + error.getDefaultMessage());
        }
        for (ObjectError error : bindingResult.getGlobalErrors()) {
            messages.add(error.getDefaultMessage());
        }
        if (messages.isEmpty()) {
            return CommonResultCode.BAD_REQUEST.getMessage();
        }
        return String.join("; ", messages);
    }

    /**
     * The jakarta.validation and javax.validation ConstraintViolationException types share the same shape and differ only by package.
     */
    private String constraintViolations(Throwable exception) {
        List<String> messages = new ArrayList<>();
        Iterable<?> violations = (Iterable<?>) Calls.invoke(exception, "getConstraintViolations");
        if (violations != null) {
            for (Object violation : violations) {
                messages.add(Calls.invoke(violation, "getPropertyPath") + " " + Calls.invoke(violation, "getMessage"));
            }
        }
        return join(messages);
    }

    /**
     * Thrown since Spring Framework 6.1 when controller method parameters are validated directly.
     */
    private String methodValidationErrors(Throwable exception) {
        List<String> messages = new ArrayList<>();
        Iterable<?> errors = (Iterable<?>) Calls.invoke(exception, "getAllErrors");
        if (errors != null) {
            for (Object error : errors) {
                messages.add(((MessageSourceResolvable) error).getDefaultMessage());
            }
        }
        return join(messages);
    }

    private String join(List<String> messages) {
        if (messages.isEmpty()) {
            return CommonResultCode.BAD_REQUEST.getMessage();
        }
        return String.join("; ", messages);
    }

    /**
     * Boot 2 exposes {@code getStatus()}; Boot 3 exposes {@code getStatusCode()}.
     */
    private int responseStatus(ResponseStatusException exception) {
        Object status = invoke(exception, "getStatusCode");
        if (status == null) {
            status = invoke(exception, "getStatus");
        }
        if (status == null) {
            return 500;
        }
        return ((Number) Calls.invoke(status, "value")).intValue();
    }

    private Object invoke(Object target, String name) {
        try {
            return target.getClass().getMethod(name).invoke(target);
        }
        catch (NoSuchMethodException ignored) {
            return null;
        }
        catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static boolean isType(Throwable exception, String... classNames) {
        Class<?> current = exception.getClass();
        while (current != null) {
            for (String className : classNames) {
                if (className.equals(current.getName())) {
                    return true;
                }
            }
            current = current.getSuperclass();
        }
        return false;
    }
}
