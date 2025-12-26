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

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract JWT from Authorization header
            String token = extractTokenFromRequest(request);

            if (token != null && jwtUtil.validateToken(token)) {
                // Check token type first
                String tokenType = jwtUtil.getTokenType(token);
                if ("ACCESS".equals(tokenType)) {
                    // Check if token is blacklisted
                    String tokenId = jwtUtil.getTokenId(token);
                    if (tokenBlacklistService.isTokenBlacklisted(tokenId)) {
                        log.warn("Attempted to use blacklisted token: {}", tokenId);
                        // Don't authenticate - let Spring Security handle as unauthorized
                        filterChain.doFilter(request, response);
                        return;
                    }
                    
                    // Extract user details from token
                    String userId = jwtUtil.getUserIdFromToken(token).toString();
                    String email = jwtUtil.getEmailFromToken(token);
                    List<String> roles = jwtUtil.getRolesFromToken(token);
                    List<String> permissions = jwtUtil.getPermissionsFromToken(token);

                    // Create authorities from roles and permissions
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    // Add permissions as authorities
                    authorities.addAll(permissions.stream()
                            .map(perm -> new SimpleGrantedAuthority("PERM_" + perm))
                            .collect(Collectors.toList()));

                    // Create authentication token
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userId,  // Principal (user ID)
                                    null,    // Credentials (not needed after authentication)
                                    authorities
                            );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set authentication in SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("Set authentication for user: {} with roles: {} and permissions: {}",
                            email, roles, permissions);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
            // Don't throw exception, just continue without authentication
            // Spring Security will handle unauthorized access
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer " prefix
        }

        return null;
    }

    /**
     * Skip filter for public endpoints
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip JWT filter for public authentication endpoints
        return path.startsWith("/auth/register") ||
               path.startsWith("/auth/login") ||
               path.startsWith("/auth/refresh") ||
               path.startsWith("/auth/forgot-password") ||
               path.startsWith("/auth/reset-password") ||
               path.startsWith("/actuator/health") ||
               path.startsWith("/error");
    }
}

