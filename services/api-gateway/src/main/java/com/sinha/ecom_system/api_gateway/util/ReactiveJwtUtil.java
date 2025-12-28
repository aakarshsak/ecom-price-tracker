package com.sinha.ecom_system.api_gateway.util;

import com.sinha.ecom_system.api_gateway.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Reactive JWT Utility for API Gateway
 * Handles JWT validation and claims extraction
 */
@Component
@Slf4j
public class ReactiveJwtUtil {

    private final SecretKey secretKey;

    public ReactiveJwtUtil(JwtConfig jwtConfig) {
        this.secretKey = Keys.hmacShaKeyFor(
            jwtConfig.getSecretKey().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Parse and validate JWT token
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validate token (signature + expiration)
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT validation error: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extract user ID from token
     */
    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }

    /**
     * Extract email from token
     */
    public String getEmailFromToken(String token) {
        return parseToken(token).get("email", String.class);
    }

    /**
     * Extract roles from token
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return parseToken(token).get("roles", List.class);
    }

    /**
     * Extract permissions from token
     */
    @SuppressWarnings("unchecked")
    public List<String> getPermissionsFromToken(String token) {
        return parseToken(token).get("permissions", List.class);
    }

    /**
     * Get token ID (jti claim)
     */
    public String getTokenId(String token) {
        return parseToken(token).getId();
    }

    /**
     * Get token type
     */
    public String getTokenType(String token) {
        return parseToken(token).get("type", String.class);
    }

    /**
     * Get expiration date
     */
    public Date getExpirationDate(String token) {
        return parseToken(token).getExpiration();
    }
}

