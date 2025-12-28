package com.sinha.ecom_system.auth_service.service;

import com.sinha.ecom_system.auth_service.dto.request.LoginRequest;
import com.sinha.ecom_system.auth_service.dto.request.RefreshTokenRequest;
import com.sinha.ecom_system.auth_service.dto.request.RegisterRequest;
import com.sinha.ecom_system.auth_service.dto.response.AuthResponse;
import com.sinha.ecom_system.auth_service.dto.response.TokenResponse;
import com.sinha.ecom_system.auth_service.dto.response.UserInfo;
import com.sinha.ecom_system.auth_service.model.AuthCredential;
import com.sinha.ecom_system.auth_service.model.RefreshToken;
import com.sinha.ecom_system.auth_service.model.Role;
import com.sinha.ecom_system.auth_service.proxy.UserProxy;
import com.sinha.ecom_system.auth_service.repository.AuthRepository;
import com.sinha.ecom_system.auth_service.repository.RefreshTokenRepository;
import com.sinha.ecom_system.auth_service.repository.RoleRepository;
import com.sinha.ecom_system.common.config.JwtProperties;
import com.sinha.ecom_system.common.dto.ApiResponse;
import com.sinha.ecom_system.common.dto.UserInfoRequest;
import com.sinha.ecom_system.common.dto.UserInfoResponse;
import com.sinha.ecom_system.common.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of Authentication Service
 * Handles user registration, login, token management, and role assignment
 */
