package com.syn.usermanagement.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * MDC (Mapped Diagnostic Context) Filter
 *
 * Adds contextual information to every log message:
 * - requestId: Unique ID for each request (for tracing)
 * - userId: Logged-in user's email
 * - clientIp: Client's IP address
 * - requestUri: The API endpoint being called
 *
 * This allows you to trace all logs related to a single request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(MdcRequestFilter.class);

    private static final String REQUEST_ID = "requestId";
    private static final String USER_ID = "userId";
    private static final String CLIENT_IP = "clientIp";
    private static final String REQUEST_URI = "requestUri";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        try {
            // Generate unique request ID
            String requestId = UUID.randomUUID().toString().substring(0, 8);
            MDC.put(REQUEST_ID, requestId);

            // Get client IP
            String clientIp = getClientIp(request);
            MDC.put(CLIENT_IP, clientIp);

            // Get request URI
            String requestUri = request.getRequestURI();
            MDC.put(REQUEST_URI, requestUri);

            // Add request ID to response header (useful for debugging)
            response.setHeader("X-Request-ID", requestId);

            // Log request start
            logger.info("▶ REQUEST START: {} {} from IP: {}",
                    request.getMethod(), requestUri, clientIp);

            // Continue with the filter chain
            filterChain.doFilter(request, response);

            // After request processing, get user info if available
            setUserIdInMdc();

            // Log request end
            long duration = System.currentTimeMillis() - startTime;
            logger.info("◀ REQUEST END: {} {} - Status: {} - Duration: {}ms",
                    request.getMethod(), requestUri, response.getStatus(), duration);

        } finally {
            // Clean up MDC to prevent memory leaks
            MDC.clear();
        }
    }

    /**
     * Get client IP address (handles proxies)
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // If multiple IPs, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * Set user ID in MDC after authentication
     */
    private void setUserIdInMdc() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            MDC.put(USER_ID, auth.getName());
        } else {
            MDC.put(USER_ID, "anonymous");
        }
    }
}