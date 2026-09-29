package com.tenadolanter.response.compat;

import com.tenadolanter.response.TraceIds;
import org.slf4j.MDC;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Creates the trace id before the filter chain and stores it in MDC and a request attribute.
 * It runs when the request is created, so errors that happen before the filter can still read the trace id from MDC.
 * The filter removes MDC when its own dispatch returns, including an async dispatch, so a container thread is clean before it is reused.
 */
public final class TraceIdListener implements InvocationHandler {

    private final String header;

    private final String mdcKey;

    private TraceIdListener(String header, String mdcKey) {
        this.header = header;
        this.mdcKey = mdcKey;
    }

    /**
     * Returns an instance of {@code jakarta.servlet.ServletRequestListener} or {@code javax.servlet.ServletRequestListener}.
     */
    public static Object create(ClassLoader loader, String header, String mdcKey) {
        Class<?> listenerType = ServletApi.load("ServletRequestListener", loader);
        return Proxy.newProxyInstance(listenerType.getClassLoader(), new Class<?>[] {listenerType}, new TraceIdListener(header, mdcKey));
    }

    public static Object create(ClassLoader loader) {
        return create(loader, TraceIds.HEADER, TraceIds.MDC_KEY);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
            case "requestInitialized":
                if (args != null && args.length == 1) {
                    TraceIdFilter.bind(Calls.invoke(args[0], "getServletRequest"), header, mdcKey);
                }
                return null;
            case "requestDestroyed":
                MDC.remove(mdcKey);
                return null;
            case "equals":
                return proxy == args[0];
            case "hashCode":
                return System.identityHashCode(proxy);
            case "toString":
                return "TraceIdListener";
            default:
                return null;
        }
    }
}
