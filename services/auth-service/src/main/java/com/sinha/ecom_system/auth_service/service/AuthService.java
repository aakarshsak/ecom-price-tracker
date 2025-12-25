package com.sinha.ecom_system.auth_service.service;

import com.sinha.ecom_system.auth_service.dto.request.RegisterRequest;
import com.sinha.ecom_system.auth_service.dto.response.AuthResponse;

import java.util.UUID;

public interface AuthService {
    AuthResponse registerAuth(RegisterRequest request);

    public boolean hasPermission(UUID userId, String permission);

    public void assignRole(UUID userId, String roleName, UUID grantedBy);

    public void removeRole(UUID userId, String roleName);
}
