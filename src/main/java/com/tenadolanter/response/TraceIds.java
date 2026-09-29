package com.tenadolanter.response;

/**
 * Default trace id keys. The header and MDC key can be overridden with {@code tenadolanter.response.trace-header} and {@code tenadolanter.response.trace-mdc-key}.
 */
public final class TraceIds {

    public static final String HEADER = "X-Trace-Id";

    public static final String MDC_KEY = "traceId";

    public static final String ATTRIBUTE = "com.tenadolanter.response.traceId";

    private TraceIds() {
    }
}
