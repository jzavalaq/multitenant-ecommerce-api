package com.ecommerce.platform.filter;

import com.ecommerce.platform.util.AppConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter for logging HTTP requests and responses.
 * <p>
 * Adds a correlation ID to each request for tracing purposes.
 * </p>
 */
@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = UUID.randomUUID().toString().substring(0, AppConstants.CORRELATION_ID_LENGTH);
        MDC.put(CORRELATION_ID, correlationId);

        long startTime = System.currentTimeMillis();

        try {
            log.info("REQUEST: {} {} correlationId={}", request.getMethod(), request.getRequestURI(), correlationId);
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("RESPONSE: {} {}ms correlationId={}", response.getStatus(), duration, correlationId);
            MDC.remove(CORRELATION_ID);
        }
    }
}
