package com.sinha.ecom_system.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway Route Configuration
 * Defines routing rules for microservices
 */
@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // User Service Routes
                .route("user-service", r -> r
                        .path("/v1/api/user-service/**")
                        .filters(f -> f
                                .rewritePath("/v1/api/user-service/(?<segment>.*)", "/${segment}")
                                .removeRequestHeader("Cookie")) // Remove cookies for stateless behavior
                        .uri("lb://USER-SERVICE"))
                
                // Auth Service Routes
                .route("auth-service", r -> r
                        .path("/v1/api/auth-service/**")
                        .filters(f -> f
                                .rewritePath("/v1/api/auth-service/(?<segment>.*)", "/${segment}")
                                .removeRequestHeader("Cookie"))
                        .uri("lb://AUTH-SERVICE"))
                
                .build();
    }
}

