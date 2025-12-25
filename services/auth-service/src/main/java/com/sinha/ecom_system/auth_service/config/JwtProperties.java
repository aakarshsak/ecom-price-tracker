package com.sinha.ecom_system.auth_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {
    
    private String secretKey = "your-secret-key-change-in-production-must-be-at-least-256-bits-long-for-HS256";
    private long accessTokenExpiry = 900000; // 15 minutes in milliseconds
    private long refreshTokenExpiry = 604800000; // 7 days in milliseconds
    private String issuer = "trading-platform-auth-service";
}

