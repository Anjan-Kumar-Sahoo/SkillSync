package com.skillsync.apigateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class JwtAuthenticationFilterTest {

    private static final String JWT_SECRET = "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=";

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "jwtSecret", JWT_SECRET);
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenIsMissing() {
        ServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/me").build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain).block();

        assertFalse(chainCalled.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldUseBearerTokenAndForwardJwtClaimsAsHeaders() {
        String token = createAccessToken("42", "learner@skillsync.dev", "ROLE_LEARNER");
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", "spoofed")
                        .header("X-User-Email", "spoofed@example.com")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .build()
        );

        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            forwarded.set(ex);
            return Mono.empty();
        };

        filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain).block();

        assertNotNull(forwarded.get());
        assertEquals("42", forwarded.get().getRequest().getHeaders().getFirst("X-User-Id"));
        assertEquals("learner@skillsync.dev", forwarded.get().getRequest().getHeaders().getFirst("X-User-Email"));
        assertEquals("ROLE_LEARNER", forwarded.get().getRequest().getHeaders().getFirst("X-User-Role"));
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldUseAccessTokenCookieWhenAuthorizationHeaderIsMissing() {
        String token = createAccessToken("7", "mentor@skillsync.dev", "ROLE_MENTOR");
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/mentors/profile")
                        .cookie(new HttpCookie("accessToken", token))
                        .build()
        );

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain).block();

        assertTrue(chainCalled.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldReturnUnauthorizedWhenTokenIsInvalid() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.value")
                        .build()
        );
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain).block();

        assertFalse(chainCalled.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldFallbackToCookieWhenAuthorizationHeaderIsNotBearer() {
        String token = createAccessToken("7", "mentor@skillsync.dev", "ROLE_MENTOR");
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/mentors/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                        .cookie(new HttpCookie("accessToken", token))
                        .build()
        );

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain).block();

        assertTrue(chainCalled.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldReturnUnauthorizedWhenAuthorizationHeaderIsNotBearerAndNoCookie() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                        .build()
        );
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, chain).block();

        assertFalse(chainCalled.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    private String createAccessToken(String userId, String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET));
        return Jwts.builder()
                .subject(userId)
                .claims(Map.of("email", email, "role", role))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
    }
}
