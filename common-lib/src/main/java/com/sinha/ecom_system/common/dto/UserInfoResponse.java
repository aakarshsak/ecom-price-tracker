package com.sinha.ecom_system.common.dto;


import com.sinha.ecom_system.common.enums.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for user registration
 */
@Data
@Builder
public class UserInfoResponse {

    // Basic Information
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private LocalDate dob;
    private Gender gender;
    private String nationality;
    private UserStatus userStatus;
    private AccountType accountType;
    private KycStatus kycStatus;
    private LocalDateTime kycVerifiedAt;
    private UUID kycVerifiedBy;
    private TradingStatus tradingStatus;
    private RiskProfile riskProfile;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastActiveAt;
    private LocalDateTime deletedAt;  // Soft delete timestamp
}

