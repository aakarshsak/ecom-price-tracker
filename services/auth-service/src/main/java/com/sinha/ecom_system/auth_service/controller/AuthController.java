package com.sinha.ecom_system.auth_service.controller;


import com.sinha.ecom_system.auth_service.dto.request.LoginRequest;
import com.sinha.ecom_system.auth_service.dto.request.RefreshTokenRequest;
import com.sinha.ecom_system.auth_service.dto.request.RegisterRequest;
import com.sinha.ecom_system.auth_service.dto.response.ApiResponse;
import com.sinha.ecom_system.auth_service.dto.response.AuthResponse;
import com.sinha.ecom_system.auth_service.dto.response.MessageResponse;
import com.sinha.ecom_system.auth_service.dto.response.TokenResponse;
import com.sinha.ecom_system.auth_service.service.AuthService;
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

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register a new user
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
     * Login user
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse authResponse = authService.login(request);
            
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
     * Refresh access token
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
     * Logout user (revoke refresh token)
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        try {
            // Get userId from SecurityContext (set by JWT filter)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            UUID userId = UUID.fromString(authentication.getPrincipal().toString());
            
            String refreshToken = request != null ? request.getRefreshToken() : null;
            authService.logout(userId, refreshToken);
            
            return ResponseEntity.ok(ApiResponse.<MessageResponse>builder()
                    .status("success")
                    .message("Logout successful")
                    .data(MessageResponse.builder()
                            .message("You have been logged out successfully")
                            .status("success")
                            .timestamp(LocalDateTime.now())
                            .build())
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            System.out.println("Logout error: {}" +  e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.<MessageResponse>builder()
                    .status("error")
                    .message(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    /**
     * Logout from all devices
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

