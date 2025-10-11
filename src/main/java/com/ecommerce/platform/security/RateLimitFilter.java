package com.ecommerce.platform.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter to prevent brute force attacks and abuse.
 * <p>
 * Applies stricter limits to authentication endpoints and general limits to all other endpoints.
 * </p>
 */
@Component
@Order(1)
@Slf4j
@ConditionalOnProperty(
    name = "rate.limit.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();

    @Value("${rate.limit.auth.capacity:5}")
    private int authCapacity;

    @Value("${rate.limit.auth.refill:5}")
    private int authRefill;

    @Value("${rate.limit.general.capacity:100}")
    private int generalCapacity;

    @Value("${rate.limit.general.refill:100}")
    private int generalRefill;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        // Use stricter rate limiting for auth endpoints
        if (path.contains("/auth/")) {
            Bucket bucket = authBuckets.computeIfAbsent(clientIp, k -> createAuthBucket());
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for IP: {} on auth endpoint: {}", clientIp, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many authentication attempts. Please try again later.\"}");
                return;
            }
        } else {
            // General rate limiting for other endpoints
            Bucket bucket = generalBuckets.computeIfAbsent(clientIp, k -> createGeneralBucket());
            if (!bucket.tryConsume(1)) {
                log.warn("Rate limit exceeded for IP: {} on endpoint: {}", clientIp, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests. Please slow down.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Creates a rate limiter bucket for authentication endpoints.
     *
     * @return configured bucket with auth rate limits
     */
    private Bucket createAuthBucket() {
        Refill refill = Refill.greedy(authRefill, Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(authCapacity, refill);
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Creates a rate limiter bucket for general endpoints.
     *
     * @return configured bucket with general rate limits
     */
    private Bucket createGeneralBucket() {
        Refill refill = Refill.greedy(generalRefill, Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(generalCapacity, refill);
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Extracts the client IP address from the request.
     *
     * @param request the HTTP request
     * @return the client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // If multiple IPs, take the first one
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
}
