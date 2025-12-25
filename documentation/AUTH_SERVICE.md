# 🔐 Authentication Service Architecture

Complete guide to the Auth Service design, responsibilities, and implementation.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Why Separate Auth and User Services?](#why-separate-auth-and-user-services)
- [Auth Service Responsibilities](#auth-service-responsibilities)
- [User Service Responsibilities](#user-service-responsibilities)
- [Database Schemas](#database-schemas)
- [Service Communication](#service-communication)
- [Implementation Status](#implementation-status)

---

## Overview

The Trading Platform follows microservices best practices by separating **authentication/authorization** (Auth Service) from **user profile management** (User Service).

### Key Principle: Single Responsibility

- **Auth Service**: "Who are you? Can you access this?"  
- **User Service**: "What do we know about this user?"

---

## Why Separate Auth and User Services?

### Comparison Matrix

| Concern | Auth Service | User Service |
|---------|-------------|--------------|
| **Primary Goal** | Security & Authentication | User Data Management |
| **Scaling Need** | High during login peaks | Moderate, steady |
| **Update Frequency** | Rarely (security is stable) | Often (business features) |
| **Team Ownership** | Security/Platform Team | Product/Feature Team |
| **Failure Impact** | Can't login, but app works | Can't update profile |
| **Data Sensitivity** | Highly sensitive (passwords, tokens) | Sensitive (PII) |
| **Cache Strategy** | Heavy Redis usage | Moderate caching |
| **Database Size** | Smaller, slower growth | Larger, faster growth |

### Security Isolation

```
┌─────────────────────────────────────────────┐
│          SECURITY BOUNDARY                  │
│                                             │
│  Auth DB Breach → Passwords compromised    │
│  User DB Breach → Profile data exposed     │
│                                             │
│  ✅ NOT BOTH AT ONCE!                       │
└─────────────────────────────────────────────┘
```

**Key Benefit**: If one database is compromised, the other remains secure.

---

## Auth Service Responsibilities

### ✅ Core Features

```
✅ Authentication (Login/Logout)
✅ JWT Token Generation & Validation
✅ Refresh Token Management
✅ Password Management (Hash, Reset, Change)
✅ Two-Factor Authentication (2FA/MFA)
✅ Session Management
✅ Rate Limiting (Login attempts)
✅ Token Blacklisting
✅ OAuth/SSO Integration (Google, Facebook)
✅ Security Events Logging
✅ Account Lockout Protection
✅ Role & Permission Management (Authorization)
```

### ❌ What Auth Service Doesn't Do

```
❌ Store user profiles (name, address, preferences)
❌ KYC verification
❌ User business logic
❌ User settings/preferences
❌ User statistics/analytics
❌ User banking information
❌ User watchlists
```

### Tech Stack

| Component | Technology |
|-----------|-----------|
| **Framework** | Spring Boot 3.5.9 |
| **Security** | Spring Security 6.x |
| **Database** | PostgreSQL 14+ (`auth_db`) |
| **Cache** | Redis (Sessions, Tokens, Rate Limiting) |
| **JWT** | JJWT 0.12.3 (HS256) |
| **Password Hashing** | BCrypt (strength 12) |
| **Port** | 8081 |

### API Endpoints

| Endpoint | Method | Auth Required | Description |
|----------|--------|---------------|-------------|
| `/auth/register` | POST | No | Register new user |
| `/auth/login` | POST | No | Login and get JWT tokens |
| `/auth/refresh` | POST | No | Refresh access token |
| `/auth/logout` | POST | Yes | Logout from current device |
| `/auth/logout-all` | POST | Yes | Logout from all devices |
| `/auth/change-password` | POST | Yes | Change password |
| `/auth/forgot-password` | POST | No | Request password reset |
| `/auth/reset-password` | POST | No | Reset password with token |
| `/auth/verify-email` | POST | No | Verify email address |
| `/auth/enable-2fa` | POST | Yes | Enable 2FA |
| `/auth/verify-2fa` | POST | Yes | Verify 2FA code |

---

## User Service Responsibilities

### ✅ Core Features

```
✅ User Profile Management (CRUD)
✅ KYC Document Upload & Verification
✅ Trading Preferences (risk tolerance, default order type)
✅ Notification Preferences (email, SMS, push)
✅ Watchlists Management
✅ Trading Account Management
✅ Bank Account Linking
✅ User Settings & Customization
✅ Profile Picture Upload
✅ Contact Information Management
✅ User Activity Analytics
✅ Document Storage (PAN, Aadhaar, etc.)
```

### ❌ What User Service Doesn't Do

```
❌ Authentication or password management
❌ JWT token generation/validation
❌ Session management
❌ Role/permission management
❌ Security event logging
```

### Tech Stack

| Component | Technology |
|-----------|-----------|
| **Framework** | Spring Boot 3.5.9 |
| **Database** | PostgreSQL 14+ (`user_db`) |
| **Storage** | AWS S3 (for documents) |
| **Cache** | Redis (user profiles) |
| **Port** | 8082 |
| **Status** | ⏳ Planned (Not yet implemented) |

---

## Database Schemas

### Auth Service Database (`auth_db`)

#### 1. auth_credentials Table

```sql
CREATE TABLE auth_credentials (
    user_id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                VARCHAR(255) NOT NULL UNIQUE,
    password_hash        VARCHAR(255) NOT NULL,
    is_email_verified    BOOLEAN DEFAULT FALSE,
    is_phone_verified    BOOLEAN DEFAULT FALSE,
    phone_number         VARCHAR(20),
    is_2fa_enabled       BOOLEAN DEFAULT FALSE,
    totp_secret          VARCHAR(255),
    backup_codes         TEXT,
    failed_attempts      INTEGER DEFAULT 0,
    locked_until         TIMESTAMP,
    last_login           TIMESTAMP,
    password_changed_at  TIMESTAMP,
    created_at           TIMESTAMP DEFAULT NOW(),
    updated_at           TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_auth_email ON auth_credentials(email);
CREATE INDEX idx_auth_phone ON auth_credentials(phone_number);
```

#### 2. roles Table

```sql
CREATE TABLE roles (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(50) NOT NULL UNIQUE,
    description  TEXT,
    permissions  JSONB,
    is_active    BOOLEAN DEFAULT TRUE,
    created_at   TIMESTAMP DEFAULT NOW(),
    updated_at   TIMESTAMP DEFAULT NOW()
);

-- Default roles
INSERT INTO roles (name, description, permissions) VALUES
('ROLE_USER', 'Default user role', '["READ_MARKET_DATA", "VIEW_PORTFOLIO"]'),
('ROLE_TRADER', 'Active trader role', '["READ_MARKET_DATA", "PLACE_ORDER", "VIEW_PORTFOLIO", "WITHDRAW"]'),
('ROLE_ADMIN', 'Administrator role', '["*"]');
```

#### 3. user_roles Table (Join Table)

```sql
CREATE TABLE user_roles (
    user_id     UUID NOT NULL REFERENCES auth_credentials(user_id) ON DELETE CASCADE,
    role_id     UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    granted_by  UUID,
    granted_at  TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);
```

#### 4. refresh_tokens Table

```sql
CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES auth_credentials(user_id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    device_info     TEXT,
    ip_address      VARCHAR(45),
    expires_at      TIMESTAMP NOT NULL,
    revoked         BOOLEAN DEFAULT FALSE,
    revoked_at      TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_token ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_expires ON refresh_tokens(expires_at);
```

### User Service Database (`user_db`) - Planned

```sql
CREATE TABLE user_profiles (
    user_id          UUID PRIMARY KEY,  -- Same as auth_credentials.user_id
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    middle_name      VARCHAR(100),
    date_of_birth    DATE NOT NULL,
    gender           VARCHAR(20),
    nationality      VARCHAR(50),
    profile_picture  VARCHAR(500),
    bio              TEXT,
    created_at       TIMESTAMP DEFAULT NOW(),
    updated_at       TIMESTAMP DEFAULT NOW()
);

CREATE TABLE user_contact_info (
    user_id          UUID PRIMARY KEY REFERENCES user_profiles(user_id),
    mobile_number    VARCHAR(20),
    alternate_mobile VARCHAR(20),
    address_line1    VARCHAR(255),
    address_line2    VARCHAR(255),
    city             VARCHAR(100),
    state            VARCHAR(100),
    postal_code      VARCHAR(20),
    country          VARCHAR(100)
);

CREATE TABLE user_kyc (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL REFERENCES user_profiles(user_id),
    document_type    VARCHAR(50) NOT NULL,  -- PAN, AADHAAR, PASSPORT
    document_number  VARCHAR(100) NOT NULL,
    document_url     VARCHAR(500),
    verification_status VARCHAR(20) DEFAULT 'PENDING',
    verified_at      TIMESTAMP,
    verified_by      UUID,
    created_at       TIMESTAMP DEFAULT NOW()
);

CREATE TABLE trading_preferences (
    user_id          UUID PRIMARY KEY REFERENCES user_profiles(user_id),
    default_order_type VARCHAR(20) DEFAULT 'LIMIT',
    risk_tolerance   VARCHAR(20) DEFAULT 'MODERATE',
    auto_square_off  BOOLEAN DEFAULT FALSE,
    max_loss_per_trade DECIMAL(19,2),
    notification_email BOOLEAN DEFAULT TRUE,
    notification_sms   BOOLEAN DEFAULT TRUE,
    notification_push  BOOLEAN DEFAULT TRUE
);
```

---

## Service Communication

### Synchronous Communication (REST)

**Auth Service ← User Service**

```
┌──────────────┐           ┌──────────────┐
│ User Service │  ───────► │ Auth Service │
└──────────────┘           └──────────────┘
    
User Service calls Auth Service for:
- Validate JWT token
- Check user permissions
- Verify user exists
```

**Example: User Profile Update**

```java
// In User Service
public void updateProfile(UUID userId, UpdateRequest request) {
    // 1. Call Auth Service to verify user exists
    boolean exists = authServiceClient.userExists(userId);
    
    if (!exists) {
        throw new UserNotFoundException();
    }
    
    // 2. Update user profile
    userRepository.save(request);
}
```

### Asynchronous Communication (Kafka)

**Auth Service → User Service**

```
Auth Service publishes events:
- UserRegistered
- EmailVerified  
- AccountLocked
- PasswordChanged

User Service consumes and:
- Creates user profile
- Updates verification status
- Sends notifications
```

**Example: User Registration Flow**

```
1. Client → Auth Service: Register
2. Auth Service: Create auth_credentials
3. Auth Service → Kafka: UserRegisteredEvent
4. User Service ← Kafka: Consume event
5. User Service: Create user_profile
6. User Service → Notification Service: Send welcome email
```

---

## Implementation Status

### ✅ Implemented (Auth Service)

- [x] User registration with JWT
- [x] Login with JWT (access + refresh tokens)
- [x] Token refresh flow
- [x] Logout (single & all devices)
- [x] Password hashing (BCrypt)
- [x] Account lockout (5 failed attempts)
- [x] Role-based access control
- [x] Refresh token management (with revocation)
- [x] Security filter (JWT validation)
- [x] Database schema (Flyway migrations)

### ⏳ Planned (Auth Service)

- [ ] Email verification
- [ ] Two-Factor Authentication (TOTP)
- [ ] Password reset flow
- [ ] OAuth/SSO integration (Google, GitHub)
- [ ] Rate limiting (Redis-based)
- [ ] Token blacklisting (Redis)
- [ ] Audit logging
- [ ] Device management

### 📝 Not Started (User Service)

- [ ] User profile CRUD
- [ ] KYC document upload
- [ ] KYC verification workflow
- [ ] Trading preferences
- [ ] Notification preferences
- [ ] Watchlist management
- [ ] Bank account linking
- [ ] Document storage (S3)
- [ ] User analytics

---

## Quick Links

- **JWT Implementation**: [JWT_AUTHENTICATION.md](./JWT_AUTHENTICATION.md)
- **Security Best Practices**: [SECURITY.md](./SECURITY.md)
- **API Gateway Setup**: [API_GATEWAY.md](./API_GATEWAY.md)
- **Quick Start**: [QUICK_START_GUIDE.md](./QUICK_START_GUIDE.md)

---

Last Updated: December 25, 2024

