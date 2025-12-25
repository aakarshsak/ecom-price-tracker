package com.sinha.ecom_system.auth_service.controller;


import com.sinha.ecom_system.auth_service.dto.request.RegisterRequest;
import com.sinha.ecom_system.auth_service.dto.response.ApiResponse;
import com.sinha.ecom_system.auth_service.dto.response.AuthResponse;
import com.sinha.ecom_system.auth_service.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> registerUser(@RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.registerAuth(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.<AuthResponse>builder()
                .status("success")
                .message("Registration successful")
                .data(authResponse)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
