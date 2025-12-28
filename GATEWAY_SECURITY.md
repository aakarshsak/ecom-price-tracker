# 🛡️ Securing Microservices: Gateway-Only Access

## Overview

This guide explains how to ensure that your microservices (auth-service, user-service, etc.) **only accept requests from the API Gateway** and reject direct access attempts.

---

## 🎯 Why This Matters

**Security Risk**: If services are directly accessible, attackers can:
- ❌ Bypass authentication/authorization at the gateway
- ❌ Access endpoints without JWT validation
- ❌ Perform DOS attacks on individual services
- ❌ Exploit service-specific vulnerabilities

**Solution**: Implement multiple layers of defense to ensure all traffic flows through the API Gateway.

---

## 🔐 Multi-Layer Security Approaches

### **Approach 1: Shared Secret Header** ⭐ Recommended for Quick Start

The API Gateway adds a secret header that downstream services validate.

#### **Security Level**: ⭐⭐⭐  
#### **Complexity**: Low  
#### **Use Case**: Development + Production (with proper secret management)

---

#### **Step 1: Gateway Adds Secret Header**

Create `services/api-gateway/src/main/java/com/sinha/ecom_system/api_gateway/config/GatewaySecurityConfig.java`:

```java
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

    @Value("${GATEWAY_SECRET:change-this-in-production}")
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
```

---

#### **Step 2: Services Validate Secret Header**

Create filter in **auth-service** and **user-service**:

**File**: `services/auth-service/src/main/java/com/sinha/ecom_system/auth_service/filter/GatewayAuthenticationFilter.java`

```java
package com.sinha.ecom_system.auth_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Gateway Authentication Filter
 * Ensures requests come only from API Gateway
 * Validates the X-Gateway-Secret header
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    @Value("${GATEWAY_SECRET:change-this-in-production}")
    private String expectedSecret;

    // Endpoints accessible directly (health checks, etc.)
    private static final List<String> BYPASS_PATHS = List.of(
        "/actuator/health",
        "/actuator/info",
        "/error"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Allow bypass for certain endpoints
        if (shouldBypass(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Validate gateway secret header
        String gatewaySecret = request.getHeader("X-Gateway-Secret");

        if (gatewaySecret == null || !gatewaySecret.equals(expectedSecret)) {
            log.warn("Unauthorized direct access attempt to: {} from IP: {}", 
                path, request.getRemoteAddr());
            
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Forbidden\"," +
                "\"message\":\"Direct access not allowed. Use API Gateway.\"," +
                "\"status\":403}"
            );
            return;
        }

        // Valid gateway request - proceed
        log.debug("Valid gateway request to: {}", path);
        filterChain.doFilter(request, response);
    }

    private boolean shouldBypass(String path) {
        return BYPASS_PATHS.stream().anyMatch(path::startsWith);
    }
}
```

**Repeat the same filter for user-service** (change package name accordingly).

---

#### **Step 3: Environment Configuration**

Add to `.env` file (or environment variables):

```bash
# Gateway Secret (MUST be the same across all services)
GATEWAY_SECRET=your-super-secret-gateway-token-change-in-production-minimum-32-chars-recommended
```

**Important**: 
- Use a strong, random string (minimum 32 characters)
- Keep it secret - don't commit to Git
- Use AWS Secrets Manager / Azure Key Vault in production

---

#### **Testing**

```bash
# ❌ Test 1: Direct access to auth-service (should fail with 403)
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"pass"}'

# Expected Response:
# {
#   "error": "Forbidden",
#   "message": "Direct access not allowed. Use API Gateway.",
#   "status": 403
# }

# ✅ Test 2: Via gateway (should work)
curl -X POST http://localhost:8765/v1/api/auth-service/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"pass"}'

# Expected: 200 OK with JWT tokens
```

---

### **Approach 2: Network Security** ⭐⭐⭐⭐⭐ Production Recommended

Isolate services at the network level so they're not publicly accessible.

