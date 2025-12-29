package com.sinha.ecom_system.api_gateway.filter;

import com.sinha.ecom_system.api_gateway.service.ReactiveTokenBlacklistService;
import com.sinha.ecom_system.api_gateway.util.ReactiveJwtUtil;
import com.sinha.ecom_system.common.contants.CommonConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Global JWT Authentication Filter for API Gateway
 * Validates JWT tokens on every request in a reactive/non-blocking way
 */
@Component
@Slf4j
public class JwtAuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private final ReactiveJwtUtil jwtUtil;
    private final ReactiveTokenBlacklistService blacklistService;

    // Public endpoints that don't require JWT authentication
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
        "/v1/api/auth-service/auth/login",
        "/v1/api/auth-service/auth/register",
        "/v1/api/auth-service/auth/refresh",
        "/v1/api/auth-service/auth/forgot-password",
        "/v1/api/auth-service/auth/reset-password",
        "/v1/api/auth-service/auth/verify-email",
        "/actuator/health",
        "/actuator/info"
    );

    public JwtAuthenticationGlobalFilter(ReactiveJwtUtil jwtUtil, 
                                        ReactiveTokenBlacklistService blacklistService) {
        this.jwtUtil = jwtUtil;
        this.blacklistService = blacklistService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        log.debug("Processing request to: {}", path);

        // Skip JWT validation for public endpoints
        if (isPublicEndpoint(path)) {
            log.debug("Public endpoint accessed: {}", path);
            return chain.filter(exchange);
        }

        // Extract JWT token from Authorization header
        String token = extractToken(request);

        if (token == null) {
            log.warn("No JWT token found in request to: {}", path);
            return onError(exchange, "Missing authorization token", HttpStatus.UNAUTHORIZED);
        }

        // Validate JWT token
        if (!jwtUtil.validateToken(token)) {
            log.warn("Invalid JWT token for path: {}", path);
            return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }

        try {
            // Extract token metadata
            String tokenId = jwtUtil.getTokenId(token);
            String tokenType = jwtUtil.getTokenType(token);

            // Only validate ACCESS tokens (not REFRESH tokens)
            if (!"ACCESS".equals(tokenType)) {
                log.warn("Non-ACCESS token used for API access: {}", tokenType);
                return onError(exchange, "Invalid token type", HttpStatus.UNAUTHORIZED);
            }

            // Check if token is blacklisted (reactive call)
            return blacklistService.isTokenBlacklisted(tokenId)
                .flatMap(isBlacklisted -> {
                    if (Boolean.TRUE.equals(isBlacklisted)) {
                        log.warn("Blacklisted token attempted: {}", tokenId);
                        return onError(exchange, "Token has been revoked", HttpStatus.UNAUTHORIZED);
                    }

                    // Extract user information from JWT
                    UUID userId = jwtUtil.getUserIdFromToken(token);
                    String email = jwtUtil.getEmailFromToken(token);
                    List<String> roles = jwtUtil.getRolesFromToken(token);
                    List<String> permissions = jwtUtil.getPermissionsFromToken(token);

                    // Add user context to request headers for downstream services
                    ServerHttpRequest mutatedRequest = request.mutate()
                        .header(CommonConstants.HEADER_USER_ID, userId.toString())
                        .header(CommonConstants.HEADER_EMAIL, email)
                        .header(CommonConstants.HEADER_USER_ROLES, String.join(",", roles))
                        .header(CommonConstants.HEADER_USER_PERMISSIONS, String.join(",", permissions))
                        .build();

                    ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(mutatedRequest)
                        .build();

                    log.debug("JWT validated for user: {} ({})", email, userId);
                    return chain.filter(mutatedExchange);
                })
                .onErrorResume(e -> {
                    log.error("Error checking token blacklist: {}", e.getMessage());
                    return onError(exchange, "Authentication error", HttpStatus.INTERNAL_SERVER_ERROR);
                });

        } catch (Exception e) {
            log.error("Error processing JWT: {}", e.getMessage(), e);
            return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractToken(ServerHttpRequest request) {
        List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        
        if (authHeaders == null || authHeaders.isEmpty()) {
            return null;
        }

        String bearerToken = authHeaders.get(0);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    /**
     * Check if endpoint is public (no authentication required)
     */
    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    /**
     * Return error response
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        
        String errorJson = String.format(
            "{\"error\":\"%s\",\"message\":\"%s\",\"status\":%d,\"timestamp\":\"%s\"}",
            status.getReasonPhrase(),
            message,
            status.value(),
            java.time.Instant.now().toString()
        );

        return response.writeWith(
            Mono.just(response.bufferFactory().wrap(errorJson.getBytes()))
        );
    }

    /**
     * Set filter order (run early in the filter chain)
     */
    @Override
    public int getOrder() {
        return -100; // Run before other filters
    }
}

