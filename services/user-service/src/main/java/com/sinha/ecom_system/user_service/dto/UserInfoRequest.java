package com.sinha.ecom_system.user_service.dto;

import com.sinha.ecom_system.user_service.model.*;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for user registration
 */
@Data
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

