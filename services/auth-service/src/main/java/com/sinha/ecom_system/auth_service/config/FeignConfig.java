package com.sinha.ecom_system.auth_service.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Value("${api-gateway.secret-key}")
    private String gatewaySecret;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // Add custom headers or logging logic here
            requestTemplate.header("X-Gateway-Secret", gatewaySecret);
        };
    }
}
