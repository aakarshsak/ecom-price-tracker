# 🌐 API Gateway Implementation Guide

Complete guide for implementing Spring Cloud Gateway for the Trading Platform.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Setup & Configuration](#setup--configuration)
- [Routing Configuration](#routing-configuration)
- [Security Integration](#security-integration)
- [Advanced Features](#advanced-features)
- [Deployment](#deployment)

---

## Overview

The API Gateway serves as the single entry point for all client requests, providing routing, security, rate limiting, and observability.

### Key Responsibilities

✅ **Request Routing** - Route requests to appropriate microservices  
✅ **Authentication** - Validate JWT tokens before routing  
✅ **Rate Limiting** - Prevent abuse and DDoS attacks  
✅ **CORS Handling** - Configure cross-origin requests  
✅ **Load Balancing** - Distribute requests across instances  
✅ **Request/Response Transformation** - Modify headers, paths  
✅ **Circuit Breaking** - Handle downstream failures gracefully  
✅ **Logging & Monitoring** - Track all incoming requests

### Tech Stack

| Component | Technology |
|-----------|-----------|
| **Framework** | Spring Cloud Gateway 4.x |
| **Service Discovery** | Eureka Client |
| **Rate Limiting** | Redis |
| **Security** | Spring Security + JWT Filter |
| **Port** | 8080 (or 8765) |

---

## Architecture

### Gateway Flow

```
┌─────────────┐
│   Client    │
│ (Web/Mobile)│
└──────┬──────┘
       │
       │ HTTP/HTTPS Request
       ▼
┌─────────────────────────────────────────────┐
│           API GATEWAY (Port 8080)           │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │        Global Filters               │   │
│  │  • CORS Filter                      │   │
│  │  • JWT Validation Filter            │   │
│  │  • Request Logging Filter           │   │
│  │  • Rate Limiting Filter             │   │
│  └─────────────────────────────────────┘   │
│                    │                        │
│                    ▼                        │
│  ┌─────────────────────────────────────┐   │
│  │        Route Predicates             │   │
│  │  • Path: /auth/**  → Auth Service   │   │
│  │  • Path: /user/**  → User Service   │   │
│  │  • Path: /order/** → Order Service  │   │
│  │  • Path: /market/**→ Market Service │   │
│  └─────────────────────────────────────┘   │
│                    │                        │
│                    ▼                        │
│  ┌─────────────────────────────────────┐   │
│  │        Route Filters                │   │
│  │  • Add/Remove Headers               │   │
│  │  • Path Rewriting                   │   │
│  │  • Circuit Breaker                  │   │
│  └─────────────────────────────────────┘   │
└─────────────────┬───────────────────────────┘
                  │
      ┌───────────┼───────────┐
      │           │           │
      ▼           ▼           ▼
┌──────────┐ ┌──────────┐ ┌──────────┐
│   Auth   │ │   User   │ │  Order   │
│ Service  │ │ Service  │ │ Service  │
│  :8081   │ │  :8082   │ │  :8084   │
└──────────┘ └──────────┘ └──────────┘
```

---

## Setup & Configuration

### Step 1: Maven Dependencies

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Cloud Gateway -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>
    
    <!-- Eureka Client (Service Discovery) -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
    
    <!-- Spring Boot Actuator -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    
    <!-- Redis for Rate Limiting -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
    </dependency>
    
    <!-- JWT for Authentication -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.3</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.3</version>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Circuit Breaker -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2025.0.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Step 2: Application Configuration

```yaml
# application.yaml
spring:
  application:
    name: api-gateway
  
  cloud:
    gateway:
      # Global CORS Configuration
      globalcors:
        cors-configurations:
          '[/**]':
            allowed-origins:
              - "http://localhost:3000"
              - "https://yourdomain.com"
            allowed-methods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowed-headers:
              - "*"
            allow-credentials: true
            max-age: 3600
      
      # Route Definitions
      routes:
        # Auth Service Routes
        - id: auth-service
          uri: lb://AUTH-SERVICE
          predicates:
            - Path=/auth/**
          filters:
            - StripPrefix=0
        
        # User Service Routes
        - id: user-service
          uri: lb://USER-SERVICE
          predicates:
            - Path=/user/**
          filters:
            - StripPrefix=0
            - name: CircuitBreaker
              args:
                name: userServiceCircuitBreaker
                fallbackUri: forward:/fallback/user
        
        # Order Service Routes
        - id: order-service
          uri: lb://ORDER-SERVICE
          predicates:
            - Path=/order/**
          filters:
            - StripPrefix=0
            - name: RequestRateLimiter
              args:
                redis-rate-limiter:
                  replenishRate: 100
                  burstCapacity: 200
        
        # Market Data Service Routes
        - id: market-data-service
          uri: lb://MARKET-DATA-SERVICE
          predicates:
            - Path=/market/**
          filters:
            - StripPrefix=0

server:
  port: 8080

# Eureka Configuration
eureka:
  client:
    serviceUrl:
      defaultZone: ${EUREKA_SERVER_URL:http://localhost:8761/eureka}
  instance:
    instance-id: ${spring.application.name}:${server.port}

# Redis Configuration (for rate limiting)
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}

# Actuator Configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,gateway
  endpoint:
    health:
      show-details: always

# Logging
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    reactor.netty: INFO
```

---

## Routing Configuration

### Static Routing (Direct URLs)

```yaml
routes:
  - id: auth-service-static
    uri: http://localhost:8081
    predicates:
      - Path=/auth/**
```

### Load-Balanced Routing (with Eureka)

```yaml
routes:
  - id: auth-service-lb
    uri: lb://AUTH-SERVICE  # Service name in Eureka
    predicates:
      - Path=/auth/**
```

### Path Rewriting

```yaml
routes:
  - id: api-v1-auth
    uri: lb://AUTH-SERVICE
    predicates:
      - Path=/api/v1/auth/**
    filters:
      - RewritePath=/api/v1/auth/(?<segment>.*), /auth/${segment}
      # /api/v1/auth/login → /auth/login
```

### Header-Based Routing

```yaml
routes:
  - id: mobile-api
    uri: lb://MOBILE-API-SERVICE
    predicates:
      - Path=/api/**
      - Header=X-Platform, mobile
```

---

## Security Integration

### JWT Validation Filter

```java
package com.trading.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret-key}")
    private String jwtSecret;

    private static final String[] PUBLIC_PATHS = {
        "/auth/register",
        "/auth/login",
        "/auth/refresh",
        "/actuator/health"
    };

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        // Skip JWT validation for public endpoints
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // Extract JWT token from Authorization header
        String token = extractToken(request);

        if (token == null || !validateToken(token)) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        // Add user info to request headers
        Claims claims = extractClaims(token);
        ServerHttpRequest modifiedRequest = request.mutate()
            .header("X-User-Id", claims.getSubject())
            .header("X-User-Email", claims.get("email", String.class))
            .header("X-User-Roles", String.join(",", claims.get("roles", List.class)))
            .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private boolean isPublicPath(String path) {
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private boolean validateToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    @Override
    public int getOrder() {
        return -100; // High priority
    }
}
```

---

## Advanced Features

### 1. Rate Limiting

```java
@Bean
public KeyResolver userKeyResolver() {
    return exchange -> {
        // Rate limit by user ID (from JWT)
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        return Mono.just(userId != null ? userId : "anonymous");
    };
}

@Bean
public KeyResolver ipKeyResolver() {
    return exchange -> {
        // Rate limit by IP address
        String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        return Mono.just(ip);
    };
}
```

### 2. Circuit Breaker

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://USER-SERVICE
          filters:
            - name: CircuitBreaker
              args:
                name: userServiceCircuitBreaker
                fallbackUri: forward:/fallback/user

resilience4j:
  circuitbreaker:
    instances:
      userServiceCircuitBreaker:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 3
```

**Fallback Controller**:

```java
@RestController
@RequestMapping("/fallback")
public class FallbackController {
    
    @GetMapping("/user")
    public ResponseEntity<String> userFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body("{\"error\":\"User service is temporarily unavailable\"}");
    }
}
```

### 3. Request Logging

```java
@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {
    
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        log.info("Incoming request: {} {} from {}",
            request.getMethod(),
            request.getPath(),
            request.getRemoteAddress()
        );
        
        long startTime = System.currentTimeMillis();
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            log.info("Request completed in {}ms with status {}",
                duration,
                exchange.getResponse().getStatusCode()
            );
        }));
    }
    
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
```

### 4. Custom Headers

```yaml
routes:
  - id: add-headers
    uri: lb://ORDER-SERVICE
    filters:
      - AddRequestHeader=X-Request-Source, API-Gateway
      - AddResponseHeader=X-Response-Time, ${responseTime}
      - RemoveRequestHeader=Cookie  # Remove sensitive headers
```

---

## Deployment

### Dockerfile

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/api-gateway-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose

```yaml
version: '3.8'

services:
  api-gateway:
    build: ./gateway-service
    ports:
      - "8080:8080"
    environment:
      - EUREKA_SERVER_URL=http://eureka-server:8761/eureka
      - REDIS_HOST=redis
      - JWT_SECRET_KEY=${JWT_SECRET_KEY}
    depends_on:
      - eureka-server
      - redis
    networks:
      - microservices-network

networks:
  microservices-network:
    driver: bridge
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api-gateway
spec:
  replicas: 3
  selector:
    matchLabels:
      app: api-gateway
  template:
    metadata:
      labels:
        app: api-gateway
    spec:
      containers:
      - name: api-gateway
        image: trading-platform/api-gateway:latest
        ports:
        - containerPort: 8080
        env:
        - name: EUREKA_SERVER_URL
          value: "http://eureka-server:8761/eureka"
        - name: REDIS_HOST
          value: "redis-service"
        - name: JWT_SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: jwt-secret
              key: secret-key
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
---
apiVersion: v1
kind: Service
metadata:
  name: api-gateway
spec:
  type: LoadBalancer
  selector:
    app: api-gateway
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
```

---

## Testing

### cURL Examples

```bash
# Test routing through gateway
curl http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test@123"}'

# Test with JWT
curl http://localhost:8080/user/profile \
  -H "Authorization: Bearer <your-jwt-token>"

# Test health endpoint
curl http://localhost:8080/actuator/health
```

### Gateway Routes Endpoint

```bash
# List all configured routes
curl http://localhost:8080/actuator/gateway/routes
```

---

## Quick Reference

| Feature | Configuration |
|---------|---------------|
| **Port** | 8080 (configurable) |
| **Service Discovery** | Eureka @ 8761 |
| **Rate Limiting** | Redis-based |
| **Circuit Breaker** | Resilience4j |
| **Health Check** | `/actuator/health` |
| **Routes Endpoint** | `/actuator/gateway/routes` |

---

## Additional Resources

- [Auth Service](./AUTH_SERVICE.md)
- [JWT Authentication](./JWT_AUTHENTICATION.md)
- [Security Best Practices](./SECURITY.md)
- [Quick Start Guide](./QUICK_START_GUIDE.md)

---

Last Updated: December 25, 2024
