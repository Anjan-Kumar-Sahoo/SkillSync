package com.skillsync.apigateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CsrfOriginFilterTest {

    private CsrfOriginFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CsrfOriginFilter();
        ReflectionTestUtils.setField(filter, "allowedOrigins", "https://app.skillsync.dev, https://skillsync.dev");
    }

    @Test
    void shouldBypassOptionsRequests() {
        AtomicInteger chainCalls = new AtomicInteger();
        GatewayFilterChain chain = exchange -> {
            chainCalls.incrementAndGet();
            return Mono.empty();
        };

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.OPTIONS, "/api/users")
                        .header("Origin", "https://evil.dev")
                        .build()
        );

        filter.filter(exchange, chain).block();

        assertEquals(1, chainCalls.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldAllowMutatingRequestFromAllowedOrigin() {
        AtomicInteger chainCalls = new AtomicInteger();
        GatewayFilterChain chain = exchange -> {
            chainCalls.incrementAndGet();
            return Mono.empty();
        };

        // Test with spaces and redundant commas in config
        ReflectionTestUtils.setField(filter, "allowedOrigins", "https://app.skillsync.dev, , https://skillsync.dev");

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/users")
                        .header("Origin", " https://app.skillsync.dev ")
                        .build()
        );

        filter.filter(exchange, chain).block();

        assertEquals(1, chainCalls.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldBlockMutatingRequestFromDisallowedOrigin() {
        AtomicInteger chainCalls = new AtomicInteger();
        GatewayFilterChain chain = exchange -> {
            chainCalls.incrementAndGet();
            return Mono.empty();
        };

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.patch("/api/users")
                        .header("Origin", "https://evil.dev")
                        .build()
        );

        filter.filter(exchange, chain).block();

        assertEquals(0, chainCalls.get());
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldSkipOriginValidationForSafeMethod() {
        AtomicInteger chainCalls = new AtomicInteger();
        GatewayFilterChain chain = exchange -> {
            chainCalls.incrementAndGet();
            return Mono.empty();
        };

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users")
                        .header("Origin", "https://evil.dev")
                        .build()
        );

        filter.filter(exchange, chain).block();

        assertEquals(1, chainCalls.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldAllowMutatingRequestWithoutOriginHeader() {
        AtomicInteger chainCalls = new AtomicInteger();
        GatewayFilterChain chain = exchange -> {
            chainCalls.incrementAndGet();
            return Mono.empty();
        };

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/users")
                        .build() // No Origin header
        );

        filter.filter(exchange, chain).block();

        assertEquals(1, chainCalls.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldHaveExpectedOrder() {
        assertEquals(-3, filter.getOrder());
    }
}
