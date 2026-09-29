package com.tenadolanter.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.slf4j.MDC;

import java.io.IOException;
import java.io.Serializable;

/**
 * Unified response body.
 *
 * <pre>
 * {
 *   "code": 0,
 *   "message": "OK",
 *   "data": {},
 *   "timestamp": 1710000000000,
 *   "traceId": "..."
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(using = Result.Writer.class)
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static volatile int successCode = CommonResultCode.SUCCESS.getCode();

    private static volatile String successMessage = CommonResultCode.SUCCESS.getMessage();

    private static volatile boolean traceEnabled = true;

    private static volatile String traceMdcKey = TraceIds.MDC_KEY;

    private static volatile String codeField = "code";

    private static volatile String messageField = "message";

    private static volatile String dataField = "data";

    private int code;

    private String message;

    private T data;

    private long timestamp;

    private String traceId;

    public Result() {
    }

    /**
     * Called by auto-configuration at startup so the static factories follow the configured values.
     */
    public static void configure(ResponseProperties properties) {
        successCode = properties.getSuccessCode();
        successMessage = properties.getSuccessMessage();
        traceEnabled = properties.isTraceEnabled();
        traceMdcKey = properties.getTraceMdcKey();
        ResponseProperties.Fields fields = properties.getFields();
        codeField = fieldName(fields.getCode(), "code");
        messageField = fieldName(fields.getMessage(), "message");
        dataField = fieldName(fields.getData(), "data");
    }

    private static String fieldName(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        return value.trim();
    }

    public static <T> Result<T> ok() {
        return of(successCode, successMessage, null);
    }

    public static <T> Result<T> ok(T data) {
        return of(successCode, successMessage, data);
    }

    public static <T> Result<T> ok(T data, String message) {
        return of(successCode, message, data);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return of(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return of(resultCode.getCode(), message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return of(code, message, null);
    }

    private static <T> Result<T> of(int code, String message, T data) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        result.data = data;
        result.timestamp = System.currentTimeMillis();
        result.traceId = currentTraceId();
        return result;
    }

    private static String currentTraceId() {
        if (!traceEnabled) {
            return null;
        }
        String value = MDC.get(traceMdcKey);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value;
    }

    @JsonIgnore
    public boolean isSuccess() {
        return this.code == successCode;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    /**
     * Writes JSON using the configured field names. A blank name omits that field. A null trace id is omitted.
     */
    public static final class Writer extends JsonSerializer<Result<?>> {

        @Override
        public void serialize(Result<?> value, JsonGenerator generator, SerializerProvider serializers) throws IOException {
            generator.writeStartObject();
            write(generator, serializers, codeField, value.code, true);
            write(generator, serializers, messageField, value.message, true);
            write(generator, serializers, dataField, value.data, true);
            write(generator, serializers, "timestamp", value.timestamp, true);
            write(generator, serializers, traceMdcKey, value.traceId, false);
            generator.writeEndObject();
        }

        private static void write(JsonGenerator generator, SerializerProvider serializers, String name, Object fieldValue,
                                  boolean includeNull) throws IOException {
            if (name == null || name.isEmpty()) {
                return;
            }
            if (fieldValue == null && !includeNull) {
                return;
            }
            generator.writeFieldName(name);
            serializers.defaultSerializeValue(fieldValue, generator);
        }
    }
}
