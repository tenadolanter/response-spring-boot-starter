package com.tenadolanter.response.web;

import com.tenadolanter.response.TraceIds;
import com.tenadolanter.response.compat.Calls;
import com.tenadolanter.response.compat.ServletApi;
import com.tenadolanter.response.compat.TraceIdFilter;
import com.tenadolanter.response.compat.TraceIdListener;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The filter is a dynamic proxy. Tests call it by reflection and do not import javax or jakarta.
 */
class TraceIdFilterTest {

    private final ClassLoader loader = getClass().getClassLoader();

    private final Object filter = TraceIdFilter.create(loader);

    @Test
    void implementsServletFilter() {
        assertThat(ServletApi.load("Filter", loader).isInstance(filter)).isTrue();
    }

    @Test
    void reusesSafeIncomingTraceId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIds.HEADER, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> duringRequest = new AtomicReference<>();

        Calls.invoke(filter, "doFilter", request, response, chain(() -> duringRequest.set(MDC.get(TraceIds.MDC_KEY))));

        assertThat(response.getHeader(TraceIds.HEADER)).isEqualTo("abc-123");
        assertThat(request.getAttribute(TraceIds.ATTRIBUTE)).isEqualTo("abc-123");
        assertThat(duringRequest.get()).isEqualTo("abc-123");
        assertThat(MDC.get(TraceIds.MDC_KEY)).isNull();
    }

    @Test
    void usesConfiguredHeaderAndMdcKey() {
        Object custom = TraceIdFilter.create(loader, "X-Request-Id", "requestId");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "req-9");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> duringRequest = new AtomicReference<>();

        Calls.invoke(custom, "doFilter", request, response, chain(() -> duringRequest.set(MDC.get("requestId"))));

        assertThat(response.getHeader("X-Request-Id")).isEqualTo("req-9");
        assertThat(duringRequest.get()).isEqualTo("req-9");
        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get(TraceIds.MDC_KEY)).isNull();
    }

    @Test
    void replacesUnsafeTraceId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIds.HEADER, "bad id\r\n");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Calls.invoke(filter, "doFilter", request, response, chain(() -> {
        }));

        assertThat(response.getHeader(TraceIds.HEADER)).matches("^[a-f0-9]{32}$");
        assertThat(MDC.get(TraceIds.MDC_KEY)).isNull();
    }

    @Test
    void listenerBindsTraceIdBeforeFilter() throws Exception {
        Object listener = TraceIdListener.create(loader);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIds.HEADER, "abc-123");
        Object event = requestEvent(request);

        Calls.invoke(listener, "requestInitialized", event);

        assertThat(MDC.get(TraceIds.MDC_KEY)).isEqualTo("abc-123");
        assertThat(request.getAttribute(TraceIds.ATTRIBUTE)).isEqualTo("abc-123");

        MockHttpServletResponse response = new MockHttpServletResponse();
        Calls.invoke(filter, "doFilter", request, response, chain(() -> {
        }));

        assertThat(response.getHeader(TraceIds.HEADER)).isEqualTo("abc-123");
        assertThat(MDC.get(TraceIds.MDC_KEY)).isNull();

        Calls.invoke(listener, "requestDestroyed", event);
        assertThat(MDC.get(TraceIds.MDC_KEY)).isNull();
    }

    @Test
    void asyncStartClearsMdcOnContainerThreadAndOnCompletion() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAsyncSupported(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        Calls.invoke(filter, "doFilter", request, response, chain(request::startAsync));

        assertThat(MDC.get(TraceIds.MDC_KEY)).isNull();
        MDC.put(TraceIds.MDC_KEY, "copied-to-async-thread");
        Objects.requireNonNull(request.getAsyncContext()).complete();
        assertThat(MDC.get(TraceIds.MDC_KEY)).isNull();
    }

    @Test
    void errorDispatchKeepsSameTraceId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> first = new AtomicReference<>();
        AtomicReference<String> second = new AtomicReference<>();

        Calls.invoke(filter, "doFilter", request, response, chain(() -> first.set(MDC.get(TraceIds.MDC_KEY))));
        Calls.invoke(filter, "doFilter", request, response, chain(() -> second.set(MDC.get(TraceIds.MDC_KEY))));

        assertThat(first.get()).isNotBlank();
        assertThat(second.get()).isEqualTo(first.get());
    }

    private Object requestEvent(MockHttpServletRequest request) throws Exception {
        Class<?> eventType = ServletApi.load("ServletRequestEvent", loader);
        return eventType.getConstructor(ServletApi.load("ServletContext", loader), ServletApi.load("ServletRequest", loader))
                .newInstance(request.getServletContext(), request);
    }

    private Object chain(Runnable action) {
        Class<?> chainType = ServletApi.load("FilterChain", loader);
        return Proxy.newProxyInstance(chainType.getClassLoader(), new Class<?>[] {chainType}, (proxy, method, args) -> {
            switch (method.getName()) {
                case "doFilter":
                    action.run();
                    return null;
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "FilterChain";
                default:
                    return null;
            }
        });
    }
}
