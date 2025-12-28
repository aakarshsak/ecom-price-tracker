package com.sinha.ecom_system.user_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Gateway Authentication Filter
 * Ensures requests come only from API Gateway
 * Validates the X-Gateway-Secret header
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    @Value("${api-gateway.secret-key}")
    private String expectedSecret;

    // Endpoints accessible directly (health checks, etc.)
    private static final List<String> BYPASS_PATHS = List.of(
            "/actuator/health",
            "/actuator/info",
            "/error"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Allow bypass for certain endpoints
        if (shouldBypass(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validate gateway secret header
        String gatewaySecret = request.getHeader("X-Gateway-Secret");

        if (gatewaySecret == null || !gatewaySecret.equals(expectedSecret)) {
            log.warn("Unauthorized direct access attempt to: {} from IP: {}",
                    path, request.getRemoteAddr());

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Forbidden\"," +
                            "\"message\":\"Direct access not allowed. Use API Gateway.\"," +
                            "\"status\":403}"
            );
            return;
        }

        // Valid gateway request - proceed
        log.debug("Valid gateway request to: {}", path);
        filterChain.doFilter(request, response);
    }

    private boolean shouldBypass(String path) {
        return BYPASS_PATHS.stream().anyMatch(path::startsWith);
    }
}