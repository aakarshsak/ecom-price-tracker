package com.sinha.ecom_system.auth_service.util;

import com.sinha.ecom_system.auth_service.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Utility Class
 * 
 * Handles all JWT operations:
 * - Token generation (ACCESS and REFRESH)
 * - Token parsing and validation
 * - Claims extraction
 * 
 * Uses HMAC-SHA256 algorithm for signing
 */
@Component
@Slf4j
public class JwtUtil {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    @Autowired
    public JwtUtil(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // Generate secure key from secret string
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate ACCESS token with user details, roles, and permissions
     * Used for API authentication
     * 
     * @param userId User's unique identifier
     * @param email User's email
     * @param roles List of role names (e.g., "ROLE_USER", "ROLE_ADMIN")
     * @param permissions List of permission names
     * @return Signed JWT token string
     */
    public String generateAccessToken(UUID userId, String email, List<String> roles, List<String> permissions) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiry());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("type", "ACCESS")
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiryDate)
                .id(UUID.randomUUID().toString()) // jti - used for blacklisting
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Generate REFRESH token
     * Used only to obtain new access tokens
     * Contains minimal claims for security
     * 
     * @param userId User's unique identifier
     * @return Signed JWT token string
     */
    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiry());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "REFRESH")
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiration(expiryDate)
                .id(UUID.randomUUID().toString())
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Parse and validate JWT token
     * Verifies signature and expiration
     * 
     * @param token JWT token string
     * @return Claims object containing token data
     * @throws JwtException if token is invalid
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Extract user ID from token subject claim
     */
    public UUID getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extract email from custom claim
     */
    public String getEmailFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("email", String.class);
    }

    /**
     * Extract roles list from custom claim
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("roles", List.class);
    }

    /**
     * Extract permissions list from custom claim
     */
    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("permissions", List.class);
    }

    /**
     * Get JWT ID (jti claim)
     * Used for token blacklisting on logout
     */
    public String getTokenId(String token) {
        Claims claims = parseToken(token);
        return claims.getId();
    }

    /**
     * Get token type from custom claim
     * Returns "ACCESS" or "REFRESH"
     */
    public String getTokenType(String token) {
        Claims claims = parseToken(token);
        return claims.get("type", String.class);
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Validate token signature and expiry
     * Returns false for any invalid token (malformed, expired, etc.)
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extract expiration date from token
     */
    public Date getExpirationDate(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration();
    }

    /**
     * Extract issued-at date from token
     */
    public Date getIssuedAtDate(String token) {
        Claims claims = parseToken(token);
        return claims.getIssuedAt();
    }

    /**
     * Get all claims as a Map
     */
    public Map<String, Object> getAllClaims(String token) {
        return parseToken(token);
    }
}

