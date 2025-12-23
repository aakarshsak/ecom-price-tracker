package com.sinha.ecom_system.user_service.dto;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class RegisterUserRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private LocalDate dob;
    private String password;
}
