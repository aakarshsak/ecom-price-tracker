package com.sinha.ecom_system.api_gateway.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT Configuration
 * Reads JWT properties from environment variables
 */
@Configuration
@Data
public class JwtConfig {
    
    @Value("${JWT_SECRET_KEY}")
    private String secretKey;
    
    @Value("${JWT_ACCESS_TOKEN_EXPIRY:900000}") // Default: 15 minutes
    private long accessTokenExpiry;
    
    @Value("${JWT_REFRESH_TOKEN_EXPIRY:604800000}") // Default: 7 days
    private long refreshTokenExpiry;
    
    @Value("${JWT_ISSUER:trading-platform}")
    private String issuer;
}

