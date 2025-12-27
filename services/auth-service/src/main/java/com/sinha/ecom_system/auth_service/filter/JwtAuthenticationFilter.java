package com.sinha.ecom_system.auth_service.filter;

import com.sinha.ecom_system.auth_service.service.TokenBlacklistService;
import com.sinha.ecom_system.auth_service.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Authentication Filter
 * Intercepts requests, validates JWT tokens, and sets Spring Security context
 * Runs once per request before Spring Security's authentication filter
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil, TokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /**
     * Main filter logic - validates token and authenticates user
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract JWT from Authorization header
            String token = extractTokenFromRequest(request);

            if (token != null && jwtUtil.validateToken(token)) {
                // Only process ACCESS tokens (not REFRESH tokens)
                String tokenType = jwtUtil.getTokenType(token);
                if ("ACCESS".equals(tokenType)) {
                    // Check if token has been blacklisted (logout)
                    String tokenId = jwtUtil.getTokenId(token);
                    if (tokenBlacklistService.isTokenBlacklisted(tokenId)) {
                        log.warn("Attempted to use blacklisted token: {}", tokenId);
                        filterChain.doFilter(request, response);
                        return;
                    }
                    
                    // Extract claims from JWT
                    String userId = jwtUtil.getUserIdFromToken(token).toString();
                    String email = jwtUtil.getEmailFromToken(token);
                    List<String> roles = jwtUtil.getRolesFromToken(token);
                    List<String> permissions = jwtUtil.getPermissionsFromToken(token);

                    // Convert roles to Spring Security authorities
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    // Add permissions as authorities with PERM_ prefix
                    authorities.addAll(permissions.stream()
                            .map(perm -> new SimpleGrantedAuthority("PERM_" + perm))
                            .collect(Collectors.toList()));

                    // Create authentication object for Spring Security
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId,      // Principal (accessible via SecurityContext)
                                    null,        // Credentials not needed
                                    authorities  // Roles and permissions
                            );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Store authentication in SecurityContext for this request
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("Set authentication for user: {} with roles: {} and permissions: {}",
                            email, roles, permissions);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
            // Continue without authentication - Spring Security will deny access
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     * Expected format: "Bearer <token>"
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    /**
     * Skip JWT validation for public endpoints
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Public endpoints don't require authentication
        return path.startsWith("/auth/register") ||
               path.startsWith("/auth/login") ||
               path.startsWith("/auth/refresh") ||
               path.startsWith("/auth/forgot-password") ||
               path.startsWith("/auth/reset-password") ||
               path.startsWith("/actuator/health") ||
               path.startsWith("/error");
    }
}

