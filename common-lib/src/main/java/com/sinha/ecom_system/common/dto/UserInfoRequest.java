package com.sinha.ecom_system.common.dto;

import com.sinha.ecom_system.common.enums.Gender;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Request DTO for user registration
 */
@Data
@Builder
public class UserInfoRequest {

    // Basic Information
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private LocalDate dob;
    private Gender gender;
    private String nationality; // Soft delete timestamp
}

