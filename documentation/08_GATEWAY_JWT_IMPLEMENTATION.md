# API Gateway - JWT Authentication Implementation

## 🏗️ Architecture

This API Gateway uses **Spring Cloud Gateway** (reactive/WebFlux) with global JWT authentication.

```
┌────────────────────────────────────────────────────────────────┐
│                       CLIENT REQUEST                           │
│                Authorization: Bearer <JWT>                     │
└──────────────────────┬─────────────────────────────────────────┘
                       │
                       ▼
┌────────────────────────────────────────────────────────────────┐
│                   API GATEWAY (Port 8765)                      │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  JwtAuthenticationGlobalFilter (Order: -100)             │ │
│  │                                                          │ │
│  │  1. Extract JWT from Authorization header               │ │
│  │  2. Validate signature & expiration                     │ │
│  │  3. Check token type (ACCESS only)                      │ │
│  │  4. Check Redis blacklist (reactive)                    │ │
│  │  5. Extract user context (userId, email, roles)         │ │
│  │  6. Add headers: X-User-Id, X-User-Email, X-User-Roles  │ │
│  └──────────────────────────────────────────────────────────┘ │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  GatewayRouteConfig                                      │ │
│  │  - /v1/api/auth-service/** → AUTH-SERVICE               │ │
│  │  - /v1/api/user-service/** → USER-SERVICE               │ │
│  └──────────────────────────────────────────────────────────┘ │
└──────────────────────┬─────────────────────────────────────────┘
                       │
         ┌─────────────┴─────────────┐
         ▼                           ▼
┌──────────────────┐      ┌──────────────────┐
│  AUTH-SERVICE    │      │  USER-SERVICE    │
│                  │      │                  │
│  Trusts headers: │      │  Trusts headers: │
│  X-User-Id       │      │  X-User-Id       │
│  X-User-Email    │      │  X-User-Email    │
│  X-User-Roles    │      │  X-User-Roles    │
└──────────────────┘      └──────────────────┘
```

## 📦 Components

### 1. **JwtConfig** (Configuration)
- Reads JWT configuration from environment variables
- Uses `@Value` annotations (no YAML needed)
- Properties: secretKey, accessTokenExpiry, refreshTokenExpiry, issuer

### 2. **ReactiveRedisConfig** (Configuration)
- Configures reactive Redis connection
- Creates `ReactiveRedisTemplate` for non-blocking operations
- Reads Redis connection from environment variables

### 3. **ReactiveJwtUtil** (Utility)
- Validates JWT tokens
- Extracts claims (userId, email, roles, permissions)
- Pure JWT logic (no I/O operations)

### 4. **ReactiveTokenBlacklistService** (Service)
- Checks if tokens are blacklisted in Redis
- Uses reactive operations (Mono<Boolean>)
- Non-blocking Redis calls

### 5. **JwtAuthenticationGlobalFilter** (Filter)
- Implements `GlobalFilter` (runs on every request)
- Order: -100 (runs early in filter chain)
- Validates JWT and adds user context headers
- Returns reactive Mono<Void>

### 6. **GatewayRouteConfig** (Configuration)
- Defines routes to microservices
- Path rewriting rules
- Load balancing via Eureka (lb://)

## 🔧 Configuration (Environment Variables)

Set these environment variables (or use `.env` file):

```bash
# JWT Configuration
JWT_SECRET_KEY=your-super-secret-256-bit-minimum-key-for-production
JWT_ACCESS_TOKEN_EXPIRY=900000          # 15 minutes (milliseconds)
JWT_REFRESH_TOKEN_EXPIRY=604800000      # 7 days (milliseconds)
JWT_ISSUER=trading-platform

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=                         # Leave empty if no password

# Eureka Configuration
EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka

# Server Configuration
SERVER_PORT=8765
```

## 🚀 Running the Gateway

### Prerequisites
1. Redis running on localhost:6379
2. Eureka server running on localhost:8761
3. Environment variables configured

### Start the Gateway
```bash
cd services/api-gateway
mvn clean install
mvn spring-boot:run
```

## 🔐 Public Endpoints (No JWT Required)

The following endpoints bypass JWT validation:
- `/v1/api/auth-service/auth/login`
- `/v1/api/auth-service/auth/register`
- `/v1/api/auth-service/auth/refresh`
- `/v1/api/auth-service/auth/forgot-password`
- `/v1/api/auth-service/auth/reset-password`
- `/v1/api/auth-service/auth/verify-email`
- `/actuator/health`
- `/actuator/info`

## 🛡️ Protected Endpoints (JWT Required)

All other endpoints require a valid JWT token in the Authorization header:

```bash
Authorization: Bearer <your-jwt-token>
```

## 📡 Request Flow Example

### 1. Login (Public)
```bash
curl -X POST http://localhost:8765/v1/api/auth-service/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'
```

Response:
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "email": "user@example.com"
}
```

### 2. Access Protected Endpoint
```bash
curl -X GET http://localhost:8765/v1/api/user-service/users/profile \
  -H "Authorization: Bearer eyJhbGc..."
```

Gateway adds these headers before routing to user-service:
```
X-User-Id: 123e4567-e89b-12d3-a456-426614174000
X-User-Email: user@example.com
X-User-Roles: ROLE_USER,ROLE_TRADER
X-User-Permissions: READ,TRADE,WITHDRAW
```

## 🔍 How Downstream Services Use Headers

User-Service can access user context without JWT validation:

```java
@GetMapping("/users/profile")
public ResponseEntity<UserProfile> getProfile(
    @RequestHeader("X-User-Id") UUID userId,
    @RequestHeader("X-User-Email") String email,
    @RequestHeader("X-User-Roles") String roles) {
    
    // userId is already validated by gateway
    return userService.getProfile(userId);
}
```

## ⚠️ Security Considerations

1. **Network Isolation**: Ensure downstream services are NOT publicly accessible
2. **Header Trust**: Services should ONLY trust headers from the gateway
3. **Token Blacklist**: Logout adds tokens to Redis blacklist
4. **Token Types**: Only ACCESS tokens are accepted (not REFRESH)
5. **Reactive Pattern**: All operations are non-blocking

## 🐛 Troubleshooting

### Error: "Missing authorization token"
- **Cause**: No Authorization header or doesn't start with "Bearer "
- **Fix**: Add header: `Authorization: Bearer <token>`

### Error: "Invalid or expired token"
- **Cause**: Token signature invalid or expired
- **Fix**: Get a new token via login or refresh endpoint

### Error: "Token has been revoked"
- **Cause**: Token was blacklisted after logout
- **Fix**: Login again to get a new token

### Error: "Invalid token type"
- **Cause**: Using REFRESH token for API access
- **Fix**: Use ACCESS token only

## 📊 Monitoring

Check gateway health:
```bash
curl http://localhost:8765/actuator/health
```

## 🎯 Benefits of This Architecture

1. ✅ **Centralized Authentication** - JWT validated once at gateway
2. ✅ **Performance** - Reactive/non-blocking (handles high concurrency)
3. ✅ **Simplified Services** - Downstream services trust gateway headers
4. ✅ **Security** - Token blacklist prevents reuse after logout
5. ✅ **Scalability** - Stateless design, scales horizontally
6. ✅ **Maintainability** - Single place to update JWT logic

---

**Note**: This implementation uses @Configuration classes with @Value annotations instead of YAML configuration files, as requested.

