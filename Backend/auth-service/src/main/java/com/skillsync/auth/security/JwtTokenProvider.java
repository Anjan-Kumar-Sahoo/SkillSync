package com.skillsync.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenProvider {

    private static final long MIN_ACCESS_EXPIRATION_MS = 24L * 60 * 60 * 1000;
    private static final long MIN_REFRESH_EXPIRATION_MS = 7L * 24 * 60 * 60 * 1000;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public String generateAccessToken(Long userId, String email, String role) {
        long effectiveAccessExpiration = getAccessExpiration();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(Map.of("email", email, "role", role))
                .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + effectiveAccessExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        long effectiveRefreshExpiration = getRefreshExpiration();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + effectiveRefreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public Long extractUserId(String token) {
        return Long.parseLong(extractClaims(token).getSubject());
    }

    public String extractEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }

    public long getAccessExpiration() {
        return Math.max(accessExpiration, MIN_ACCESS_EXPIRATION_MS);
    }

    public long getRefreshExpiration() {
        return Math.max(refreshExpiration, MIN_REFRESH_EXPIRATION_MS);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
