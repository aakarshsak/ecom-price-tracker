# 🚀 API Gateway - Quick Start Guide

## ⚡ Setup (5 Minutes)

### 1. Set Environment Variables

Create `.env` file in `services/api-gateway/`:

```env
JWT_SECRET_KEY=my-super-secret-256-bit-key-for-jwt-signing-change-in-production
JWT_ACCESS_TOKEN_EXPIRY=900000
JWT_REFRESH_TOKEN_EXPIRY=604800000
JWT_ISSUER=trading-platform

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://localhost:8761/eureka
SERVER_PORT=8765
```

### 2. Build & Run

```bash
cd services/api-gateway
mvn clean install
mvn spring-boot:run
```

### 3. Verify

```bash
curl http://localhost:8765/actuator/health
```

Expected:
```json
{"status":"UP"}
```

---

## 📡 API Testing

### Login (Get JWT Token)
```bash
curl -X POST http://localhost:8765/v1/api/auth-service/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@trading.com",
    "password": "Admin@123"
  }'
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "email": "admin@trading.com",
  "roles": ["ROLE_ADMIN", "ROLE_USER"]
}
```

### Access Protected Endpoint
```bash
curl -X GET http://localhost:8765/v1/api/user-service/users/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

---

## 🔧 Architecture Summary

```
Client → API Gateway → Validates JWT → Adds Headers → Routes to Service
                      ↓
                   Redis (check blacklist)
```

**Headers Added by Gateway:**
- `X-User-Id`: User UUID
- `X-User-Email`: User email
- `X-User-Roles`: Comma-separated roles
- `X-User-Permissions`: Comma-separated permissions

---

## 🛡️ Public Routes (No JWT)

- `/v1/api/auth-service/auth/login`
- `/v1/api/auth-service/auth/register`
- `/v1/api/auth-service/auth/refresh`
- `/actuator/health`

---

## ⚠️ Common Issues

### "Missing authorization token"
**Fix:** Add header: `Authorization: Bearer <token>`

### "Invalid or expired token"
**Fix:** Login again or use refresh token

### Port 8765 already in use
**Fix:** Change `SERVER_PORT` in .env or kill process on 8765

### Redis connection refused
**Fix:** Start Redis: `docker run -d -p 6379:6379 redis`

---

## 📊 Key Files

| File | Purpose |
|------|---------|
| `filter/JwtAuthenticationGlobalFilter.java` | Main JWT validation logic |
| `config/JwtConfig.java` | JWT configuration |
| `config/ReactiveRedisConfig.java` | Redis setup |
| `config/GatewayRouteConfig.java` | Service routing |
| `util/ReactiveJwtUtil.java` | JWT utilities |
| `service/ReactiveTokenBlacklistService.java` | Token blacklist check |

---

## ✅ Implementation Status

- ✅ Reactive JWT validation (GlobalFilter)
- ✅ Token blacklist checking (Redis)
- ✅ User context propagation (headers)
- ✅ Public endpoint bypass
- ✅ Configuration via @Configuration classes
- ✅ No YAML configuration files
- ✅ Error handling with JSON responses
- ✅ Service discovery via Eureka
- ✅ Path rewriting
- ✅ Load balancing

---

**Ready to use! 🎉**

