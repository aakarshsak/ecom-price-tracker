package com.sinha.ecom_system.auth_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {
    
    private String secretKey;
    private long accessTokenExpiry;
    private long refreshTokenExpiry;
    private String issuer;
}

