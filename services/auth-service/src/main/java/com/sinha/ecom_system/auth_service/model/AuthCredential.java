package com.sinha.ecom_system.auth_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "auth_credentials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthCredential {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    private UUID userId;          // Maps to user_id
    private String email;
    private String passwordHash;  // Maps to password_hash
    private String salt;

    private Boolean isEmailVerified;  // Maps to is_email_verified
    private Boolean isPhoneVerified;  // Maps to is_phone_verified
    private Boolean is2faEnabled;     // Maps to is_2fa_enabled
    private String totpSecret;        // Maps to totp_secret

    private Integer failedAttempts;   // Maps to failed_attempts
    private LocalDateTime lockedUntil;    // Maps to locked_until
    private LocalDateTime lastLogin;      // Maps to last_login
    private LocalDateTime lastPasswordChange; // Maps to last_password_change

    private LocalDateTime createdAt;  // Maps to created_at
    private LocalDateTime updatedAt;  // Maps to updated_at
    private UUID createdBy;           // Maps to created_by

    @OneToMany(mappedBy = "authCredential", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();

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

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


    /**
     * Add a role to this auth credential
     */
    public void addRole(Role role, UUID grantedBy) {
        UserRole userRole = UserRole.builder()
                .id(new UserRoleId(this.userId, role.getId()))
                .authCredential(this)
                .role(role)
                .grantedAt(LocalDateTime.now())
                .grantedBy(grantedBy)
                .isActive(true)
                .build();
        userRoles.add(userRole);
    }

    /**
     * Remove a role from this auth credential
     */
    public void removeRole(Role role) {
        userRoles.removeIf(ur -> ur.getRole().getId().equals(role.getId()));
    }

    /**
     * Get all active role names
     */
    public List<String> getRoleNames() {
        return userRoles.stream()
                .filter(UserRole::isValid)
                .map(ur -> ur.getRole().getName())
                .collect(Collectors.toList());
    }

    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String roleName) {
        return userRoles.stream()
                .filter(UserRole::isValid)
                .anyMatch(ur -> ur.getRole().getName().equals(roleName));
    }

    /**
     * Check if user has a specific permission
     */
    public boolean hasPermission(String permission) {
        return userRoles.stream()
                .filter(UserRole::isValid)
                .map(UserRole::getRole)
                .anyMatch(role -> role.getPermissions() != null &&
                        role.getPermissions().hasPermission(permission));
    }

    /**
     * Get all permissions from all roles
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
     * Check if account is locked
     */
    public boolean isAccountLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    /**
     * Increment failed login attempts
     */
    public void incrementFailedAttempts() {
        this.failedAttempts = (this.failedAttempts == null ? 0 : this.failedAttempts) + 1;
    }

    /**
     * Reset failed login attempts
     */
    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.lockedUntil = null;
    }

    /**
     * Lock the account for specified minutes
     */
    public void lockAccount(long durationMinutes) {
        this.lockedUntil = LocalDateTime.now().plusMinutes(durationMinutes);
    }
}
