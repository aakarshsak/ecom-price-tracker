package com.sinha.ecom_system.auth_service.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AuthCredential Entity
 * 
 * Stores user authentication credentials and account security settings
 * Core entity for authentication system with many-to-many relationship to Roles
 * 
 * Features:
 * - Email/password authentication
 * - 2FA/TOTP support
 * - Account locking after failed attempts
 * - Role-based access control (RBAC)
 * - Audit timestamps
 */
@Entity
@Table(name = "auth_credentials")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"userRoles"})
@EqualsAndHashCode(of = {"id", "userId", "email"})
public class AuthCredential {

    // Primary identifier (auto-generated UUID)
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    // Authentication fields
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    
    @Column(name = "salt", length = 255)
    private String salt;  // Optional: BCrypt includes salt in hash

    // Verification flags
    @Column(name = "is_email_verified")
    private Boolean isEmailVerified;
    
    @Column(name = "is_phone_verified")
    private Boolean isPhoneVerified;
    
    // 2FA fields
    @Column(name = "is_2fa_enabled")
    private Boolean is2faEnabled;
    
    @Column(name = "totp_secret", length = 255)
    private String totpSecret;  // Secret for TOTP generation

    // Security - failed login tracking
    @Column(name = "failed_attempts")
    private Integer failedAttempts;
    
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    
    // Activity tracking
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @Column(name = "last_password_change")
    private LocalDateTime lastPasswordChange;

    // Audit fields
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private UUID createdBy;

    // Relationship to roles (many-to-many via UserRole junction table)
    @OneToMany(mappedBy = "authCredential", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();

    /**
     * JPA callback - Set defaults on entity creation
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.lastPasswordChange == null) {
            this.lastPasswordChange = LocalDateTime.now();
        }
        if (this.failedAttempts == null) {
            this.failedAttempts = 0;
        }
        if (this.isEmailVerified == null) {
            this.isEmailVerified = false;
        }
        if (this.isPhoneVerified == null) {
            this.isPhoneVerified = false;
        }
        if (this.is2faEnabled == null) {
            this.is2faEnabled = false;
        }
    }

    /**
     * JPA callback - Update timestamp on entity update
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Role Management Methods
     */

    /**
     * Assign role to this user
     * @param role The role to assign
     * @param grantedBy UUID of user who granted the role (null for system)
     */
    public void addRole(Role role, UUID grantedBy) {
        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(this.id, role.getId()))
                .authCredential(this)
                .role(role)
                .grantedAt(LocalDateTime.now())
                .grantedBy(grantedBy)
                .isActive(true)
                .build();
        userRoles.add(userRole);
    }

    /**
     * Remove role from this user
     */
    public void removeRole(Role role) {
        userRoles.removeIf(ur -> ur.getRole().getId().equals(role.getId()));
    }

    /**
     * Get list of active role names for this user
     */
    public List<String> getRoleNames() {
        return userRoles.stream()
                .filter(UserRole::isValid)
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toList());
    }

    /**
     * Check if user has specific role
     */
    public boolean hasRole(String roleName) {
        return userRoles.stream()
                .filter(UserRole::isValid)
                .anyMatch(ur -> ur.getRole().getName().equals(roleName));
    }

    /**
     * Check if user has specific permission through any of their roles
     */
    public boolean hasPermission(String permission) {
        return userRoles.stream()
                .filter(UserRole::isValid)
                .map(UserRole::getRole)
                .anyMatch(role -> role.getPermissions() != null &&
                        role.getPermissions().hasPermission(permission));
    }

    /**
     * Get all unique permissions from all user's roles
     */
    public Set<String> getAllPermissions() {
        Set<String> permissions = new HashSet<>();
        userRoles.stream()
                .filter(UserRole::isValid)
                .forEach(ur -> {
                    RolePermissions perms = ur.getRole().getPermissions();
                    if (perms != null) {
                        if (Boolean.TRUE.equals(perms.getCanTrade())) permissions.add("TRADE");
                        if (Boolean.TRUE.equals(perms.getCanWithdraw())) permissions.add("WITHDRAW");
                        if (Boolean.TRUE.equals(perms.getCanManageUsers())) permissions.add("MANAGE_USERS");
                        if (Boolean.TRUE.equals(perms.getCanViewReports())) permissions.add("VIEW_REPORTS");
                        if (Boolean.TRUE.equals(perms.getCanModifyOrders())) permissions.add("MODIFY_ORDERS");
                        if (Boolean.TRUE.equals(perms.getCanAccessAPI())) permissions.add("ACCESS_API");
                    }
                });
        return permissions;
    }

    /**
     * Security Methods
     */

    /**
     * Check if account is currently locked
     */
    public boolean isAccountLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    /**
     * Increment failed login attempt counter
     */
    public void incrementFailedAttempts() {
        this.failedAttempts = (this.failedAttempts == null ? 0 : this.failedAttempts) + 1;
    }

    /**
     * Reset failed attempts and unlock account
     * Called on successful login
     */
    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
    }

    /**
     * Lock account for specified duration
     * @param durationMinutes How long to lock the account
     */
    public void lockAccount(long durationMinutes) {
        this.lockedUntil = LocalDateTime.now().plusMinutes(durationMinutes);
    }
}
