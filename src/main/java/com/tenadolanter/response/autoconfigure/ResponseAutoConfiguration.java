package com.tenadolanter.response.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenadolanter.response.ResponseProperties;
import com.tenadolanter.response.Result;
import com.tenadolanter.response.compat.Calls;
import com.tenadolanter.response.compat.ServletApi;
import com.tenadolanter.response.compat.TraceIdFilter;
import com.tenadolanter.response.compat.TraceIdListener;
import com.tenadolanter.response.web.GlobalExceptionHandler;
import com.tenadolanter.response.web.ResponseWrapAdvice;
import com.tenadolanter.response.web.ResultErrorController;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.EnumSet;

/**
 * Registers response wrapping, the global exception handler, and trace id support.
 * Uses {@code @Configuration} instead of {@code @AutoConfiguration} so versions before Spring Boot 2.7 can load it.
 */
@Configuration
@AutoConfigureBefore(ErrorMvcAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@EnableConfigurationProperties(ResponseProperties.class)
@ConditionalOnProperty(prefix = "tenadolanter.response", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ResponseAutoConfiguration {

    @Bean
    ResultConfigurer resultConfigurer(ResponseProperties properties) {
        return new ResultConfigurer(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ResponseWrapAdvice responseWrapAdvice(ResponseProperties properties, ObjectProvider<ObjectMapper> objectMapper) {
        return new ResponseWrapAdvice(properties, objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "tenadolanter.response", name = "exception-handler-enabled", havingValue = "true", matchIfMissing = true)
    public GlobalExceptionHandler globalExceptionHandler(ResponseProperties properties) {
        return new GlobalExceptionHandler(properties);
    }

    @Bean
    @ConditionalOnMissingBean(ErrorController.class)
    @ConditionalOnProperty(prefix = "tenadolanter.response", name = "error-controller-enabled", havingValue = "true", matchIfMissing = true)
    public ResultErrorController resultErrorController(ResponseProperties properties) {
        return new ResultErrorController(properties);
    }

    /**
     * Creates the trace id when the request is created, before filters run. {@code setListener} is invoked by reflection because its parameter type differs between javax and jakarta.
     */
    @Bean
    @ConditionalOnProperty(prefix = "tenadolanter.response", name = "trace-enabled", havingValue = "true", matchIfMissing = true)
    @SuppressWarnings("rawtypes")
    public ServletListenerRegistrationBean traceIdListenerRegistration(ResponseProperties properties) {
        ClassLoader loader = ResponseAutoConfiguration.class.getClassLoader();
        ServletListenerRegistrationBean registration = new ServletListenerRegistrationBean();
        Calls.invoke(registration, "setListener", TraceIdListener.create(loader, properties.getTraceHeader(), properties.getTraceMdcKey()));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    /**
     * {@code setFilter} and {@code setDispatcherTypes} are invoked by reflection because their parameter types differ between javax and jakarta.
     */
    @Bean
    @ConditionalOnProperty(prefix = "tenadolanter.response", name = "trace-enabled", havingValue = "true", matchIfMissing = true)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public FilterRegistrationBean traceIdFilterRegistration(ResponseProperties properties) {
        ClassLoader loader = ResponseAutoConfiguration.class.getClassLoader();
        FilterRegistrationBean registration = new FilterRegistrationBean();
        Calls.invoke(registration, "setFilter", TraceIdFilter.create(loader, properties.getTraceHeader(), properties.getTraceMdcKey()));
        Class dispatcherType = ServletApi.load("DispatcherType", loader);
        Calls.invoke(registration, "setDispatcherTypes", EnumSet.allOf(dispatcherType));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        registration.setName("traceIdFilter");
        return registration;
    }

    static final class ResultConfigurer {

        ResultConfigurer(ResponseProperties properties) {
            Result.configure(properties);
        }
    }
}
