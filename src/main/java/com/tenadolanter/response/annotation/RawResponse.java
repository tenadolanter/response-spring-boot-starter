package com.tenadolanter.response.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a server-facing endpoint. Success is written as the original return value. Failure keeps the HTTP status and is not wrapped in {@code code}/{@code message}.
 * May be placed on a controller type or method. A method annotation still applies when the type already has {@code @ResultResponse}.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RawResponse {
}
