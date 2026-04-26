package com.skillsync.apigateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityHeadersFilterTest {

    private final SecurityHeadersFilter filter = new SecurityHeadersFilter();

    @Test
    void shouldAddSecurityHeadersToResponse() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/me").build());
        GatewayFilterChain chain = ex -> Mono.empty();

        filter.filter(exchange, chain).block();

        assertEquals("DENY", exchange.getResponse().getHeaders().getFirst("X-Frame-Options"));
        assertEquals("nosniff", exchange.getResponse().getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals("1; mode=block", exchange.getResponse().getHeaders().getFirst("X-XSS-Protection"));
        assertEquals("max-age=31536000; includeSubDomains",
                exchange.getResponse().getHeaders().getFirst("Strict-Transport-Security"));
        assertEquals("strict-origin-when-cross-origin",
                exchange.getResponse().getHeaders().getFirst("Referrer-Policy"));
    }

    @Test
    void shouldHaveExpectedOrder() {
        assertEquals(-1, filter.getOrder());
    }
}
