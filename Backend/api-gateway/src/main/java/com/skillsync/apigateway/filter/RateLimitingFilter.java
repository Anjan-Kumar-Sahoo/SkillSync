package com.skillsync.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Global rate limiting filter for the API Gateway.
 * Uses in-memory sliding window counters (production: swap for Redis-based).
 *
 * Rate limits:
 *   - OTP endpoints:   5 req/min per IP
 *   - Login endpoints:  10 req/min per IP
 *   - Public APIs:     60 req/min per IP
 *   - Authenticated:   100 req/min per user
 */
@Component
@Slf4j
public class RateLimitingFilter implements GlobalFilter, Ordered {

    // Sliding window: key → (count, windowStartMs)
    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    private static final long WINDOW_MS = 60_000; // 1 minute

    // Rate limits per category
    private static final int OTP_LIMIT = 5;
    private static final int LOGIN_LIMIT = 10;
    private static final int PAYMENT_LIMIT = 10;
    private static final int PUBLIC_LIMIT = 60;
    private static final int AUTHENTICATED_LIMIT = 100;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String clientIp = extractClientIp(request);

        // Determine rate limit category
        int limit = determineLimit(path);

        // Build rate limit key: prefer user ID (authenticated), fallback to IP
        String userId = request.getHeaders().getFirst("X-User-Id");
        String rateLimitKey = (userId != null && !userId.isEmpty())
                ? "user:" + userId + ":" + categorize(path)
                : "ip:" + clientIp + ":" + categorize(path);

        // Check rate limit
        if (isRateLimited(rateLimitKey, limit)) {
            log.warn("[RATE_LIMIT] 429 Too Many Requests | key={} | path={} | limit={}/min",
                    rateLimitKey, path, limit);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
            exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
            exchange.getResponse().getHeaders().add("Retry-After", "60");
            return exchange.getResponse().setComplete();
        }

        // Add rate limit headers
        RateLimitBucket bucket = buckets.get(rateLimitKey);
        int remaining = bucket != null ? Math.max(0, limit - bucket.count.get()) : limit;
        exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(limit));
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(remaining));

        return chain.filter(exchange);
    }

    private int determineLimit(String path) {
        if (path.contains("/otp") || path.contains("/verify-otp") || path.contains("/forgot-password")) {
            return OTP_LIMIT;
        }
        if (path.contains("/login") || path.contains("/oauth-login") || path.contains("/setup-password")) {
            return LOGIN_LIMIT;
        }
        if (path.startsWith("/api/auth/")) {
            return LOGIN_LIMIT;
        }
        // Payment endpoints: stricter limit (10 req/min per user)
        if (path.startsWith("/api/payments/create-order") || path.startsWith("/api/payments/verify")) {
            return PAYMENT_LIMIT;
        }
        return path.contains("/actuator") ? PUBLIC_LIMIT : AUTHENTICATED_LIMIT;
    }

    private String categorize(String path) {
        if (path.contains("/otp") || path.contains("/verify-otp") || path.contains("/forgot-password")) {
            return "otp";
        }
        if (path.contains("/login") || path.contains("/oauth-login")) {
            return "auth";
        }
        if (path.startsWith("/api/payments/create-order") || path.startsWith("/api/payments/verify")) {
            return "payment";
        }
        return "general";
    }

    private boolean isRateLimited(String key, int limit) {
        long now = System.currentTimeMillis();
        buckets.compute(key, (k, bucket) -> {
            if (bucket == null || now - bucket.windowStart > WINDOW_MS) {
                return new RateLimitBucket(now);
            }
            return bucket;
        });

        RateLimitBucket bucket = buckets.get(key);
        return bucket.count.incrementAndGet() > limit;
    }

    private String extractClientIp(ServerHttpRequest request) {
        // Check X-Forwarded-For header (behind NGINX/proxy)
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        // Check X-Real-IP header
        String realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        // Fallback to remote address
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }

    @Override
    public int getOrder() {
        return -2; // Run before JWT filter
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 120_000)
    public void cleanExpiredBuckets() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(e -> now - e.getValue().windowStart > WINDOW_MS * 2);
    }

    private static class RateLimitBucket {
        final long windowStart;
        final AtomicInteger count = new AtomicInteger(0);

        RateLimitBucket(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
