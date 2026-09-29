package com.tenadolanter.response.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opts this type or method into the unified response.
 * Unmarked controllers keep their existing return values and exception handling, so a project can adopt the component one endpoint at a time.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResultResponse {
}
