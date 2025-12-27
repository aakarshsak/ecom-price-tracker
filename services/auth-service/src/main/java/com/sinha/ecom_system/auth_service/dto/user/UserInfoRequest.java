package com.sinha.ecom_system.auth_service.dto.user;

import com.sinha.ecom_system.auth_service.model.user.Gender;
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

