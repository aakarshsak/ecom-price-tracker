package com.sinha.ecom_system.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * API Gateway Application
 * Entry point for the reactive API Gateway service
 * 
 * This service:
 * - Routes requests to downstream microservices
 * - Validates JWT tokens globally
 * - Adds user context headers to requests
 * - Provides a single entry point for the trading platform
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.sinha.ecom_system.common", "com.sinha.ecom_system.api_gateway"})
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

}
