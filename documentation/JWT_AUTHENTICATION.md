# 🔑 JWT Authentication

Complete guide to JWT (JSON Web Token) authentication implementation in the Trading Platform.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Token Structure](#token-structure)
- [API Reference](#api-reference)
- [Implementation Details](#implementation-details)
- [Configuration](#configuration)
- [Security Features](#security-features)
- [Troubleshooting](#troubleshooting)

---

## Overview

The Auth Service uses **JWT (JSON Web Tokens)** for stateless authentication with the following token strategy:

- **Access Token**: Short-lived (15 minutes) - Used for API requests
- **Refresh Token**: Long-lived (7 days) - Used to get new access tokens

### Why JWT?

✅ **Stateless** - No server-side session storage  
✅ **Scalable** - Works across multiple servers  
✅ **Self-contained** - Contains user info, roles, permissions  
✅ **Secure** - Signed with HS256 algorithm  
✅ **Standard** - Industry-standard (RFC 7519)

---

## Architecture

### Token Flow Diagram

```
┌─────────────┐                                    ┌─────────────┐
│   Client    │                                    │ Auth Service│
└──────┬──────┘                                    └──────┬──────┘
       │                                                  │
       │  1. POST /auth/login                            │
       │  { email, password }                            │
       ├────────────────────────────────────────────────>│
       │                                                  │
       │  2. Validate credentials                        │
       │     Generate Access Token (15 min)              │
       │     Generate Refresh Token (7 days)             │
       │     Store refresh token hash in DB              │
       │                                                  │
       │  3. Return tokens                               │
       │<────────────────────────────────────────────────┤
       │  { accessToken, refreshToken, user }            │
       │                                                  │
       │  4. API Request with Access Token               │
       │  Authorization: Bearer <accessToken>            │
       ├────────────────────────────────────────────────>│
       │                                                  │
       │  5. JWT Filter validates token                  │
       │     Extract user ID, roles, permissions         │
       │     Set SecurityContext                         │
       │                                                  │
       │  6. Return response                             │
       │<────────────────────────────────────────────────┤
       │                                                  │
       │  [15 minutes later - access token expired]      │
       │                                                  │
       │  7. POST /auth/refresh                          │
       │  { refreshToken }                               │
       ├────────────────────────────────────────────────>│
       │                                                  │
       │  8. Validate refresh token                      │
       │     Check not revoked in DB                     │
       │     Generate new access token                   │
       │                                                  │
       │  9. Return new access token                     │
       │<────────────────────────────────────────────────┤
       │  { accessToken, refreshToken }                  │
```

### Component Flow

```
HTTP Request
    │
    ▼
JwtAuthenticationFilter
    ├─ Extract JWT from Authorization header
    ├─ Validate token (signature, expiry)
    ├─ Extract user ID, roles, permissions
    └─ Set SecurityContext
    │
    ▼
SecurityFilterChain
    ├─ Check endpoint permissions
    └─ Authorize request
    │
    ▼
AuthController
    └─ Process request
```

---

## Token Structure

### Access Token (15 minutes)

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "roles": ["ROLE_USER", "ROLE_TRADER"],
  "permissions": ["TRADE", "WITHDRAW", "VIEW_PORTFOLIO"],
  "type": "ACCESS",
  "iss": "trading-platform-auth-service",
  "iat": 1703275200,
  "exp": 1703276100,
  "jti": "token-id-uuid"
}
```

**Claims Explained:**
- `sub` - Subject (User ID)
- `email` - User's email
- `roles` - List of assigned roles
- `permissions` - List of permissions
- `type` - Token type (ACCESS or REFRESH)
- `iss` - Issuer (auth service)
- `iat` - Issued at (timestamp)
- `exp` - Expiration (timestamp)
- `jti` - JWT ID (for revocation)

### Refresh Token (7 days)

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "type": "REFRESH",
  "iss": "trading-platform-auth-service",
  "iat": 1703275200,
  "exp": 1703880000,
  "jti": "refresh-token-id-uuid"
}
```

**Storage**: Refresh token hash (SHA-256) is stored in database with device info.

---

## API Reference

### Register User

**Endpoint**: `POST /auth/register`  
**Auth Required**: No

**Request:**
```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "password": "SecurePass@123",
    "mobileNumber": "+1-555-123-4567",
    "dob": "1990-05-15"
  }'
```

**Response:**
```json
{
  "status": "success",
  "message": "Registration successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "email": "john@example.com",
      "roles": ["ROLE_USER"],
      "permissions": [],
      "isEmailVerified": false,
      "is2faEnabled": false
    },
    "requires2FA": false
  },
  "timestamp": "2024-12-25T10:30:00"
}
```

### Login

**Endpoint**: `POST /auth/login`  
**Auth Required**: No

**Request:**
```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePass@123"
  }'
```

**Response**: Same as register

### Refresh Token

**Endpoint**: `POST /auth/refresh`  
**Auth Required**: No

**Request:**
```bash
curl -X POST http://localhost:8081/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }'
```

**Response:**
```json
{
  "status": "success",
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs... (NEW)",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs... (SAME)",
    "tokenType": "Bearer",
    "expiresIn": 900
  },
  "timestamp": "2024-12-25T10:45:00"
}
```

### Access Protected Endpoint

**Endpoint**: `GET /auth/profile` (example)  
**Auth Required**: Yes

**Request:**
```bash
curl -X GET http://localhost:8081/auth/profile \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

### Logout

**Endpoint**: `POST /auth/logout`  
**Auth Required**: Yes

**Request:**
```bash
curl -X POST http://localhost:8081/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..." \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }'
```

**Response:**
```json
{
  "status": "success",
  "message": "Logout successful",
  "timestamp": "2024-12-25T11:00:00"
}
```

### Logout All Devices

**Endpoint**: `POST /auth/logout-all`  
**Auth Required**: Yes

**Request:**
```bash
curl -X POST http://localhost:8081/auth/logout-all \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIs..."
```

---

## Implementation Details

### Key Components

#### 1. JwtUtil.java

Handles all JWT operations:

```java
// Generate Access Token
String accessToken = jwtUtil.generateAccessToken(
    userId,      // User ID (UUID)
    email,       // User email
    roles,       // List of role names
    permissions  // List of permissions
);

// Validate Token
boolean isValid = jwtUtil.validateToken(token);

// Extract User ID
UUID userId = jwtUtil.getUserIdFromToken(token);

// Extract Roles
List<String> roles = jwtUtil.getRolesFromToken(token);
```

#### 2. JwtAuthenticationFilter.java

Spring Security filter that validates JWT on each request:

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) {
        // 1. Extract token from Authorization header
        String token = extractTokenFromRequest(request);
        
        // 2. Validate token
        if (token != null && jwtUtil.validateToken(token)) {
            // 3. Extract user details
            UUID userId = jwtUtil.getUserIdFromToken(token);
            List<String> roles = jwtUtil.getRolesFromToken(token);
            
            // 4. Set SecurityContext
            Authentication auth = createAuthentication(userId, roles);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        
        // 5. Continue filter chain
        filterChain.doFilter(request, response);
    }
}
```

**Skipped Endpoints** (no JWT validation):
- `/auth/register`
- `/auth/login`
- `/auth/refresh`
- `/actuator/health`
- `/error`

#### 3. RefreshToken Entity

Stores refresh tokens securely:

```java
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    private UUID id;
    private UUID userId;
    private String tokenHash;  // SHA-256 hash (NOT plain text)
    private String deviceInfo; // Device metadata
    private String ipAddress;
    private LocalDateTime expiresAt;
    private Boolean revoked;
    private LocalDateTime revokedAt;
    
    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
```

**Why hash tokens?** If database is compromised, attackers can't use tokens directly.

#### 4. SecurityConfig.java

Configures Spring Security with JWT filter:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .csrf(AbstractHttpConfigurer::disable)  // Disable CSRF for JWT
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/auth/register", "/auth/login").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthenticationFilter, 
                UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}
```

---

## Configuration

### Environment Variables

```properties
# JWT Configuration
JWT_SECRET_KEY=your-256-bit-secret-key-change-in-production
JWT_ACCESS_TOKEN_EXPIRY=900000        # 15 minutes (milliseconds)
JWT_REFRESH_TOKEN_EXPIRY=604800000    # 7 days (milliseconds)
JWT_ISSUER=trading-platform-auth-service
```

### Application YAML

```yaml
jwt:
  secret-key: ${JWT_SECRET_KEY}
  access-token-expiry: ${JWT_ACCESS_TOKEN_EXPIRY:900000}
  refresh-token-expiry: ${JWT_REFRESH_TOKEN_EXPIRY:604800000}
  issuer: ${JWT_ISSUER:trading-platform-auth-service}
```

### Database Migration

```sql
-- V2__create_refresh_tokens.sql
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    device_info     TEXT,
    ip_address      VARCHAR(45),
    expires_at      TIMESTAMP NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at      TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_token ON refresh_tokens(token_hash);
```

---

## Security Features

### 1. Token Signing

- **Algorithm**: HS256 (HMAC with SHA-256)
- **Secret Key**: 256-bit minimum
- **Signature**: Prevents token tampering

### 2. Token Hashing

- **Refresh Tokens**: Stored as SHA-256 hash
- **Protection**: Database compromise doesn't expose tokens

### 3. Token Expiry

- **Access Token**: 15 minutes (short-lived)
- **Refresh Token**: 7 days (can be revoked)

### 4. Token Revocation

- **Logout**: Marks refresh token as revoked in database
- **Logout All**: Revokes all user's refresh tokens
- **Cannot refresh**: After revocation

### 5. Account Lockout

- **Failed Attempts**: 5 consecutive failures
- **Lock Duration**: 15 minutes
- **Protection**: Prevents brute force attacks

### 6. Role-Based Access Control

- **Roles in Token**: No database lookup needed
- **Permissions in Token**: Fast authorization
- **Spring Security**: `@PreAuthorize` annotations

### 7. Stateless Authentication

- **No Sessions**: Fully stateless
- **Scalable**: Works across multiple servers
- **Load Balanced**: No session affinity needed

---

## Troubleshooting

### Issue: 401 Unauthorized

**Symptoms:**
```json
{
  "status": 401,
  "error": "Unauthorized"
}
```

**Possible Causes:**

1. **Missing Authorization header**
   ```bash
   # Wrong
   curl http://localhost:8081/auth/profile
   
   # Correct
   curl -H "Authorization: Bearer <token>" http://localhost:8081/auth/profile
   ```

2. **Token expired**
   - Access tokens expire after 15 minutes
   - Use `/auth/refresh` to get a new one

3. **Invalid token format**
   - Must be: `Bearer <token>` (note the space)

4. **JWT secret mismatch**
   - Check `JWT_SECRET_KEY` in environment
   - Must be at least 32 characters

### Issue: Token Refresh Fails

**Symptoms:**
```json
{
  "status": "error",
  "message": "Refresh token is invalid or expired"
}
```

**Possible Causes:**

1. **Token revoked (logged out)**
   - Login again to get new tokens

2. **Token expired**
   - Refresh tokens expire after 7 days
   - Login again

3. **Using access token instead of refresh token**
   - Make sure you're sending the refresh token

### Issue: Cannot Decode JWT

**Tool**: Use [jwt.io](https://jwt.io/) to decode and verify tokens

**Steps**:
1. Copy your JWT token
2. Paste into jwt.io
3. Check claims (sub, email, roles, exp)
4. Verify signature with secret key

### Issue: Database Connection Failed

**Check**:
```bash
psql -U postgres -d auth_db -c "SELECT COUNT(*) FROM refresh_tokens;"
```

**Solution**: Ensure PostgreSQL is running and database exists

### Issue: Signature Verification Failed

**Cause**: JWT secret key mismatch

**Solution**:
1. Check `JWT_SECRET_KEY` in `.env`
2. Restart service after changing secret
3. Old tokens will be invalid after secret change

---

## Best Practices

### Client-Side Token Storage

| Storage | Security | Pros | Cons |
|---------|----------|------|------|
| LocalStorage | ⚠️ Low | Persists across sessions | Vulnerable to XSS |
| SessionStorage | ⚠️ Low | Cleared on tab close | Vulnerable to XSS |
| HTTP-Only Cookie | ✅ High | Protected from XSS | Needs CSRF protection |
| Memory (state) | ✅ High | Most secure | Lost on page refresh |

**Recommendation**:
- **Access Token**: Store in memory (React state, Angular service)
- **Refresh Token**: HTTP-only cookie OR secure storage

### Token Rotation

For enhanced security, rotate refresh tokens:

```java
public TokenResponse refreshAccessToken(String refreshToken) {
    // 1. Validate old refresh token
    // 2. Generate new access token
    // 3. Generate new refresh token (rotation)
    // 4. Revoke old refresh token
    // 5. Return new tokens
}
```

### Monitoring

**Check active tokens:**
```sql
SELECT user_id, COUNT(*) as active_tokens
FROM refresh_tokens
WHERE revoked = false AND expires_at > NOW()
GROUP BY user_id;
```

**Clean up expired tokens:**
```sql
DELETE FROM refresh_tokens
WHERE expires_at < NOW() - INTERVAL '30 days';
```

---

## Testing Checklist

- [ ] Register new user
- [ ] Login with valid credentials
- [ ] Login with invalid credentials
- [ ] Verify account lockout after 5 failed attempts
- [ ] Access protected endpoint with valid JWT
- [ ] Access protected endpoint without JWT (should return 401)
- [ ] Refresh access token with valid refresh token
- [ ] Refresh with expired refresh token (should fail)
- [ ] Logout from single device
- [ ] Logout from all devices
- [ ] Verify refresh token is revoked after logout
- [ ] Test token expiry (wait 15 min for access token)

---

## Additional Resources

- **Auth Service Architecture**: [AUTH_SERVICE.md](./AUTH_SERVICE.md)
- **Security Best Practices**: [SECURITY.md](./SECURITY.md)
- **Quick Start Guide**: [QUICK_START_GUIDE.md](./QUICK_START_GUIDE.md)
- **JWT.io**: https://jwt.io/ - Decode and verify JWT tokens
- **RFC 7519**: https://tools.ietf.org/html/rfc7519 - JWT specification

---

Last Updated: December 25, 2024