#### **Security Level**: ⭐⭐⭐⭐⭐  
#### **Complexity**: Medium  
#### **Use Case**: Production (Docker/Kubernetes/Cloud)

---

#### **Docker Compose Setup**

**File**: `docker-compose.yml`

```yaml
version: '3.8'

networks:
  public:
    driver: bridge
  internal:
    driver: bridge
    internal: true  # No external access

services:
  # API Gateway - ONLY service exposed to outside
  api-gateway:
    build: ./services/api-gateway
    ports:
      - "8765:8765"  # ✅ Exposed to host/internet
    networks:
      - public
      - internal
    environment:
      - GATEWAY_SECRET=${GATEWAY_SECRET}
    depends_on:
      - eureka-server
      - redis

  # Auth Service - NOT exposed publicly
  auth-service:
    build: ./services/auth-service
    # ❌ NO ports exposed to host
    networks:
      - internal  # Only accessible within Docker network
    environment:
      - GATEWAY_SECRET=${GATEWAY_SECRET}
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/auth_db
    depends_on:
      - postgres
      - redis
      - eureka-server

  # User Service - NOT exposed publicly
  user-service:
    build: ./services/user-service
    # ❌ NO ports exposed to host
    networks:
      - internal
    environment:
      - GATEWAY_SECRET=${GATEWAY_SECRET}
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/user_db
    depends_on:
      - postgres
      - eureka-server

  # Eureka Server
  eureka-server:
    build: ./services/naming-server
    ports:
      - "8761:8761"  # Exposed for management console
    networks:
      - internal

  # PostgreSQL
  postgres:
    image: postgres:16
    networks:
      - internal  # Only accessible from services
    environment:
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data

  # Redis
  redis:
    image: redis:7-alpine
    networks:
      - internal
    command: redis-server --requirepass ${REDIS_PASSWORD}

volumes:
  postgres_data:
```

**Result**: Services can only be accessed from within the Docker network. External requests MUST go through the gateway.

---

#### **AWS VPC Setup**

```
┌──────────────────────────────────────────────────────┐
│                    VPC (10.0.0.0/16)                 │
│                                                      │
│  ┌────────────────────────────────────────────────┐ │
│  │         Public Subnet (10.0.1.0/24)            │ │
│  │                                                │ │
│  │  ┌──────────────────────────────────────────┐ │ │
│  │  │  Application Load Balancer (ALB)        │ │ │
│  │  │  Internet-facing                        │ │ │
│  │  │  Security Group: Allow 443 from 0.0.0.0│ │ │
│  │  └──────────────────┬───────────────────────┘ │ │
│  └─────────────────────┼─────────────────────────┘ │
│                        │                           │
│  ┌─────────────────────┼─────────────────────────┐ │
│  │    Private Subnet (10.0.2.0/24)              │ │
│  │                    │                          │ │
│  │  ┌─────────────────▼────────────┐             │ │
│  │  │  API Gateway (ECS/EKS)       │             │ │
│  │  │  Security Group:             │             │ │
│  │  │  - Inbound: Only from ALB    │             │ │
│  │  └─────────────┬──────────────┬─┘             │ │
│  │                │              │                │ │
│  │  ┌─────────────▼───┐  ┌───────▼──────────┐    │ │
│  │  │  Auth Service   │  │  User Service    │    │ │
│  │  │  Security Group:│  │  Security Group: │    │ │
│  │  │  Inbound: Only  │  │  Inbound: Only   │    │ │
│  │  │  from Gateway   │  │  from Gateway    │    │ │
│  │  └─────────────┬───┘  └───────┬──────────┘    │ │
│  │                │              │                │ │
│  │  ┌─────────────▼──────────────▼───────────┐   │ │
│  │  │  RDS PostgreSQL / ElastiCache Redis    │   │ │
│  │  │  Security Group: Only from services    │   │ │
│  │  └────────────────────────────────────────┘   │ │
│  └───────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────┘
```

**AWS Security Groups**:

