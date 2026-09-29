package com.tenadolanter.response.compat;

import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Invokes a method by name. Shared code does not import javax.servlet or jakarta.servlet,
 * because one class file can bind to only one of those packages.
 */
public final class Calls {

    private Calls() {
    }

    public static Object invoke(Object target, String name, Object... args) {
        Method method = find(target.getClass(), name, args);
        try {
            return method.invoke(target, args);
        }
        catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot invoke " + name, exception);
        }
        catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new CheckedException(cause);
        }
    }

    @SuppressWarnings("deprecation")
    private static Method find(Class<?> type, String name, Object[] args) {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterTypes().length != args.length) {
                continue;
            }
            if (matches(method.getParameterTypes(), args)) {
                return ClassUtils.getInterfaceMethodIfPossible(method);
            }
        }
        throw new IllegalStateException(type.getName() + " has no method " + name);
    }

    private static boolean matches(Class<?>[] types, Object[] args) {
        for (int i = 0; i < types.length; i++) {
            if (args[i] == null) {
                if (types[i].isPrimitive()) {
                    return false;
                }
                continue;
            }
            if (!ClassUtils.isAssignableValue(types[i], args[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checked exception thrown by the invoked method. Callers can rethrow the original cause.
     */
    public static final class CheckedException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        CheckedException(Throwable cause) {
            super(cause);
        }
    }
}
