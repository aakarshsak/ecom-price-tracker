package com.sinha.ecom_system.auth_service.controller;


import com.sinha.ecom_system.auth_service.dto.request.LoginRequest;
import com.sinha.ecom_system.auth_service.dto.request.RefreshTokenRequest;
import com.sinha.ecom_system.auth_service.dto.request.RegisterRequest;
import com.sinha.ecom_system.auth_service.dto.response.ApiResponse;
import com.sinha.ecom_system.auth_service.dto.response.AuthResponse;
import com.sinha.ecom_system.auth_service.dto.response.MessageResponse;
import com.sinha.ecom_system.auth_service.dto.response.TokenResponse;
import com.sinha.ecom_system.auth_service.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST Controller for Authentication endpoints
 * Handles user registration, login, token refresh, and logout operations
 */
@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new user with default ROLE_USER
     * @param request Contains email and password
     * @return AuthResponse with access token, refresh token, and user info
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> registerUser(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.registerAuth(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<AuthResponse>builder()
                .status("success")
                .message("Registration successful")
                .data(authResponse)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * Authenticate user with email and password
     * Returns tokens unless 2FA is required
     * @param request Contains email and password
     * @return AuthResponse with tokens or 2FA requirement flag
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse authResponse = authService.login(request);
            
            // Check if 2FA verification is needed
            String message = authResponse.getRequires2FA() != null && authResponse.getRequires2FA()
                    ? "2FA verification required"
                    : "Login successful";
            
            return ResponseEntity.ok(ApiResponse.<AuthResponse>builder()
                    .status("success")
                    .message(message)
                    .data(authResponse)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            System.out.println("Login error: {}" +  e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.<AuthResponse>builder()
                    .status("error")
                    .message(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    /**
     * Generate new access token using valid refresh token
     * @param request Contains refresh token
     * @return New access token with same refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            TokenResponse tokenResponse = authService.refreshAccessToken(request);
            return ResponseEntity.ok(ApiResponse.<TokenResponse>builder()
                    .status("success")
                    .message("Token refreshed successfully")
                    .data(tokenResponse)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            System.out.println("Refresh error: {}" +  e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.<TokenResponse>builder()
                    .status("error")
                    .message(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    /**
     * Logout user by blacklisting their access token
     * Token is added to Redis blacklist and cannot be reused
     * Also revokes all refresh tokens for this user
     * 
     * @param request HTTP request containing Authorization header with Bearer token
     * @return Success message
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletRequest request) {
        try {
            // Extract access token from Authorization header
            String authHeader = request.getHeader("Authorization");
            String accessToken = null;
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                accessToken = authHeader.substring(7); // Remove "Bearer " prefix
            }
            
            if (accessToken == null) {
                throw new RuntimeException("Access token not provided");
            }
            
            // Get userId from SecurityContext (populated by JwtAuthenticationFilter)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UUID userId = UUID.fromString(authentication.getPrincipal().toString());
            
            // Blacklist access token and revoke refresh tokens
            authService.logout(userId, accessToken);
            
            return ResponseEntity.ok(MessageResponse.builder()
                            .message("You have been logged out successfully")
                            .status("success")
                            .timestamp(LocalDateTime.now())
                            .build());
        } catch (Exception e) {
            log.error("Logout error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(MessageResponse.builder()
                    .status("error")
                    .message(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    /**
     * Revoke all refresh tokens for the user
     * Forces re-authentication on all devices
     * 
     * @return Success message
     */
    @PostMapping("/logout-all")
    public ResponseEntity<ApiResponse<MessageResponse>> logoutAllDevices() {
        try {
            // Get userId from SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UUID userId = UUID.fromString(authentication.getPrincipal().toString());
            
            authService.logoutAllDevices(userId);
            
            return ResponseEntity.ok(ApiResponse.<MessageResponse>builder()
                    .status("success")
                    .message("Logged out from all devices")
                    .data(MessageResponse.builder()
                            .message("You have been logged out from all devices")
                            .status("success")
                            .timestamp(LocalDateTime.now())
                            .build())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            System.out.println("Logout all error: {}" +  e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.<MessageResponse>builder()
                    .status("error")
                    .message(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }
}

