package com.project.flightOps.service;

import com.project.flightOps.config.JwtProperties;
import com.project.flightOps.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties jwtProperties;

    // ── Token generation ─────────────────────────────────────────────────────

    public String generateAccessToken(UserDetails userDetails, Role role, String userId) {
        log.info("Generating Access Token for user: {} with role: {}", userDetails.getUsername(), role);

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role.name());
        claims.put("userId", userId);
        return buildToken(claims, userDetails.getUsername(), jwtProperties.getAccessTokenExpiryMs());
    }

    public String generateRefreshToken(UserDetails userDetails) {
        log.info("Generating Refresh Token for user: {}", userDetails.getUsername());
        return buildToken(new HashMap<>(), userDetails.getUsername(), jwtProperties.getRefreshTokenExpiryMs());
    }

    private String buildToken(Map<String, Object> extraClaims, String subject, long expiryMs) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Token validation ─────────────────────────────────────────────────────

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String email = extractEmail(token);
            boolean isUsernameMatch = email.equals(userDetails.getUsername());
            boolean isExpired = isTokenExpired(token);

            if (!isUsernameMatch) {
                log.warn("JWT validation failed: Token username '{}' does not match user details '{}'", email, userDetails.getUsername());
            }

            return isUsernameMatch && !isExpired;
        } catch (JwtException e) {
            log.error("JWT validation failed due to structural or cryptographic error: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            boolean expired = extractExpiration(token).before(new Date());
            if (expired) {
                log.warn("JWT validation failed: Token has expired.");
            }
            return expired;
        } catch (JwtException e) {
            log.error("Failed to check token expiration: {}", e.getMessage());
            return true;
        }
    }

    // ── Claims extraction ─────────────────────────────────────────────────────

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Role extractRole(String token) {
        try {
            String roleName = extractClaim(token, claims -> claims.get("role", String.class));
            return Role.valueOf(roleName);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Failed to parse Role from token claims: {}", e.getMessage());
            throw new JwtException("Invalid role in token", e);
        }
    }

    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.debug("Failed to parse JWT claims: {}", e.getMessage());
            throw e; // Rethrow to let calling methods handle validation context
        }
    }

    private SecretKey getSigningKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            log.error("Critical configuration error: JWT secret key is not properly Base64 encoded.");
            throw e;
        }
    }
}