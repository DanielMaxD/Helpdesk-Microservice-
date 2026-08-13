package com.helpdesk.ticketservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ticket-service never issues tokens - it only validates tokens issued by
 * user-service. These tests build a raw JWT the same way user-service's
 * JwtService does (same claim names, same HS256 signing) to prove the two
 * services are genuinely interoperable when configured with the same secret,
 * and that mismatched secrets are correctly rejected.
 */
class JwtServiceTest {

    private static final String SHARED_SECRET =
            "test-secret-key-that-is-long-enough-for-hs256-signing-1234567890";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SHARED_SECRET);
    }

    @Test
    void validatesTokenIssuedWithMatchingSecret() {
        UUID userId = UUID.randomUUID();
        String token = buildToken(SHARED_SECRET, userId, "AGENT", 3_600_000L);

        assertTrue(jwtService.isTokenValid(token));
        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals("AGENT", jwtService.extractRole(token));
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        String differentSecret = "a-completely-different-secret-key-value-should-fail-1234567890";
        String token = buildToken(differentSecret, UUID.randomUUID(), "ADMIN", 3_600_000L);

        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    void rejectsExpiredToken() {
        String token = buildToken(SHARED_SECRET, UUID.randomUUID(), "USER", -1000L);

        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    void rejectsGarbageToken() {
        assertFalse(jwtService.isTokenValid("not-a-real-token"));
    }

    private String buildToken(String secret, UUID userId, String role, long expirationOffsetMs) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationOffsetMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .claim("email", "demo@helpdesk.dev")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }
}
