package com.sinha.ecom_system.user_service.dto;

import com.sinha.ecom_system.user_service.model.Gender;
import com.sinha.ecom_system.user_service.model.RiskProfile;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request DTO for updating user profile
 * Only includes fields that users can update themselves
 */
@Data
public class UpdateUserRequest {
    // Basic Information
    private String firstName;
    private String lastName;
    private String mobileNumber;
    
    // Personal Information
    private LocalDate dob;
    private Gender gender;
    private String nationality;
    
    // Trading Preferences
    private RiskProfile riskProfile;
}

