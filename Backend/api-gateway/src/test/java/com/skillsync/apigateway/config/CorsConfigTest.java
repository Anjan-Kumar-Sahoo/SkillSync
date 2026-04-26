package com.skillsync.apigateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {

    @Test
    void shouldCreateCorsWebFilterWithExpectedConfiguration() {
        CorsConfig corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins", "https://app.skillsync.dev, https://skillsync.dev");

        CorsWebFilter filter = corsConfig.corsWebFilter();
        assertNotNull(filter);

        Object configSourceField = ReflectionTestUtils.getField(filter, "configSource");
        assertNotNull(configSourceField);
        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) configSourceField;

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("/api/users")
                        .header("Origin", "https://app.skillsync.dev")
                        .header("Access-Control-Request-Method", "POST")
                        .build()
        );

        CorsConfiguration configuration = source.getCorsConfiguration(exchange);
        assertNotNull(configuration);
        assertTrue(configuration.getAllowedOriginPatterns().contains("https://app.skillsync.dev"));
        assertTrue(configuration.getAllowedOriginPatterns().contains("https://skillsync.dev"));
        assertEquals(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"), configuration.getAllowedMethods());
        assertEquals(List.of("*"), configuration.getAllowedHeaders());
        assertEquals(List.of("X-RateLimit-Limit", "X-RateLimit-Remaining", "Retry-After"),
                configuration.getExposedHeaders());
        assertTrue(Boolean.TRUE.equals(configuration.getAllowCredentials()));
        assertEquals(3600L, configuration.getMaxAge());
    }
}
