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
        assertLimitHeader("/api/auth/otp", "1.1.1.0", null, "5");
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

        // Allow 10 requests
        for (int i = 1; i <= 10; i++) {
            ServerWebExchange exchange = exchange(HttpMethod.POST, "/api/auth/login", "127.0.0.1", null);
            filter.filter(exchange, chain).block();
            assertEquals(i, chainCalls.get());
            assertNull(exchange.getResponse().getStatusCode());
        }

        // 11th request should be blocked
        ServerWebExchange blockedExchange = exchange(HttpMethod.POST, "/api/auth/login", "127.0.0.1", null);
        filter.filter(blockedExchange, chain).block();
        assertEquals(10, chainCalls.get()); // Chain NOT called
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, blockedExchange.getResponse().getStatusCode());
    }

    @Test
    void shouldRateLimitDifferentUsersSeparately() {
        AtomicInteger chainCalls = new AtomicInteger();
        GatewayFilterChain chain = exchange -> {
            chainCalls.incrementAndGet();
            return Mono.empty();
        };

        // User 1 hits limit
        for (int i = 0; i < 10; i++) {
            filter.filter(exchange(HttpMethod.POST, "/api/auth/login", "1.1.1.1", null), chain).block();
        }

        // User 2 should still be allowed
        ServerWebExchange exchange2 = exchange(HttpMethod.POST, "/api/auth/login", "2.2.2.2", null);
        filter.filter(exchange2, chain).block();
        assertEquals(11, chainCalls.get());
        assertNull(exchange2.getResponse().getStatusCode());
    }

    private void assertLimitHeader(String path, String ip, String userId, String expectedLimit) {
        GatewayFilterChain chain = exchange -> Mono.empty();
        ServerWebExchange exchange = exchange(HttpMethod.GET, path, ip, userId);
        filter.filter(exchange, chain).block();
        assertEquals(expectedLimit, exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
    }

    private ServerWebExchange exchange(HttpMethod method, String path, String forwardedIp, String userId) {
        return exchange(method, path, forwardedIp, null, null, userId);
    }

    private ServerWebExchange exchange(HttpMethod method, String path, String forwardedIp, String realIp, java.net.InetSocketAddress remoteAddress, String userId) {
        MockServerHttpRequest.BaseBuilder<?> requestBuilder = MockServerHttpRequest.method(method, path);
        if (forwardedIp != null) {
            requestBuilder.header("X-Forwarded-For", forwardedIp);
        }
        if (realIp != null) {
            requestBuilder.header("X-Real-IP", realIp);
        }
        if (remoteAddress != null) {
            requestBuilder.remoteAddress(remoteAddress);
        }
        if (userId != null) {
            requestBuilder.header("X-User-Id", userId);
        }
        return MockServerWebExchange.from(requestBuilder.build());
    }

    @Test
    void shouldExtractRealIpWhenForwardedIsNull() {
        GatewayFilterChain chain = exchange -> Mono.empty();
        ServerWebExchange exchange = exchange(HttpMethod.GET, "/api/test", null, "8.8.8.8", null, null);
        filter.filter(exchange, chain).block();
        assertEquals("100", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
    }

    @Test
    void shouldHandleEmptyForwardedForHeader() {
        GatewayFilterChain chain = exchange -> Mono.empty();
        ServerWebExchange exchange = exchange(HttpMethod.GET, "/api/test", "", "8.8.8.8", null, null);
        filter.filter(exchange, chain).block();
        assertEquals("100", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
    }

    @Test
    void shouldHandleEmptyRealIpHeader() {
        GatewayFilterChain chain = exchange -> Mono.empty();
        ServerWebExchange exchange = exchange(HttpMethod.GET, "/api/test", null, "", new java.net.InetSocketAddress("9.9.9.9", 80), null);
        filter.filter(exchange, chain).block();
        assertEquals("100", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
    }

    @Test
    void shouldExtractRemoteAddressWhenHeadersAreNull() {
        GatewayFilterChain chain = exchange -> Mono.empty();
        ServerWebExchange exchange = exchange(HttpMethod.GET, "/api/test", null, null, new java.net.InetSocketAddress("9.9.9.9", 80), null);
        filter.filter(exchange, chain).block();
        assertEquals("100", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
    }

    @Test
    void shouldHandleNullRemoteAddress() {
        GatewayFilterChain chain = exchange -> Mono.empty();
        ServerWebExchange exchange = exchange(HttpMethod.GET, "/api/test", null, null, null, null);
        filter.filter(exchange, chain).block();
        assertEquals("100", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
    }

    @Test
    void shouldExtractFirstIpFromForwardedForList() {
        GatewayFilterChain chain = exchange -> Mono.empty();
        ServerWebExchange exchange = exchange(HttpMethod.GET, "/api/test", "1.1.1.1, 2.2.2.2", null, null, null);
        filter.filter(exchange, chain).block();
        assertEquals("100", exchange.getResponse().getHeaders().getFirst("X-RateLimit-Limit"));
    }

    @Test
    void shouldCategorizePathsProperly() {
        assertLimitHeader("/forgot-password", "10.1.1.1", null, "5");
        assertLimitHeader("/oauth-login", "10.2.2.2", null, "10");
        assertLimitHeader("/setup-password", "10.3.3.3", null, "10");
        assertLimitHeader("/api/auth/something", "10.4.4.4", null, "10");
        assertLimitHeader("/api/payments/create-order", "10.5.5.5", null, "10");
        assertLimitHeader("/api/payments/verify", "10.6.6.6", null, "10");
    }
}
