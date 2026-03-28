package com.homecare.user.security;

import com.homecare.user.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final UUID testUserId = UUID.randomUUID();
    private final String testEmail = "test@homecare.in";
    private final Role testRole = Role.CUSTOMER;

    @BeforeEach
    void setUp() {
        // 1440 min access, 30 day refresh
        jwtUtil = new JwtUtil("homecare-test-secret-min-32-characters-long!!", 1440, 30);
    }

    @Test
    @DisplayName("Generate access token and extract userId")
    void generateAccessToken_extractUserId() {
        String token = jwtUtil.generateAccessToken(testUserId, testEmail, testRole);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(testUserId, jwtUtil.extractUserId(token));
    }

    @Test
    @DisplayName("Generate access token and extract role")
    void generateAccessToken_extractRole() {
        String token = jwtUtil.generateAccessToken(testUserId, testEmail, testRole);

        assertEquals(testRole.name(), jwtUtil.extractRole(token));
    }

    @Test
    @DisplayName("Token is valid immediately after generation")
    void generatedToken_isValid() {
        String token = jwtUtil.generateAccessToken(testUserId, testEmail, testRole);

        assertTrue(jwtUtil.isValid(token));
    }

    @Test
    @DisplayName("Tampered token is invalid")
    void tamperedToken_isInvalid() {
        String token = jwtUtil.generateAccessToken(testUserId, testEmail, testRole);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertFalse(jwtUtil.isValid(tampered));
    }

    @Test
    @DisplayName("Expired token is detected as invalid")
    void expiredToken_isInvalid() {
        // Create a JwtUtil with 0-minute expiry
        JwtUtil shortLived = new JwtUtil("homecare-test-secret-min-32-characters-long!!", 0, 0);
        String token = shortLived.generateAccessToken(testUserId, testEmail, testRole);

        // Token with 0 expiry is already expired
        assertFalse(shortLived.isValid(token));
    }

    @Test
    @DisplayName("Generate refresh token and validate")
    void generateRefreshToken_isValid() {
        String refreshToken = jwtUtil.generateRefreshToken(testUserId);

        assertNotNull(refreshToken);
        assertTrue(jwtUtil.isValid(refreshToken));
        assertEquals(testUserId, jwtUtil.extractUserId(refreshToken));
    }

    @Test
    @DisplayName("Different refresh tokens for same user are unique (random JTI)")
    void differentRefreshTokens_areUnique() {
        String token1 = jwtUtil.generateRefreshToken(testUserId);
        String token2 = jwtUtil.generateRefreshToken(testUserId);

        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("Garbage string is invalid token")
    void garbageString_isInvalid() {
        assertFalse(jwtUtil.isValid("not.a.jwt"));
        assertFalse(jwtUtil.isValid(""));
        assertFalse(jwtUtil.isValid("eyJhbGciOiJIUzI1NiJ9.garbage.garbage"));
    }

    @Test
    @DisplayName("Token signed with different secret is invalid")
    void differentSecret_isInvalid() {
        JwtUtil otherJwt = new JwtUtil("completely-different-secret-32-chars-minimum!!", 1440, 30);
        String token = otherJwt.generateAccessToken(testUserId, testEmail, testRole);

        assertFalse(jwtUtil.isValid(token));
    }

    @Test
    @DisplayName("getExpiration returns future instant for valid token")
    void getExpiration_returnsFutureInstant() {
        String token = jwtUtil.generateAccessToken(testUserId, testEmail, testRole);

        assertNotNull(jwtUtil.getExpiration(token));
        assertTrue(jwtUtil.getExpiration(token).isAfter(java.time.Instant.now()));
    }

    @Test
    @DisplayName("Access token and refresh token have different expiry")
    void accessAndRefreshTokens_haveDifferentExpiry() {
        String accessToken = jwtUtil.generateAccessToken(testUserId, testEmail, testRole);
        String refreshToken = jwtUtil.generateRefreshToken(testUserId);

        assertTrue(jwtUtil.getExpiration(refreshToken).isAfter(jwtUtil.getExpiration(accessToken)));
    }
}

