package com.sinha.ecom_system.api_gateway.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;


/**
 * Gateway Security Configuration
 * Adds internal secret header to prove request came from gateway
 */
@Configuration
public class GatewaySecurityConfig {

    @Value("${api-gateway.secret-key}")
    private String gatewaySecret;

    /**
     * Add secret header to all requests going to downstream services
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public GlobalFilter gatewaySecretHeaderFilter() {
        return (exchange, chain) -> {
            return chain.filter(
                    exchange.mutate()
                            .request(exchange.getRequest().mutate()
                                    .header("X-Gateway-Secret", gatewaySecret)
                                    .build())
                            .build()
            );
        };
    }
}
