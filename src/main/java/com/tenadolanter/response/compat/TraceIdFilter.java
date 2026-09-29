package com.tenadolanter.response.compat;

import com.tenadolanter.response.TraceIds;
import org.slf4j.MDC;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Writes the existing trace id to the response header.
 * The id itself is created by {@link TraceIdListener} before the filter chain.
 * A dynamic proxy implements the Servlet Filter interface, choosing jakarta or javax from the classpath.
 * An error or async dispatch enters the filter again and reuses the same trace id.
 * MDC is removed when this dispatch returns, so the container thread does not carry it back to the pool.
 */
public final class TraceIdFilter implements InvocationHandler {

    private static final Pattern SAFE_TRACE_ID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    private static final String ALREADY_FILTERED = TraceIdFilter.class.getName() + ".FILTERED";

    private static final String ASYNC_LISTENER = TraceIdFilter.class.getName() + ".ASYNC_LISTENER";

    private final String header;

    private final String mdcKey;

    private TraceIdFilter(String header, String mdcKey) {
        this.header = header;
        this.mdcKey = mdcKey;
    }

    /**
     * Returns an instance of {@code jakarta.servlet.Filter} or {@code javax.servlet.Filter}.
     */
    public static Object create(ClassLoader loader) {
        return create(loader, TraceIds.HEADER, TraceIds.MDC_KEY);
    }

    public static Object create(ClassLoader loader, String header, String mdcKey) {
        Class<?> filterType = ServletApi.load("Filter", loader);
        return Proxy.newProxyInstance(filterType.getClassLoader(), new Class<?>[] {filterType}, new TraceIdFilter(header, mdcKey));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        switch (method.getName()) {
            case "doFilter":
                if (args != null && args.length == 3) {
                    doFilter(args[0], args[1], args[2]);
                }
                return null;
            case "equals":
                return proxy == args[0];
            case "hashCode":
                return System.identityHashCode(proxy);
            case "toString":
                return "TraceIdFilter";
            default:
                return null;
        }
    }

    private void doFilter(Object request, Object response, Object chain) throws Throwable {
        if (Calls.invoke(request, "getAttribute", ALREADY_FILTERED) != null) {
            proceed(chain, request, response);
            return;
        }
        String traceId = bind(request, header, mdcKey);
        Calls.invoke(request, "setAttribute", ALREADY_FILTERED, Boolean.TRUE);
        Calls.invoke(response, "setHeader", header, traceId);
        try {
            proceed(chain, request, response);
        }
        finally {
            watchAsync(request);
            Calls.invoke(request, "removeAttribute", ALREADY_FILTERED);
            MDC.remove(mdcKey);
        }
    }

    /**
     * Async completion can run on another thread. Clear MDC there as well, after the container thread has already been cleared.
     */
    private void watchAsync(Object request) {
        if (!Boolean.TRUE.equals(Calls.invoke(request, "isAsyncStarted"))) {
            return;
        }
        if (Calls.invoke(request, "getAttribute", ASYNC_LISTENER) != null) {
            return;
        }
        Object asyncContext = Calls.invoke(request, "getAsyncContext");
        if (asyncContext == null) {
            return;
        }
        Calls.invoke(request, "setAttribute", ASYNC_LISTENER, Boolean.TRUE);
        Calls.invoke(asyncContext, "addListener", asyncListener());
    }

    private Object asyncListener() {
        ClassLoader loader = TraceIdFilter.class.getClassLoader();
        Class<?> listenerType = ServletApi.load("AsyncListener", loader);
        String key = mdcKey;
        return Proxy.newProxyInstance(listenerType.getClassLoader(), new Class<?>[] {listenerType}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "onComplete":
                case "onTimeout":
                case "onError":
                    MDC.remove(key);
                    return null;
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "TraceIdAsyncListener";
                default:
                    return null;
            }
        });
    }

    static String bind(Object request, String header, String mdcKey) {
        String traceId = resolveTraceId(request, header);
        Calls.invoke(request, "setAttribute", TraceIds.ATTRIBUTE, traceId);
        MDC.put(mdcKey, traceId);
        return traceId;
    }

    private void proceed(Object chain, Object request, Object response) throws Throwable {
        try {
            Calls.invoke(chain, "doFilter", request, response);
        }
        catch (Calls.CheckedException exception) {
            throw exception.getCause();
        }
    }

    private static String resolveTraceId(Object request, String header) {
        Object existing = Calls.invoke(request, "getAttribute", TraceIds.ATTRIBUTE);
        if (existing instanceof String && SAFE_TRACE_ID.matcher((String) existing).matches()) {
            return (String) existing;
        }
        Object incoming = Calls.invoke(request, "getHeader", header);
        if (incoming instanceof String && SAFE_TRACE_ID.matcher((String) incoming).matches()) {
            return (String) incoming;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
