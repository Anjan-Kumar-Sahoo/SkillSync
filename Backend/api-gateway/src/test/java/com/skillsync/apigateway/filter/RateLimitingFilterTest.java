package com.skillsync.apigateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingFilterTest {

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
    }

    @Test
    void shouldBypassOptionsRequests() {
        AtomicInteger chainCalls = new AtomicInteger();
        GatewayFilterChain chain = exchange -> {
            chainCalls.incrementAndGet();
            return Mono.empty();
        };

        ServerWebExchange exchange = exchange(HttpMethod.OPTIONS, "/api/auth/login", "10.0.0.1", null);
        filter.filter(exchange, chain).block();

        assertEquals(1, chainCalls.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldApplyExpectedRateLimitForEachPathCategory() {
        assertLimitHeader("/api/auth/verify-otp", "1.1.1.1", null, "5");
        assertLimitHeader("/api/auth/login", "2.2.2.2", null, "10");
        assertLimitHeader("/api/payments/create-order", "3.3.3.3", "99", "10");
        assertLimitHeader("/actuator/health", "4.4.4.4", null, "60");
        assertLimitHeader("/api/users/profile", "5.5.5.5", "42", "100");
    }

    @Test
    void shouldReturnTooManyRequestsAfterLoginLimitIsExceeded() {
        AtomicInteger chainCalls = new AtomicInteger();
        GatewayFilterChain chain = exchange -> {
            chainCalls.incrementAndGet();
            return Mono.empty();
        };

        for (int i = 0; i < 10; i++) {
            ServerWebExchange allowed = exchange(HttpMethod.POST, "/api/auth/login", "20.20.20.20", null);
            filter.filter(allowed, chain).block();
            assertNull(allowed.getResponse().getStatusCode());
        }

        ServerWebExchange blocked = exchange(HttpMethod.POST, "/api/auth/login", "20.20.20.20", null);
        filter.filter(blocked, chain).block();

        assertEquals(10, chainCalls.get());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, blocked.getResponse().getStatusCode());
        assertEquals("0", blocked.getResponse().getHeaders().getFirst("X-RateLimit-Remaining"));
        assertEquals("60", blocked.getResponse().getHeaders().getFirst("Retry-After"));
    }

    @Test
    void shouldRateLimitByUserKeyForAuthenticatedPaymentRequests() {
        GatewayFilterChain chain = exchange -> Mono.empty();

        for (int i = 0; i < 10; i++) {
            ServerWebExchange allowed = exchange(HttpMethod.POST, "/api/payments/verify", "30.30.30.30", "7");
            filter.filter(allowed, chain).block();
            assertNull(allowed.getResponse().getStatusCode());
        }

        ServerWebExchange blocked = exchange(HttpMethod.POST, "/api/payments/verify", "30.30.30.30", "7");
        filter.filter(blocked, chain).block();

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, blocked.getResponse().getStatusCode());
        assertEquals("10", blocked.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCleanExpiredBuckets() throws Exception {
        Field bucketsField = RateLimitingFilter.class.getDeclaredField("buckets");
        bucketsField.setAccessible(true);
        Map<String, Object> buckets = (Map<String, Object>) bucketsField.get(filter);

        Class<?> bucketClass = Class.forName("com.skillsync.apigateway.filter.RateLimitingFilter$RateLimitBucket");
        Constructor<?> constructor = bucketClass.getDeclaredConstructor(long.class);
        constructor.setAccessible(true);

        Object expired = constructor.newInstance(System.currentTimeMillis() - 130_000);
        Object active = constructor.newInstance(System.currentTimeMillis());
        buckets.put("expired", expired);
        buckets.put("active", active);

        filter.cleanExpiredBuckets();

        assertFalse(buckets.containsKey("expired"));
        assertTrue(buckets.containsKey("active"));
    }

    @Test
    void shouldHaveExpectedOrder() {
        assertEquals(-2, filter.getOrder());
    }

    private void assertLimitHeader(String path, String ip, String userId, String expectedLimit) {
        GatewayFilterChain chain = exchange -> Mono.empty();
        ServerWebExchange exchange = exchange(HttpMethod.GET, path, ip, userId);

        filter.filter(exchange, chain).block();

        assertEquals(expectedLimit, exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
    }

    private ServerWebExchange exchange(HttpMethod method, String path, String forwardedIp, String userId) {
        MockServerHttpRequest.BaseBuilder<?> requestBuilder = MockServerHttpRequest.method(method, path)
                .header("X-Forwarded-For", forwardedIp);
        if (userId != null) {
            requestBuilder.header("X-User-Id", userId);
        }
        return MockServerWebExchange.from(requestBuilder.build());
    }
}
