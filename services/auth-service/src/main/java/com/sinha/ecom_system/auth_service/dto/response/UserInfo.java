package com.sinha.ecom_system.auth_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo {

    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private LocalDate dob;

    private Boolean isEmailVerified;
    private Boolean isPhoneVerified;
    private Boolean is2faEnabled;

    private List<String> roles;
    private List<String> permissions;

    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
}
