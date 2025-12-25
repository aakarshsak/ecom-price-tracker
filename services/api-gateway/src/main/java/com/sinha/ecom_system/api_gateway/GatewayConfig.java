package com.sinha.ecom_system.api_gateway;


import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", r -> r
                        .path("/v1/api/user-service/**")
                        .filters(f -> f
                                .rewritePath("/v1/api/user-service/(?<segment>.*)", "/${segment}"))
                        .uri("lb://USER-SERVICE"))
                .route("auth-service", r -> r
                        .path("/v1/api/auth-service/**")
                        .filters(f -> f
                                .rewritePath("/v1/api/auth-service/(?<segment>.*)", "/${segment}"))
                .uri("lb://AUTH-SERVICE"))
                .build();
    }
}
