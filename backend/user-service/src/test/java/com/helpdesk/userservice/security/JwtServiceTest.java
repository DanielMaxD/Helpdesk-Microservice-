package com.helpdesk.userservice.security;

import com.helpdesk.userservice.entity.Role;
import com.helpdesk.userservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "test-secret-key-that-is-long-enough-for-hs256-signing-1234567890",
                3_600_000L
        );

        user = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@helpdesk.dev")
                .passwordHash("hashed")
                .role(Role.AGENT)
                .active(true)
                .build();
    }

    @Test
    void generatedTokenContainsUserIdAndRole() {
        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token));
        assertEquals(user.getId(), jwtService.extractUserId(token));
        assertEquals("AGENT", jwtService.extractRole(token));
        assertEquals("test@helpdesk.dev", jwtService.extractEmail(token));
    }

    @Test
    void invalidTokenIsRejected() {
        assertTrue(!jwtService.isTokenValid("not-a-real-token"));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService shortLivedService = new JwtService(
                "test-secret-key-that-is-long-enough-for-hs256-signing-1234567890",
                -1000L
        );
        String token = shortLivedService.generateToken(user);

        assertTrue(!shortLivedService.isTokenValid(token));
    }
}
