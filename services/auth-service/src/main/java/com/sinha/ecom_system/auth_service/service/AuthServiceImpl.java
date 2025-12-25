package com.sinha.ecom_system.auth_service.service;

import com.sinha.ecom_system.auth_service.config.JwtProperties;
import com.sinha.ecom_system.auth_service.dto.request.LoginRequest;
import com.sinha.ecom_system.auth_service.dto.request.RefreshTokenRequest;
import com.sinha.ecom_system.auth_service.dto.request.RegisterRequest;
import com.sinha.ecom_system.auth_service.dto.response.AuthResponse;
import com.sinha.ecom_system.auth_service.dto.response.TokenResponse;
import com.sinha.ecom_system.auth_service.dto.response.UserInfo;
import com.sinha.ecom_system.auth_service.model.AuthCredential;
import com.sinha.ecom_system.auth_service.model.RefreshToken;
import com.sinha.ecom_system.auth_service.model.Role;
import com.sinha.ecom_system.auth_service.repository.AuthRepository;
import com.sinha.ecom_system.auth_service.repository.RefreshTokenRepository;
import com.sinha.ecom_system.auth_service.repository.RoleRepository;
import com.sinha.ecom_system.auth_service.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthRepository authRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    @Autowired
    public AuthServiceImpl(
            AuthRepository authRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            JwtProperties jwtProperties) {
        this.authRepository = authRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional
    public AuthResponse registerAuth(RegisterRequest request) {
        // Check if email already exists
        if (authRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Hash password
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // Create AuthCredential
        AuthCredential authCredential = AuthCredential.builder()
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordHash)
                .isEmailVerified(false)
                .isPhoneVerified(false)
                .is2faEnabled(false)
                .failedAttempts(0)
                .build();

        System.out.println("Checking if here");

        // Assign default role (ROLE_USER)
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        authCredential.addRole(userRole, null);

        // Save
        authCredential = authRepository.save(authCredential);

        // Generate tokens
        String accessToken = generateAccessToken(authCredential);
        String refreshToken = generateAndStoreRefreshToken(authCredential, null, null);

        // Build response
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiry() / 1000) // Convert to seconds
                .user(buildUserInfo(authCredential))
                .requires2FA(false)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Find user by email
        AuthCredential authCredential = authRepository.findByEmailWithRoles(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Check if account is locked
        if (authCredential.isAccountLocked()) {
            throw new RuntimeException("Account is locked. Please try again later.");
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), authCredential.getPasswordHash())) {
            // Increment failed attempts
            authCredential.incrementFailedAttempts();
            
            // Lock account after 5 failed attempts
            if (authCredential.getFailedAttempts() >= 5) {
                authCredential.lockAccount(15); // Lock for 15 minutes
            }
            
            authRepository.save(authCredential);
            throw new RuntimeException("Invalid email or password");
        }

        // Check if 2FA is enabled
        if (Boolean.TRUE.equals(authCredential.getIs2faEnabled())) {
            // TODO: Implement 2FA flow
            // For now, just return requires2FA flag
            return AuthResponse.builder()
                    .requires2FA(true)
                    .tempToken("temp-2fa-token")
                    .user(UserInfo.builder()
                            .email(authCredential.getEmail())
                            .is2faEnabled(true)
                            .build())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // Reset failed attempts on successful login
        authCredential.resetFailedAttempts();
        authCredential.setLastLogin(LocalDateTime.now());
        authRepository.save(authCredential);

        // Generate tokens
        String accessToken = generateAccessToken(authCredential);
        String refreshToken = generateAndStoreRefreshToken(
                authCredential,
                null,
                null // TODO: Get IP from request
        );

        // Build response
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiry() / 1000)
                .user(buildUserInfo(authCredential))
                .requires2FA(false)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public TokenResponse refreshAccessToken(RefreshTokenRequest request) {
        // Validate refresh token format
        if (!jwtUtil.validateToken(request.getRefreshToken())) {
            throw new RuntimeException("Invalid refresh token");
        }

        // Check token type
        String tokenType = jwtUtil.getTokenType(request.getRefreshToken());
        if (!"REFRESH".equals(tokenType)) {
            throw new RuntimeException("Invalid token type");
        }

        // Get userId from token
        UUID userId = jwtUtil.getUserIdFromToken(request.getRefreshToken());

        // Hash the refresh token to find in database
        String tokenHash = hashToken(request.getRefreshToken());

        // Find refresh token in database
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        // Validate token
        if (!storedToken.isValid()) {
            throw new RuntimeException("Refresh token is invalid or expired");
        }

        // Verify userId matches
        if (!storedToken.getAuthId().equals(userId)) {
            throw new RuntimeException("Token user mismatch");
        }

        // Get user details with roles
        AuthCredential authCredential = authRepository.findByUserIdWithRoles(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate new access token
        String newAccessToken = generateAccessToken(authCredential);

        // Optionally, rotate refresh token for security
        // For now, we'll keep the same refresh token
        
        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiry() / 1000)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public void logout(UUID userId, String refreshToken) {
        if (refreshToken != null) {
            String tokenHash = hashToken(refreshToken);
            refreshTokenRepository.revokeToken(tokenHash, LocalDateTime.now());
        }
    }

    @Override
    @Transactional
    public void logoutAllDevices(UUID userId) {
        refreshTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());
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
        AuthCredential authCredential = authRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        authCredential.addRole(role, grantedBy);
        authRepository.save(authCredential);
    }

    @Override
    @Transactional
    public void removeRole(UUID userId, String roleName) {
        AuthCredential authCredential = authRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        authCredential.removeRole(role);
        authRepository.save(authCredential);
    }

    // Helper Methods

    private String generateAccessToken(AuthCredential authCredential) {
        List<String> roles = authCredential.getRoleNames();
        List<String> permissions = authCredential.getAllPermissions().stream().toList();

        return jwtUtil.generateAccessToken(
                authCredential.getId(),
                authCredential.getEmail(),
                roles,
                permissions
        );
    }

    private String generateAndStoreRefreshToken(AuthCredential authCredential, String deviceInfo, String ipAddress) {
        // Generate refresh token
        String refreshToken = jwtUtil.generateRefreshToken(authCredential.getId());

        // Hash the token for storage
        String tokenHash = hashToken(refreshToken);

        // Calculate expiry date
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtProperties.getRefreshTokenExpiry() / 1000);

        // Store in database
        RefreshToken storedToken = RefreshToken.builder()
                .authId(authCredential.getId())
                .tokenHash(tokenHash)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(storedToken);

        return refreshToken;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }

    private UserInfo buildUserInfo(AuthCredential authCredential) {
        return UserInfo.builder()
                .userId(authCredential.getId())
                .email(authCredential.getEmail())
                .roles(authCredential.getRoleNames())
                .permissions(authCredential.getAllPermissions().stream().toList())
                .isEmailVerified(authCredential.getIsEmailVerified())
                .isPhoneVerified(authCredential.getIsPhoneVerified())
                .is2faEnabled(authCredential.getIs2faEnabled())
                .createdAt(authCredential.getCreatedAt())
                .lastLogin(authCredential.getLastLogin())
                .build();
    }
}
