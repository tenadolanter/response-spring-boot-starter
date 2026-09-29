package com.tenadolanter.response;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified response settings. Prefix: {@code tenadolanter.response}.
 */
@ConfigurationProperties(prefix = "tenadolanter.response")
public class ResponseProperties {

    /**
     * Whether the unified response component is enabled.
     */
    private boolean enabled = true;

    /**
     * Scope. {@code annotation} handles only endpoints marked with {@code @ResultResponse}, for incremental adoption.
     * {@code all} handles every controller.
     */
    private ResponseMode mode = ResponseMode.ANNOTATION;

    /**
     * Whether the global exception handler is enabled.
     */
    private boolean exceptionHandlerEnabled = true;

    /**
     * Whether errors that never reach a controller, such as an unmapped path, use the unified response.
     * This is application-wide and defaults to off so existing error pages stay in place.
     */
    private boolean errorControllerEnabled = false;

    /**
     * Whether to create a trace id and write it to the response header and body.
     */
    private boolean traceEnabled = true;

    /**
     * Request header used to read and write the trace id. Existing projects can switch it, for example to {@code X-Request-Id}.
     */
    private String traceHeader = TraceIds.HEADER;

    /**
     * SLF4J MDC key. It must match the placeholder in the project's log pattern.
     */
    private String traceMdcKey = TraceIds.MDC_KEY;

    /**
     * Business code used for a successful response.
     */
    private int successCode = CommonResultCode.SUCCESS.getCode();

    /**
     * Default message used for a successful response.
     */
    private String successMessage = CommonResultCode.SUCCESS.getMessage();

    /**
     * Message returned to the caller for an unknown exception.
     */
    private String defaultErrorMessage = CommonResultCode.INTERNAL_ERROR.getMessage();

    /**
     * Whether the unknown exception's own message is returned to the caller. Leave this off in production.
     */
    private boolean exposeExceptionMessage = false;

    /**
     * When true, error responses also use HTTP 200 and the outcome is distinguished only by {@code body.code}.
     */
    private boolean alwaysOk = false;

    /**
     * Ant-style paths excluded from wrapping. Declaring this list in configuration replaces the defaults entirely.
     */
    private List<String> excludePaths = defaultExcludePaths();

    /**
     * JSON field names. A blank value omits that field.
     */
    private Fields fields = new Fields();

    private static List<String> defaultExcludePaths() {
        List<String> paths = new ArrayList<>();
        paths.add("/actuator/**");
        paths.add("/v3/api-docs/**");
        paths.add("/swagger-ui/**");
        paths.add("/swagger-ui.html");
        paths.add("/webjars/**");
        return paths;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ResponseMode getMode() {
        return mode;
    }

    public void setMode(ResponseMode mode) {
        this.mode = mode;
    }

    public boolean isExceptionHandlerEnabled() {
        return exceptionHandlerEnabled;
    }

    public void setExceptionHandlerEnabled(boolean exceptionHandlerEnabled) {
        this.exceptionHandlerEnabled = exceptionHandlerEnabled;
    }

    public boolean isErrorControllerEnabled() {
        return errorControllerEnabled;
    }

    public void setErrorControllerEnabled(boolean errorControllerEnabled) {
        this.errorControllerEnabled = errorControllerEnabled;
    }

    public boolean isTraceEnabled() {
        return traceEnabled;
    }

    public void setTraceEnabled(boolean traceEnabled) {
        this.traceEnabled = traceEnabled;
    }

    public String getTraceHeader() {
        return textOrDefault(traceHeader, TraceIds.HEADER);
    }

    public void setTraceHeader(String traceHeader) {
        this.traceHeader = traceHeader;
    }

    public String getTraceMdcKey() {
        return textOrDefault(traceMdcKey, TraceIds.MDC_KEY);
    }

    public void setTraceMdcKey(String traceMdcKey) {
        this.traceMdcKey = traceMdcKey;
    }

    public int getSuccessCode() {
        return successCode;
    }

    public void setSuccessCode(int successCode) {
        this.successCode = successCode;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }

    public String getDefaultErrorMessage() {
        return defaultErrorMessage;
    }

    public void setDefaultErrorMessage(String defaultErrorMessage) {
        this.defaultErrorMessage = defaultErrorMessage;
    }

    public boolean isExposeExceptionMessage() {
        return exposeExceptionMessage;
    }

    public void setExposeExceptionMessage(boolean exposeExceptionMessage) {
        this.exposeExceptionMessage = exposeExceptionMessage;
    }

    public boolean isAlwaysOk() {
        return alwaysOk;
    }

    public void setAlwaysOk(boolean alwaysOk) {
        this.alwaysOk = alwaysOk;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public Fields getFields() {
        if (fields == null) {
            fields = new Fields();
        }
        return fields;
    }

    public void setFields(Fields fields) {
        this.fields = fields;
    }

    /**
     * JSON field names of the unified response.
     */
    public static class Fields {

        private String code = "code";

        private String message = "message";

        private String data = "data";

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }

    private static String textOrDefault(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }
}
