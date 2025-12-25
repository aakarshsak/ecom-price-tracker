package com.sinha.ecom_system.auth_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn; // seconds

    private UserInfo user;
    
    // For 2FA flow
    private Boolean requires2FA;
    private String tempToken; // Temporary token for 2FA verification

    private LocalDateTime timestamp;

}