```hcl
# Application Load Balancer
resource "aws_security_group" "alb" {
  name = "alb-sg"
  
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]  # Public access
  }
}

# API Gateway
resource "aws_security_group" "gateway" {
  name = "gateway-sg"
  
  ingress {
    from_port       = 8765
    to_port         = 8765
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]  # Only from ALB
  }
}

# Microservices (Auth, User, etc.)
resource "aws_security_group" "services" {
  name = "services-sg"
  
  ingress {
    from_port       = 8081
    to_port         = 8090
    protocol        = "tcp"
    security_groups = [aws_security_group.gateway.id]  # Only from Gateway
  }
}

# Database
resource "aws_security_group" "database" {
  name = "database-sg"
  
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.services.id]  # Only from Services
  }
}
```

---

### **Approach 3: IP Whitelisting**

Restrict services to only accept from localhost or specific IPs.

#### **Security Level**: ⭐⭐  
#### **Complexity**: Low  
#### **Use Case**: Local development only

---

**File**: `services/auth-service/src/main/java/com/sinha/ecom_system/auth_service/filter/IPWhitelistFilter.java`

```java
package com.sinha.ecom_system.auth_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * IP Whitelist Filter
 * Only allows requests from specific IP addresses
 * Useful for local development
 */
@Component
@Order(1)
@Slf4j
public class IPWhitelistFilter extends OncePerRequestFilter {

    @Value("${ALLOWED_IPS:127.0.0.1,::1,localhost}")
    private String allowedIPsConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) 
            throws ServletException, IOException {

        String clientIP = getClientIP(request);
        List<String> allowedIPs = Arrays.asList(allowedIPsConfig.split(","));

        if (!isAllowed(clientIP, allowedIPs)) {
            log.warn("Blocked request from unauthorized IP: {}", clientIP);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Access denied\",\"message\":\"IP " + clientIP + " not whitelisted\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }

    private boolean isAllowed(String ip, List<String> allowedIPs) {
        return allowedIPs.stream().anyMatch(allowed -> 
            ip.equals(allowed) || 
            ip.equals("127.0.0.1") || 
            ip.equals("0:0:0:0:0:0:0:1") ||
            ip.equals("::1")
        );
    }
}
```

---

### **Approach 4: Mutual TLS (mTLS)**

Services require valid certificates to communicate.

#### **Security Level**: ⭐⭐⭐⭐⭐  
#### **Complexity**: High  
#### **Use Case**: Enterprise production, high-security requirements

---

**Overview**:
- Gateway has a client certificate
- Services validate the certificate
- Encrypted and authenticated communication

**Implementation** (High-level):

1. **Generate Certificates**:
```bash
# Create CA
openssl genrsa -out ca-key.pem 4096
openssl req -new -x509 -days 365 -key ca-key.pem -out ca-cert.pem

# Create Gateway certificate
openssl genrsa -out gateway-key.pem 4096
openssl req -new -key gateway-key.pem -out gateway.csr
openssl x509 -req -days 365 -in gateway.csr -CA ca-cert.pem -CAkey ca-key.pem -out gateway-cert.pem

# Create Service certificates
openssl genrsa -out auth-service-key.pem 4096
openssl req -new -key auth-service-key.pem -out auth-service.csr
openssl x509 -req -days 365 -in auth-service.csr -CA ca-cert.pem -CAkey ca-key.pem -out auth-service-cert.pem
```

2. **Configure Gateway** (Spring Boot):
```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:gateway-keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

3. **Configure Services to Require Client Cert**:
```yaml
server:
  ssl:
    enabled: true
    client-auth: need  # Require client certificate
    trust-store: classpath:truststore.p12
    trust-store-password: ${TRUSTSTORE_PASSWORD}
