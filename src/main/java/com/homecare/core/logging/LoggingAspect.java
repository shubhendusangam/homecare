package com.homecare.core.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;


/**
 * Centralized AOP logging aspect that automatically logs:
 * <ul>
 *   <li>Entry + arguments (with sensitive data masking) for every controller,
 *       service, and repository method</li>
 *   <li>Exit + return value + execution time</li>
 *   <li>Exceptions with full context</li>
 *   <li>Slow method detection (>1s WARN, >5s ERROR)</li>
 * </ul>
 * Methods/classes annotated with {@link Logged} get customised verbosity.
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    /** Tracks invocation counts per method for periodic metrics logging. */
    private final ConcurrentHashMap<String, LongAdder> invocationCounts = new ConcurrentHashMap<>();

    private static final int SLOW_THRESHOLD_MS = 1_000;
    private static final int VERY_SLOW_THRESHOLD_MS = 5_000;

    // ─── Pointcuts ────────────────────────────────────────────────────

    @Pointcut("within(com.homecare..controller..*)")
    private void controllerLayer() {}

    @Pointcut("within(com.homecare..service..*)")
    private void serviceLayer() {}

    @Pointcut("within(com.homecare..repository..*)")
    private void repositoryLayer() {}

    @Pointcut("@within(com.homecare.core.logging.Logged) || @annotation(com.homecare.core.logging.Logged)")
    private void loggedAnnotation() {}

    @Pointcut("execution(public * com.homecare..*(..)) && !execution(* com.homecare.core.logging..*(..))")
    private void publicHomecareMethod() {}

    // ─── Advice: Controller methods ───────────────────────────────────

    @Around("controllerLayer() && publicHomecareMethod()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        return logExecution(joinPoint, "CTRL", "DEBUG");
    }

    // ─── Advice: Service methods ──────────────────────────────────────

    @Around("serviceLayer() && publicHomecareMethod()")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
        return logExecution(joinPoint, "SVC", "DEBUG");
    }

    // ─── Advice: Repository methods (TRACE — enabled only in dev) ────

    @Around("repositoryLayer() && publicHomecareMethod()")
    public Object logRepository(ProceedingJoinPoint joinPoint) throws Throwable {
        return logExecution(joinPoint, "REPO", "TRACE");
    }

    // ─── Advice: Explicitly @Logged methods ───────────────────────────

    @Around("loggedAnnotation() && publicHomecareMethod()")
    public Object logAnnotated(ProceedingJoinPoint joinPoint) throws Throwable {
        Logged annotation = getAnnotation(joinPoint);
        String level = annotation != null ? annotation.level() : "INFO";
        return logExecution(joinPoint, "LOG", level,
                annotation == null || annotation.logArgs(),
                annotation == null || annotation.logResult());
    }

    // ─── Core logging logic ──────────────────────────────────────────

    private Object logExecution(ProceedingJoinPoint joinPoint, String layer, String defaultLevel) throws Throwable {
        return logExecution(joinPoint, layer, defaultLevel, true, true);
    }

    private Object logExecution(ProceedingJoinPoint joinPoint, String layer, String defaultLevel,
                                 boolean logArgs, boolean logResult) throws Throwable {
        Logger targetLog = LoggerFactory.getLogger(joinPoint.getTarget().getClass());
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        String className = sig.getDeclaringType().getSimpleName();
        String methodName = className + "." + sig.getName();

        // Track invocation count
        invocationCounts.computeIfAbsent(methodName, k -> new LongAdder()).increment();

        // Don't log if target logger wouldn't emit at this level
        if (!isLevelEnabled(targetLog, defaultLevel)) {
            return joinPoint.proceed();
        }

        // Entry log — with sensitive data masking
        String argsStr = logArgs ? formatArgs(sig.getParameterNames(), joinPoint.getArgs()) : "[hidden]";
        doLog(targetLog, defaultLevel, "▶ [{}] {} ({})", layer, methodName, argsStr);

        long start = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            long elapsed = (System.nanoTime() - start) / 1_000_000;

            // Exit log — with tiered slow detection
            String resultStr = logResult ? summarise(result) : "[hidden]";
            if (elapsed > VERY_SLOW_THRESHOLD_MS) {
                targetLog.error("◀ [{}] {} → {} [{}ms] 🔴 VERY SLOW", layer, methodName, resultStr, elapsed);
            } else if (elapsed > SLOW_THRESHOLD_MS) {
                targetLog.warn("◀ [{}] {} → {} [{}ms] ⚠ SLOW", layer, methodName, resultStr, elapsed);
            } else {
                doLog(targetLog, defaultLevel, "◀ [{}] {} → {} [{}ms]", layer, methodName, resultStr, elapsed);
            }
            return result;
        } catch (Exception ex) {
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            targetLog.error("✖ [{}] {} threw {} [{}ms]: {}",
                    layer, methodName, ex.getClass().getSimpleName(), elapsed, ex.getMessage());
            throw ex;
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private Logged getAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        Logged methodAnnotation = method.getAnnotation(Logged.class);
        if (methodAnnotation != null) return methodAnnotation;
        return joinPoint.getTarget().getClass().getAnnotation(Logged.class);
    }

    private String formatArgs(String[] names, Object[] values) {
        if (values == null || values.length == 0) return "";
        StringJoiner sj = new StringJoiner(", ");
        for (int i = 0; i < values.length; i++) {
            String name = (names != null && i < names.length) ? names[i] : "arg" + i;
            // Mask sensitive parameters (password, token, etc.)
            if (SensitiveDataMasker.isSensitiveField(name)) {
                sj.add(name + "=***");
            } else {
                sj.add(name + "=" + summarise(values[i]));
            }
        }
        return sj.toString();
    }

    private String summarise(Object obj) {
        if (obj == null) return "null";
        String str = obj.toString();
        // Truncate large payloads
        if (str.length() > 200) {
            str = str.substring(0, 200) + "…(" + str.length() + " chars)";
        }
        // Mask any inline sensitive data (tokens, card numbers)
        return SensitiveDataMasker.maskInline(str);
    }

    private boolean isLevelEnabled(Logger logger, String level) {
        return switch (level.toUpperCase()) {
            case "TRACE" -> logger.isTraceEnabled();
            case "DEBUG" -> logger.isDebugEnabled();
            case "WARN"  -> logger.isWarnEnabled();
            case "ERROR" -> logger.isErrorEnabled();
            default      -> logger.isInfoEnabled();
        };
    }

    private void doLog(Logger logger, String level, String format, Object... args) {
        switch (level.toUpperCase()) {
            case "TRACE" -> logger.trace(format, args);
            case "DEBUG" -> logger.debug(format, args);
            case "WARN"  -> logger.warn(format, args);
            case "ERROR" -> logger.error(format, args);
            default      -> logger.info(format, args);
        }
    }
}

