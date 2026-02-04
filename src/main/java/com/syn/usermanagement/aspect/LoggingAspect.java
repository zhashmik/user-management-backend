package com.syn.usermanagement.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Logging Aspect - Automatically logs method entry, exit, and exceptions
 *
 * Uses AOP (Aspect Oriented Programming) to add logging without modifying existing code.
 */
@Aspect
@Component
public class LoggingAspect {

    /**
     * Pointcut for all controller methods
     */
    @Pointcut("within(com.syn.usermanagement.controller..*)")
    public void controllerMethods() {}

    /**
     * Pointcut for all service methods
     */
    @Pointcut("within(com.syn.usermanagement.service..*)")
    public void serviceMethods() {}

    /**
     * Pointcut for all repository methods
     */
    @Pointcut("within(com.syn.usermanagement.repository..*)")
    public void repositoryMethods() {}

    /**
     * Log around controller methods (entry + exit + duration)
     */
    @Around("controllerMethods()")
    public Object logAroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        // Log method entry
        logger.info("➡️  CONTROLLER [{}] Method: {} - Args: {}",
                className, methodName, formatArgs(joinPoint.getArgs()));

        long startTime = System.currentTimeMillis();

        try {
            // Execute the method
            Object result = joinPoint.proceed();

            // Log method exit with duration
            long duration = System.currentTimeMillis() - startTime;
            logger.info("⬅️  CONTROLLER [{}] Method: {} - Duration: {}ms - Success",
                    className, methodName, duration);

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("❌ CONTROLLER [{}] Method: {} - Duration: {}ms - Exception: {}",
                    className, methodName, duration, e.getMessage());
            throw e;
        }
    }

    /**
     * Log around service methods
     */
    @Around("serviceMethods()")
    public Object logAroundService(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        // Log method entry
        logger.debug("  ➡️  SERVICE [{}] Method: {} - Args: {}",
                className, methodName, formatArgs(joinPoint.getArgs()));

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("  ⬅️  SERVICE [{}] Method: {} - Duration: {}ms",
                    className, methodName, duration);

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("  ❌ SERVICE [{}] Method: {} - Duration: {}ms - Exception: {}",
                    className, methodName, duration, e.getMessage());
            throw e;
        }
    }

    /**
     * Log exceptions from any layer
     */
    @AfterThrowing(pointcut = "controllerMethods() || serviceMethods()", throwing = "exception")
    public void logException(JoinPoint joinPoint, Throwable exception) {
        Logger logger = LoggerFactory.getLogger(joinPoint.getTarget().getClass());

        logger.error("🔥 EXCEPTION in {}.{}() - Message: {}",
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName(),
                exception.getMessage(),
                exception);
    }

    /**
     * Format method arguments for logging (hide sensitive data)
     */
    private String formatArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        return Arrays.stream(args)
                .map(arg -> {
                    if (arg == null) {
                        return "null";
                    }
                    String argString = arg.toString();
                    // Hide passwords
                    if (argString.toLowerCase().contains("password")) {
                        return "[HIDDEN]";
                    }
                    // Truncate long strings
                    if (argString.length() > 100) {
                        return argString.substring(0, 100) + "...";
                    }
                    return argString;
                })
                .toList()
                .toString();
    }
}