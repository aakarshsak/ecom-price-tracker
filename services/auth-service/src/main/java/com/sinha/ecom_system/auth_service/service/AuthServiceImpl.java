package com.sinha.ecom_system.auth_service.service;

import com.sinha.ecom_system.auth_service.dto.request.RegisterRequest;
import com.sinha.ecom_system.auth_service.dto.response.AuthResponse;
import com.sinha.ecom_system.auth_service.dto.response.UserInfo;
import com.sinha.ecom_system.auth_service.model.AuthCredential;
import com.sinha.ecom_system.auth_service.model.Role;
import com.sinha.ecom_system.auth_service.repository.AuthRepository;
import com.sinha.ecom_system.auth_service.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private AuthRepository authRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public AuthServiceImpl(AuthRepository authRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.authRepository = authRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public AuthResponse registerAuth(RegisterRequest request) {
        // Check if email already exists
        if (authRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Generate userId
        UUID userId = UUID.randomUUID();

        // Hash password
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // Create AuthCredential
        AuthCredential authCredential = AuthCredential.builder()
                .userId(userId)
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordHash)
                .isEmailVerified(false)
                .isPhoneVerified(false)
                .is2faEnabled(false)
                .failedAttempts(0)
                .build();

        // ⭐ ASSIGN DEFAULT ROLE (ROLE_USER)
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        authCredential.addRole(userRole, null);  // null = system granted

        // Save
        authCredential = authRepository.save(authCredential);

        // Build response
        return AuthResponse.builder()
                .accessToken("temp-token")  // TODO: Generate real JWT
                .refreshToken("temp-refresh")  // TODO: Generate real refresh token
                .tokenType("Bearer")
                .expiresIn(900L)
                .user(UserInfo.builder()
                        .userId(authCredential.getUserId())
                        .email(authCredential.getEmail())
                        .roles(authCredential.getRoleNames())
                        .permissions(authCredential.getAllPermissions().stream().toList())
                        .isEmailVerified(authCredential.getIsEmailVerified())
                        .is2faEnabled(authCredential.getIs2faEnabled())
                        .createdAt(authCredential.getCreatedAt())
                        .build())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    public boolean hasPermission(UUID userId, String permission) {
        AuthCredential authCredential = authRepository.findByUserIdWithRoles(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return authCredential.hasPermission(permission);
    }

    @Override
    @Transactional
    public void assignRole(UUID userId, String roleName, UUID grantedBy) {
        AuthCredential authCredential = authRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        authCredential.addRole(role, grantedBy);
        authRepository.save(authCredential);
    }

    @Override
    @Transactional
    public void removeRole(UUID userId, String roleName) {
        AuthCredential authCredential = authRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        authCredential.removeRole(role);
        authRepository.save(authCredential);
    }
}