```

**Note**: mTLS is complex but provides the highest security. Consider using a service mesh (Istio, Linkerd) for easier mTLS management.

---

## 📊 **Comparison Matrix**

| Approach | Security | Complexity | Dev | Docker | Cloud | Performance |
|----------|----------|------------|-----|--------|-------|-------------|
| **Shared Secret** | ⭐⭐⭐ | ⭐ | ✅ | ✅ | ✅ | ⭐⭐⭐⭐⭐ |
| **Network Security** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⚠️ | ✅ | ✅ | ⭐⭐⭐⭐⭐ |
| **IP Whitelist** | ⭐⭐ | ⭐ | ✅ | ❌ | ❌ | ⭐⭐⭐⭐ |
| **mTLS** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⚠️ | ⚠️ | ✅ | ⭐⭐⭐ |

---

## 🎯 **Recommended Implementation Strategy**

### **Phase 1: Immediate (Development)**
✅ Implement **Shared Secret Header**
- Quick to implement
- Works in all environments
- Good baseline security

### **Phase 2: Containerization**
✅ Add **Docker Network Isolation**
- Services in internal network
- Only gateway exposed
- Network-level security

### **Phase 3: Production**
✅ Implement **Cloud Security Groups / Network Policies**
- AWS Security Groups
- Kubernetes Network Policies
- Firewall rules

### **Phase 4: Enterprise (Optional)**
✅ Add **mTLS**
- Certificate-based authentication
- Encrypted service-to-service communication
- Use service mesh for easier management

---

## 🚀 **Quick Start: Implementing Shared Secret**

### **1. Add to API Gateway**

Create `GatewaySecurityConfig.java` in api-gateway (see code above).

### **2. Add to Each Service**

Create `GatewayAuthenticationFilter.java` in:
- `auth-service/filter/`
- `user-service/filter/`
- Any other services

### **3. Set Environment Variable**

```bash
# .env file (all services)
GATEWAY_SECRET=your-super-secret-32-char-minimum-token-here
```

### **4. Test**

```bash
# Direct access (should fail)
curl http://localhost:8081/auth/login
# Expected: 403 Forbidden

# Via gateway (should work)
curl http://localhost:8765/v1/api/auth-service/auth/login
# Expected: Works normally
```

---

## 🔒 **Security Best Practices**

1. **Never expose service ports publicly**
   - Only gateway port (8765) should be public
   - Other services on internal network only

2. **Use strong secrets**
   - Minimum 32 characters
   - Random, unpredictable
   - Rotate regularly

3. **Log suspicious activity**
   - Failed gateway secret validations
   - Direct access attempts
   - Unusual IP patterns

4. **Environment-specific configuration**
   - Different secrets per environment (dev/staging/prod)
   - Use secret management services (AWS Secrets Manager, etc.)

5. **Defense in depth**
   - Combine multiple approaches
   - Network isolation + Secret header + JWT validation

---

## 📝 **Monitoring & Alerts**

Set up alerts for:
- ⚠️ Failed gateway secret validations
- ⚠️ Direct access attempts to services
- ⚠️ Unusual traffic patterns
- ⚠️ Mismatched headers (X-User-Id vs JWT)

**Example Log Pattern**:
```
WARN: Unauthorized direct access attempt to: /auth/login from IP: 203.0.113.42
```

---

## 🐛 **Troubleshooting**

### **Issue: Services returning 403 even via Gateway**

**Cause**: Gateway secret mismatch

**Fix**:
```bash
# Ensure GATEWAY_SECRET is same across all services
echo $GATEWAY_SECRET  # Should be identical
```

### **Issue: Docker services can't communicate**

**Cause**: Not on same network

**Fix**:
```yaml
# docker-compose.yml
networks:
  internal:
    driver: bridge
```

### **Issue: Health checks failing**

**Cause**: Health endpoint blocked by gateway filter

**Fix**: Add `/actuator/health` to bypass list in `GatewayAuthenticationFilter`

---

## 📚 **Additional Resources**

- [Spring Cloud Gateway Documentation](https://spring.io/projects/spring-cloud-gateway)
- [AWS VPC Security Best Practices](https://docs.aws.amazon.com/vpc/latest/userguide/vpc-security-best-practices.html)
- [Docker Network Security](https://docs.docker.com/network/network-tutorial-standalone/)
- [Kubernetes Network Policies](https://kubernetes.io/docs/concepts/services-networking/network-policies/)

---

**Last Updated**: December 2024