@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthRepository authRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserProxy proxy;

    @Autowired
    public AuthServiceImpl(
            AuthRepository authRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            JwtProperties jwtProperties,
            TokenBlacklistService tokenBlacklistService,
            UserProxy proxy) {
        this.authRepository = authRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
        this.tokenBlacklistService = tokenBlacklistService;
        this.proxy = proxy;
    }

    @Override
    @Transactional
    public AuthResponse registerAuth(RegisterRequest request) {
        // Validate email uniqueness
        if (authRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Hash password using BCrypt
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // Create new AuthCredential entity
        AuthCredential authCredential = AuthCredential.builder()
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordHash)
                .isEmailVerified(false)
                .isPhoneVerified(false)
                .is2faEnabled(false)
                .failedAttempts(0)
                .build();

        ResponseEntity<ApiResponse<UserInfoResponse>> response = proxy.addUser(UserInfoRequest.builder()
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .mobileNumber(request.getMobileNumber())
                        .dob(request.getDob())
                        .email(authCredential.getEmail())
                        .build());

        UUID userId = response.getBody().getData().getUserId();

        // Assign default ROLE_USER role
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        authCredential.addRole(userRole, null);
        authCredential.setUserId(userId);

        // Persist to database (ID auto-generated)
        authCredential = authRepository.save(authCredential);

        // Generate JWT tokens
        String accessToken = generateAccessToken(authCredential);
        String refreshToken = generateAndStoreRefreshToken(authCredential, null, null);

        // Return auth response with tokens and user info
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiry() / 1000)
                .user(buildUserInfo(authCredential, userId))
                .requires2FA(false)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Fetch user by email with roles eagerly loaded
        AuthCredential authCredential = authRepository.findByEmailWithRoles(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        // Prevent login if account is locked
        if (authCredential.isAccountLocked()) {
            throw new RuntimeException("Account is locked. Please try again later.");
        }

        // Validate password with BCrypt
        if (!passwordEncoder.matches(request.getPassword(), authCredential.getPasswordHash())) {
            authCredential.incrementFailedAttempts();
            
            // Lock account after 5 consecutive failed attempts
            if (authCredential.getFailedAttempts() >= 5) {
                authCredential.lockAccount(15);
            }
            
            authRepository.save(authCredential);
            throw new RuntimeException("Invalid email or password");
        }

        // If 2FA enabled, return temp token for 2FA verification
        if (Boolean.TRUE.equals(authCredential.getIs2faEnabled())) {
            // TODO: Implement full 2FA flow with OTP generation
            return AuthResponse.builder()
                    .requires2FA(true)
                    .tempToken("temp-2fa-token")
                    .user(UserInfo.builder()
                            .email(authCredential.getEmail())
                            .build())
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        // Successful login: reset failed attempts and update last login
        authCredential.resetFailedAttempts();
        authCredential.setLastLogin(LocalDateTime.now());
        authRepository.save(authCredential);

        // Generate access and refresh tokens
        String accessToken = generateAccessToken(authCredential);
        String refreshToken = generateAndStoreRefreshToken(
                authCredential,
                null,  // TODO: Extract device info from User-Agent
                null   // TODO: Extract IP address from request
        );

        ResponseEntity<ApiResponse<UserInfoResponse>> response = proxy.getUser(authCredential.getUserId());
        UUID userId = response.getBody().getData().getUserId();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiry() / 1000)
                .user(buildUserInfo(authCredential, userId))
                .requires2FA(false)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional
    public TokenResponse refreshAccessToken(RefreshTokenRequest request) {
        // Validate JWT signature and structure
        if (!jwtUtil.validateToken(request.getRefreshToken())) {
            throw new RuntimeException("Invalid refresh token");
        }

        // Ensure token is a REFRESH token, not ACCESS token
        String tokenType = jwtUtil.getTokenType(request.getRefreshToken());
        if (!"REFRESH".equals(tokenType)) {
            throw new RuntimeException("Invalid token type");
        }

        // Extract userId from JWT claims
        UUID userId = jwtUtil.getUserIdFromToken(request.getRefreshToken());

        // Hash token to lookup in database (we don't store raw tokens)
        String tokenHash = hashToken(request.getRefreshToken());

        // Verify token exists in database and is not revoked
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        // Check expiry and revoked status
        if (!storedToken.isValid()) {
            throw new RuntimeException("Refresh token is invalid or expired");
        }

        // Verify token belongs to the user in JWT claims (prevent token theft)
        if (!storedToken.getAuthId().equals(userId)) {
            throw new RuntimeException("Token user mismatch");
        }

        // Fetch user with roles for new access token
        AuthCredential authCredential = authRepository.findByUserIdWithRoles(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate new access token with current user roles/permissions
        String newAccessToken = generateAccessToken(authCredential);

        // TODO: Implement refresh token rotation for better security
        // For now, return same refresh token
        
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
    public void logout(UUID userId, String accessToken) {
        // Add access token to Redis blacklist to prevent reuse
        if (accessToken != null) {
            try {
                String tokenId = jwtUtil.getTokenId(accessToken);
                Date expiryDate = jwtUtil.getExpirationDate(accessToken);
                
                // Blacklist token with TTL matching its natural expiry
                tokenBlacklistService.blacklistToken(tokenId, expiryDate);
                
                log.info("Access token blacklisted for user: {}", userId);
            } catch (Exception e) {
                log.error("Error blacklisting token: {}", e.getMessage());
                throw new RuntimeException("Failed to logout: " + e.getMessage());
            }
        }
        
        // Revoke all refresh tokens to force re-login on all devices
        refreshTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());
        
        log.info("User {} logged out successfully", userId);
    }

    @Override
    @Transactional
    public void logoutAllDevices(UUID userId) {
        // Revoke all refresh tokens for this user
        refreshTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());
    }

    @Override
    public boolean hasPermission(UUID userId, String permission) {
        // Check if user has specific permission through their roles
        AuthCredential authCredential = authRepository.findByUserIdWithRoles(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return authCredential.hasPermission(permission);
    }

    @Override
    @Transactional
    public void assignRole(UUID userId, String roleName, UUID grantedBy) {
        // Add role to user's account
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
        // Remove role from user's account
        AuthCredential authCredential = authRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        authCredential.removeRole(role);
        authRepository.save(authCredential);
    }

    /**
     * Helper Methods
     */

    /**
     * Generate JWT access token with user's roles and permissions
     */
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

    /**
     * Generate refresh token, hash it, and store in database
     * Raw token returned to client, hash stored in DB for security
     */
    private String generateAndStoreRefreshToken(AuthCredential authCredential, String deviceInfo, String ipAddress) {
        // Generate JWT refresh token
        String refreshToken = jwtUtil.generateRefreshToken(authCredential.getId());

        // Hash token before storing (never store raw tokens)
        String tokenHash = hashToken(refreshToken);

        // Calculate expiry timestamp
        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(jwtProperties.getRefreshTokenExpiry() / 1000);

        // Create and persist refresh token record
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

    /**
     * Hash token using SHA-256 for secure storage
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }

    /**
     * Build UserInfo DTO from AuthCredential entity
     */
    private UserInfo buildUserInfo(AuthCredential authCredential, UUID userId) {
        return UserInfo.builder()
                .userId(userId)
                .email(authCredential.getEmail())
                .roles(authCredential.getRoleNames())
                .permissions(authCredential.getAllPermissions().stream().toList())
                .isPhoneVerified(authCredential.getIsPhoneVerified())
                .isEmailVerified(authCredential.getIsEmailVerified())
                .is2faEnabled(authCredential.getIs2faEnabled())
                .createdAt(authCredential.getCreatedAt())
                .lastLogin(authCredential.getLastLogin())
                .build();
    }
}
