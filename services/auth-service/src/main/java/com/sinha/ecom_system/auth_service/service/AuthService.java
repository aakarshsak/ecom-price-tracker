package com.sinha.ecom_system.auth_service.service;

import com.sinha.ecom_system.auth_service.dto.request.LoginRequest;
import com.sinha.ecom_system.auth_service.dto.request.RefreshTokenRequest;
import com.sinha.ecom_system.auth_service.dto.request.RegisterRequest;
import com.sinha.ecom_system.auth_service.dto.response.AuthResponse;
import com.sinha.ecom_system.auth_service.dto.response.TokenResponse;

import java.util.UUID;

public interface AuthService {
    AuthResponse registerAuth(RegisterRequest request);
    
    AuthResponse login(LoginRequest request);
    
    TokenResponse refreshAccessToken(RefreshTokenRequest request);
    
    void logout(UUID userId, String refreshToken);
    
    void logoutAllDevices(UUID userId);

    boolean hasPermission(UUID userId, String permission);

    void assignRole(UUID userId, String roleName, UUID grantedBy);

    void removeRole(UUID userId, String roleName);
}
