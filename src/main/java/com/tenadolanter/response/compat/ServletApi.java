package com.tenadolanter.response.compat;

import org.springframework.util.ClassUtils;

/**
 * Locates Servlet API classes at runtime. Spring Boot 3 uses jakarta.servlet; Spring Boot 2 uses javax.servlet.
 */
public final class ServletApi {

    private static final String[] PACKAGES = {"jakarta.servlet.", "javax.servlet."};

    private ServletApi() {
    }

    public static Class<?> load(String simpleName, ClassLoader loader) {
        for (String prefix : PACKAGES) {
            String className = prefix + simpleName;
            if (ClassUtils.isPresent(className, loader)) {
                return ClassUtils.resolveClassName(className, loader);
            }
        }
        throw new IllegalStateException("Servlet API not found on classpath");
    }

    /**
     * Returns the Jakarta and javax names of one servlet error attribute.
     */
    public static String[] errorAttributes(String name) {
        return new String[] {"jakarta.servlet.error." + name, "javax.servlet.error." + name};
    }
}
