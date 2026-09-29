package com.tenadolanter.response;

/**
 * Scope of the unified response.
 */
public enum ResponseMode {

    /**
     * Wrap return values and translate exceptions for every controller.
     */
    ALL,

    /**
     * Apply only to types or methods marked with {@link com.tenadolanter.response.annotation.ResultResponse}.
     */
    ANNOTATION
}
