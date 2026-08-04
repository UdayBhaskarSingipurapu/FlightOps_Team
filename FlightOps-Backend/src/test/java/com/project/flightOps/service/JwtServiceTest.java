package com.project.flightOps.service;

import com.project.flightOps.config.JwtProperties;
import com.project.flightOps.enums.Role;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    // Test-only signing key built from a fixed-length zero byte array (no real secret material involved).
    private static final SecretKey TEST_SIGNING_KEY = Keys.hmacShaKeyFor(new byte[32]);

    private static final long ACCESS_TOKEN_EXPIRY_MS = 1000 * 60 * 15; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRY_MS = 1000 * 60 * 60 * 24; // 24 hours

    private JwtProperties jwtProperties;
    private JwtService jwtService;

    private UserDetails userDetails;
    private final String userEmail = "pilot@flightops.com";
    private final String userId = "USR-100";

    @BeforeEach
    void setUp() {
        String encodedTestKey = Encoders.BASE64.encode(TEST_SIGNING_KEY.getEncoded());

        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(encodedTestKey);
        jwtProperties.setAccessTokenExpiryMs(ACCESS_TOKEN_EXPIRY_MS);
        jwtProperties.setRefreshTokenExpiryMs(REFRESH_TOKEN_EXPIRY_MS);

        jwtService = new JwtService(jwtProperties);

        userDetails = new User(userEmail, "password", Collections.emptyList());
    }

    // --- generateAccessToken Tests ---

    @Test
    void generateAccessToken_Success() {
        String token = jwtService.generateAccessToken(userDetails, Role.RampOfficer, userId);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(userEmail, jwtService.extractEmail(token));
        assertEquals(Role.RampOfficer, jwtService.extractRole(token));
        assertEquals(userId, jwtService.extractUserId(token));
    }

    // --- generateRefreshToken Tests ---

    @Test
    void generateRefreshToken_Success() {
        String token = jwtService.generateRefreshToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals(userEmail, jwtService.extractEmail(token));
        // Refresh tokens carry no role/userId claims
        assertNull(jwtService.extractUserId(token));
    }

    // --- extractEmail / extractRole / extractUserId Tests ---

    @Test
    void extractEmail_ReturnsSubject() {
        String token = jwtService.generateAccessToken(userDetails, Role.GroundSupervisor, userId);

        assertEquals(userEmail, jwtService.extractEmail(token));
    }

}
