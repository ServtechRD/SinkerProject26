package com.sinker.app.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
                "test-secret-key-for-unit-tests-must-be-at-least-32-bytes!", 86400000L);
    }

    @Test
    void generateTokenReturnsNonNull() {
        String token = tokenProvider.generateToken(1L, "admin", "admin");
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void generateTokenContainsThreeParts() {
        String token = tokenProvider.generateToken(1L, "admin", "admin");
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have header.payload.signature");
    }

    @Test
    void extractUsernameFromToken() {
        String token = tokenProvider.generateToken(1L, "testuser", "sales");
        assertEquals("testuser", tokenProvider.getUsernameFromToken(token));
    }

    @Test
    void extractUserIdFromToken() {
        String token = tokenProvider.generateToken(42L, "testuser", "sales");
        assertEquals(42L, tokenProvider.getUserIdFromToken(token));
    }

    @Test
    void extractRoleCodeFromToken() {
        String token = tokenProvider.generateToken(1L, "testuser", "production_planner");
        assertEquals("production_planner", tokenProvider.getRoleCodeFromToken(token));
    }

    @Test
    void validateValidToken() {
        String token = tokenProvider.generateToken(1L, "admin", "admin");
        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    void validateExpiredToken() {
        JwtTokenProvider shortLived = new JwtTokenProvider(
                "test-secret-key-for-unit-tests-must-be-at-least-32-bytes!", -1000L);
        String token = shortLived.generateToken(1L, "admin", "admin");
        assertFalse(tokenProvider.validateToken(token));
    }

    @Test
    void validateTamperedToken() {
        String token = tokenProvider.generateToken(1L, "admin", "admin");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertFalse(tokenProvider.validateToken(tampered));
    }

    @Test
    void validateTokenWithDifferentSecret() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "a-completely-different-secret-key-at-least-32-bytes!!", 86400000L);
        String token = otherProvider.generateToken(1L, "admin", "admin");
        assertFalse(tokenProvider.validateToken(token));
    }

    @Test
    void validateNullToken() {
        assertFalse(tokenProvider.validateToken(null));
    }

    @Test
    void validateEmptyToken() {
        assertFalse(tokenProvider.validateToken(""));
    }

    @Test
    void validateMalformedToken() {
        assertFalse(tokenProvider.validateToken("not.a.jwt.token"));
    }
}
