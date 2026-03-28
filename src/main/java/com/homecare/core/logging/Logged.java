package com.homecare.core.logging;

import java.lang.annotation.*;

/**
 * Marks a method or class for detailed logging by {@link LoggingAspect}.
 * <p>
 * When placed on a class, all public methods of that class are logged.
 * When placed on a method, only that method is logged at the specified level.
 *
 * <pre>
 * &#64;Logged                           // defaults: log args + result, INFO level
 * &#64;Logged(level = "DEBUG")          // debug-level entry/exit
 * &#64;Logged(logArgs = false)          // hide argument values (PII etc.)
 * &#64;Logged(logResult = false)        // hide return value
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Logged {

    /** SLF4J level name: TRACE, DEBUG, INFO, WARN, ERROR. Default INFO. */
    String level() default "INFO";

    /** Whether to include method argument values in the log. */
    boolean logArgs() default true;

    /** Whether to include the return value in the log. */
    boolean logResult() default true;
}

