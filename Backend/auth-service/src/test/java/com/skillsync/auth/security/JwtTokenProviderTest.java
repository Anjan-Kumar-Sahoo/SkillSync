package com.skillsync.auth.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private static final String JWT_SECRET = "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=";

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(tokenProvider, "accessExpiration", 1_000L);
        ReflectionTestUtils.setField(tokenProvider, "refreshExpiration", 2_000L);
    }

    @Test
    void shouldGenerateAccessTokenWithExpectedClaims() {
        String token = tokenProvider.generateAccessToken(42L, "learner@skillsync.dev", "ROLE_LEARNER");

        Claims claims = tokenProvider.extractClaims(token);
        assertEquals("42", claims.getSubject());
        assertEquals("learner@skillsync.dev", claims.get("email", String.class));
        assertEquals("ROLE_LEARNER", claims.get("role", String.class));
        assertTrue(tokenProvider.isTokenValid(token));
        assertEquals(42L, tokenProvider.extractUserId(token));
        assertEquals("learner@skillsync.dev", tokenProvider.extractEmail(token));
    }

    @Test
    void shouldGenerateRefreshTokenWithUserSubject() {
        String refreshToken = tokenProvider.generateRefreshToken(9L);

        Claims claims = tokenProvider.extractClaims(refreshToken);
        assertEquals("9", claims.getSubject());
        assertNull(claims.get("email", String.class));
        assertTrue(tokenProvider.isTokenValid(refreshToken));
    }

    @Test
    void shouldReturnFalseForMalformedToken() {
        assertFalse(tokenProvider.isTokenValid("not.a.valid.jwt"));
    }

    @Test
    void shouldRespectMinimumExpirationThresholds() {
        assertEquals(24L * 60 * 60 * 1000, tokenProvider.getAccessExpiration());
        assertEquals(7L * 24 * 60 * 60 * 1000, tokenProvider.getRefreshExpiration());
    }

    @Test
    void shouldUseConfiguredExpirationWhenHigherThanMinimum() {
        ReflectionTestUtils.setField(tokenProvider, "accessExpiration", 48L * 60 * 60 * 1000);
        ReflectionTestUtils.setField(tokenProvider, "refreshExpiration", 14L * 24 * 60 * 60 * 1000);

        assertEquals(48L * 60 * 60 * 1000, tokenProvider.getAccessExpiration());
        assertEquals(14L * 24 * 60 * 60 * 1000, tokenProvider.getRefreshExpiration());
    }
}
