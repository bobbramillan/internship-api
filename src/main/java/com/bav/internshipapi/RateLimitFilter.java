package com.bav.internshipapi;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple per-IP token-bucket rate limiter for /api/** endpoints.
 * Not distributed — fine for a single Railway replica. If this app ever
 * scales to multiple instances, swap the in-memory map for a shared store
 * (e.g. Redis) or Railway's built-in rate limiting.
 */
@Component
@Order(1)
public class RateLimitFilter implements Filter {

    private static final int REQUESTS_PER_MINUTE = 60;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(
                REQUESTS_PER_MINUTE,
                Refill.greedy(REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        // Only rate-limit the public API — leave the docs root page open
        if (!request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(req, res);
            return;
        }

        String clientIp = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(clientIp, ip -> newBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.getWriter().write(
                    "{\"error\":\"Rate limit exceeded. Max " + REQUESTS_PER_MINUTE +
                            " requests per minute. Try again shortly.\"}"
            );
        }
    }

    // Railway sits behind a proxy, so the real client IP is in
    // X-Forwarded-For, not getRemoteAddr() (which would be Railway's proxy IP)
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}