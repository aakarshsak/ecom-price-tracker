package com.sinha.ecom_system.user_service.model;

import com.sinha.ecom_system.common.enums.AccountType;
import com.sinha.ecom_system.common.enums.Gender;
import com.sinha.ecom_system.common.enums.KycStatus;
import com.sinha.ecom_system.common.enums.TradingStatus;
import com.sinha.ecom_system.common.enums.RiskProfile;
import com.sinha.ecom_system.common.enums.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User entity representing user profile and business information
 * Separate from authentication (managed by auth-service)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_status", columnList = "user_status"),
    @Index(name = "idx_users_kyc", columnList = "kyc_status"),
    @Index(name = "idx_users_trading", columnList = "trading_status"),
    @Index(name = "idx_users_created", columnList = "created_at")
})
public class User {

    // Primary Key
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    // Basic Information
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;
    
    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;
    
    // Personal Information
    @Column(name = "dob")
    private LocalDate dob;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;
    
    @Column(name = "nationality", length = 50)
    private String nationality;
    
    // Status Fields
    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", length = 20)
    @Builder.Default
    private UserStatus userStatus = UserStatus.ACTIVE;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", length = 50)
    @Builder.Default
    private AccountType accountType = AccountType.RETAIL;
    
    // KYC (Know Your Customer) Fields
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", length = 20)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;
    
    @Column(name = "kyc_verified_at")
    private LocalDateTime kycVerifiedAt;
    
    @Column(name = "kyc_verified_by")
    private UUID kycVerifiedBy;
    
    // Trading Specific Fields
    @Enumerated(EnumType.STRING)
    @Column(name = "trading_status", length = 20)
    @Builder.Default
    private TradingStatus tradingStatus = TradingStatus.RESTRICTED;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_profile", length = 20)
    private RiskProfile riskProfile;
    
    // Metadata / Audit Fields
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;  // Soft delete timestamp
    
    // JPA Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.userStatus == null) {
            this.userStatus = UserStatus.ACTIVE;
        }
        if (this.accountType == null) {
            this.accountType = AccountType.RETAIL;
        }
        if (this.kycStatus == null) {
            this.kycStatus = KycStatus.PENDING;
        }
        if (this.tradingStatus == null) {
            this.tradingStatus = TradingStatus.RESTRICTED;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Business Methods
    
    /**
     * Check if user account is soft deleted
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
    
    /**
     * Check if user is active
     */
    public boolean isActive() {
        return this.userStatus == UserStatus.ACTIVE && !isDeleted();
    }
    
    /**
     * Check if user KYC is verified
     */
    public boolean isKycVerified() {
        return this.kycStatus == KycStatus.VERIFIED;
    }
    
    /**
     * Check if user can trade
     */
    public boolean canTrade() {
        return this.tradingStatus == TradingStatus.ENABLED 
            && isActive() 
            && isKycVerified();
    }
    
    /**
     * Soft delete the user
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.userStatus = UserStatus.DELETED;
    }
    
    /**
     * Update last active timestamp
     */
    public void updateLastActive() {
        this.lastActiveAt = LocalDateTime.now();
    }
    
    /**
     * Verify KYC
     */
    public void verifyKyc(UUID verifiedBy) {
        this.kycStatus = KycStatus.VERIFIED;
        this.kycVerifiedAt = LocalDateTime.now();
        this.kycVerifiedBy = verifiedBy;
    }
    
    /**
     * Enable trading for user (requires verified KYC)
     */
    public void enableTrading() {
        if (!isKycVerified()) {
            throw new IllegalStateException("Cannot enable trading without KYC verification");
        }
        this.tradingStatus = TradingStatus.ENABLED;
    }
}
