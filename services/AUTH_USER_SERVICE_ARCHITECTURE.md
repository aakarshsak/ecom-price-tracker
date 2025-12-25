# 🏗️ Auth Service vs User Service: Architecture Guide

## 📋 Table of Contents
- [Overview](#overview)
- [Why Separate Services?](#why-separate-services)
- [Service Responsibilities](#service-responsibilities)
- [Database Schemas](#database-schemas)
- [Service Communication](#service-communication)
- [Benefits of Separation](#benefits-of-separation)
- [Implementation Guide](#implementation-guide)
- [When to Combine Services](#when-to-combine-services)
- [Real-World Examples](#real-world-examples)

---

## Overview

This document explains the architectural decision to separate **Authentication/Authorization** concerns from **User Profile Management** in our microservices trading platform.

### Key Principle: Single Responsibility Principle (SRP)

Each service has a single, well-defined purpose:
- **Auth Service**: "Who are you? Can you access this?"
- **User Service**: "What do we know about this user?"

---

## Why Separate Services?

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

---

## Service Responsibilities

## 🔐 Auth Service

### Core Responsibilities
```
✅ Authentication (Login/Logout)
✅ JWT Token Generation & Validation
✅ Refresh Token Management
✅ Password Management (Hash, Reset, Change)
✅ Two-Factor Authentication (2FA/MFA)
✅ Session Management
✅ Rate Limiting (Login attempts)
✅ Token Blacklisting
✅ OAuth/SSO Integration (Google, Facebook, etc.)
✅ Security Events Logging
✅ Account Lockout Protection
✅ Role & Permission Management (Authorization)
```

### What Auth Service DOESN'T Do
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
- **Database**: PostgreSQL (`auth_db`)
- **Cache**: Redis (Sessions, Tokens, Rate Limiting)
- **Security**: Spring Security, BCrypt, JWT (RS256)
- **Port**: 8081

---

## 👤 User Service

### Core Responsibilities
```
✅ User Profile Management (CRUD)
✅ KYC Document Management
✅ User Preferences/Settings
✅ User Address Management
✅ User Status Management (Active/Inactive/Suspended)
✅ User Analytics/Statistics
✅ User Search & Filtering
✅ User Business Roles Assignment
✅ User Notification Preferences
✅ User Portfolio/Trading Preferences
✅ User Bank Account Management
✅ User Watchlist Management
```

### What User Service DOESN'T Do
```
❌ Password hashing/validation
❌ Token generation/validation
❌ Authentication logic
❌ Security decisions
❌ Login/Logout handling
❌ 2FA implementation
```

### Tech Stack
- **Database**: PostgreSQL (`user_db`)
- **Cache**: Redis (Profile caching)
- **Storage**: S3 (KYC documents, profile pictures)
- **Port**: 8082

---

## Database Schemas

## 🗄️ Auth Service Database (`auth_db`)

### 1. AUTH_CREDENTIALS (Core Authentication)

```sql
CREATE TABLE auth_credentials (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL UNIQUE,  -- FK to User Service
    email               VARCHAR(255) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,  -- BCrypt hash
    salt                VARCHAR(255),           -- Optional additional salt
    
    -- Account Security
    is_email_verified   BOOLEAN DEFAULT FALSE,
    is_phone_verified   BOOLEAN DEFAULT FALSE,
    is_2fa_enabled      BOOLEAN DEFAULT FALSE,
    totp_secret         VARCHAR(255),           -- Google Authenticator
    
    -- Account Protection
    failed_attempts     INT DEFAULT 0,
    locked_until        TIMESTAMP,
    last_login          TIMESTAMP,
    last_password_change TIMESTAMP DEFAULT NOW(),
    
    -- Metadata
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW(),
    created_by          UUID
);

CREATE INDEX idx_auth_email ON auth_credentials(email);
CREATE INDEX idx_auth_user_id ON auth_credentials(user_id);
```

### 2. ROLES (Authorization Roles)

```sql
CREATE TABLE roles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(50) NOT NULL UNIQUE,  -- ROLE_ADMIN, ROLE_TRADER, ROLE_USER
    description     VARCHAR(255),
    permissions     JSONB,  -- {"canTrade": true, "canWithdraw": true}
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_role_name ON roles(name);

-- Sample Data
INSERT INTO roles (name, description, permissions) VALUES
('ROLE_ADMIN', 'System Administrator', '{"canTrade": true, "canWithdraw": true, "canManageUsers": true}'),
('ROLE_TRADER', 'Active Trader', '{"canTrade": true, "canWithdraw": true, "canManageUsers": false}'),
('ROLE_USER', 'Basic User', '{"canTrade": false, "canWithdraw": false, "canManageUsers": false}');
```

### 3. USER_ROLES (Many-to-Many)

```sql
CREATE TABLE user_roles (
    user_id         UUID NOT NULL,
    role_id         UUID NOT NULL,
    granted_at      TIMESTAMP DEFAULT NOW(),
    granted_by      UUID,
    expires_at      TIMESTAMP,  -- Optional: Temporary roles
    is_active       BOOLEAN DEFAULT TRUE,
    
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES auth_credentials(user_id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE INDEX idx_user_roles_user ON user_roles(user_id);
```

### 4. REFRESH_TOKENS (Backup - Primary in Redis)

```sql
CREATE TABLE refresh_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,  -- Store hash, not plain
    device_info     JSONB,  -- {"device": "iPhone 13", "ip": "192.168.1.1"}
    expires_at      TIMESTAMP NOT NULL,
    revoked         BOOLEAN DEFAULT FALSE,
    revoked_at      TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES auth_credentials(user_id)
);

CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_token ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_expires ON refresh_tokens(expires_at);
```

### 5. PASSWORD_RESET_TOKENS

```sql
CREATE TABLE password_reset_tokens (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    token_hash      VARCHAR(255) NOT NULL UNIQUE,
    expires_at      TIMESTAMP NOT NULL,
    used            BOOLEAN DEFAULT FALSE,
    used_at         TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW(),
    ip_address      VARCHAR(45),
    
    CONSTRAINT fk_reset_user FOREIGN KEY (user_id) REFERENCES auth_credentials(user_id)
);

CREATE INDEX idx_reset_token ON password_reset_tokens(token_hash);
CREATE INDEX idx_reset_expires ON password_reset_tokens(expires_at);
```

### 6. LOGIN_HISTORY (Audit & Security)

```sql
CREATE TABLE login_history (
    id              BIGSERIAL PRIMARY KEY,
    user_id         UUID NOT NULL,
    login_time      TIMESTAMP DEFAULT NOW(),
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    device_info     JSONB,
    location        JSONB,  -- {"city": "Mumbai", "country": "India"}
    login_method    VARCHAR(50),  -- PASSWORD, 2FA, OAUTH, SSO
    status          VARCHAR(20),  -- SUCCESS, FAILED, BLOCKED
    failure_reason  VARCHAR(255),
    
    CONSTRAINT fk_login_user FOREIGN KEY (user_id) REFERENCES auth_credentials(user_id)
);

CREATE INDEX idx_login_user ON login_history(user_id);
CREATE INDEX idx_login_time ON login_history(login_time DESC);
CREATE INDEX idx_login_status ON login_history(status);
```

### 7. SECURITY_EVENTS (Audit Trail)

```sql
CREATE TABLE security_events (
    id              BIGSERIAL PRIMARY KEY,
    user_id         UUID,
    event_type      VARCHAR(50) NOT NULL,  -- PASSWORD_CHANGE, 2FA_ENABLED, ROLE_CHANGED
    event_data      JSONB,
    ip_address      VARCHAR(45),
    created_at      TIMESTAMP DEFAULT NOW(),
    severity        VARCHAR(20)  -- INFO, WARNING, CRITICAL
);

CREATE INDEX idx_security_user ON security_events(user_id);
CREATE INDEX idx_security_type ON security_events(event_type);
CREATE INDEX idx_security_time ON security_events(created_at DESC);
CREATE INDEX idx_security_severity ON security_events(severity);
```

### 8. OAUTH_PROVIDERS (Optional: For SSO)

```sql
CREATE TABLE oauth_providers (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    provider        VARCHAR(50) NOT NULL,  -- GOOGLE, FACEBOOK, APPLE
    provider_user_id VARCHAR(255) NOT NULL,
    access_token    TEXT,
    refresh_token   TEXT,
    expires_at      TIMESTAMP,
    profile_data    JSONB,
    created_at      TIMESTAMP DEFAULT NOW(),
    updated_at      TIMESTAMP DEFAULT NOW(),
    
    UNIQUE(provider, provider_user_id),
    CONSTRAINT fk_oauth_user FOREIGN KEY (user_id) REFERENCES auth_credentials(user_id)
);

CREATE INDEX idx_oauth_user ON oauth_providers(user_id);
CREATE INDEX idx_oauth_provider ON oauth_providers(provider);
```

---

## 🗄️ User Service Database (`user_db`)

### 1. USERS (Core User Profile)

```sql
CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Basic Info
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    email               VARCHAR(255) NOT NULL UNIQUE,  -- Duplicate from auth
    mobile_number       VARCHAR(20),
    
    -- Personal Info
    dob                 DATE,
    gender              VARCHAR(20),
    nationality         VARCHAR(50),
    
    -- Status
    user_status         VARCHAR(20) DEFAULT 'ACTIVE',  -- ACTIVE, INACTIVE, SUSPENDED, DELETED
    account_type        VARCHAR(50) DEFAULT 'RETAIL',  -- RETAIL, INSTITUTIONAL, VIP
    
    -- KYC Status
    kyc_status          VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, VERIFIED, REJECTED, EXPIRED
    kyc_verified_at     TIMESTAMP,
    kyc_verified_by     UUID,
    
    -- Trading Specific
    trading_status      VARCHAR(20) DEFAULT 'RESTRICTED',  -- RESTRICTED, ENABLED, SUSPENDED
    risk_profile        VARCHAR(20),  -- CONSERVATIVE, MODERATE, AGGRESSIVE
    
    -- Metadata
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW(),
    last_active_at      TIMESTAMP,
    deleted_at          TIMESTAMP  -- Soft delete
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(user_status);
CREATE INDEX idx_users_kyc ON users(kyc_status);
CREATE INDEX idx_users_trading ON users(trading_status);
CREATE INDEX idx_users_created ON users(created_at DESC);
```

### 2. USER_PROFILES (Extended Profile)

```sql
CREATE TABLE user_profiles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL UNIQUE,
    
    -- Professional Info
    occupation          VARCHAR(100),
    annual_income       VARCHAR(50),
    source_of_funds     VARCHAR(100),
    employer_name       VARCHAR(200),
    
    -- Additional Info
    profile_picture_url TEXT,
    bio                 TEXT,
    referral_code       VARCHAR(20) UNIQUE,
    referred_by         UUID,
    
    -- Communication Preferences
    language_preference VARCHAR(10) DEFAULT 'en',
    timezone            VARCHAR(50) DEFAULT 'UTC',
    
    -- Metadata
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_profile_referral ON user_profiles(referral_code);
```

### 3. USER_ADDRESSES

```sql
CREATE TABLE user_addresses (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    address_type        VARCHAR(20) NOT NULL,  -- PERMANENT, CURRENT, BILLING
    
    -- Address Details
    address_line1       VARCHAR(255) NOT NULL,
    address_line2       VARCHAR(255),
    city                VARCHAR(100) NOT NULL,
    state               VARCHAR(100),
    postal_code         VARCHAR(20),
    country             VARCHAR(50) NOT NULL,
    
    -- Verification
    is_verified         BOOLEAN DEFAULT FALSE,
    verified_at         TIMESTAMP,
    
    -- Metadata
    is_primary          BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_address_user ON user_addresses(user_id);
CREATE INDEX idx_address_type ON user_addresses(address_type);
```

### 4. KYC_DOCUMENTS

```sql
CREATE TABLE kyc_documents (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    
    -- Document Info
    document_type       VARCHAR(50) NOT NULL,  -- AADHAAR, PAN, PASSPORT, DRIVING_LICENSE
    document_number     VARCHAR(100),
    document_url        TEXT NOT NULL,  -- S3 URL
    document_back_url   TEXT,           -- For two-sided docs
    
    -- Verification
    verification_status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, APPROVED, REJECTED
    verified_at         TIMESTAMP,
    verified_by         UUID,
    rejection_reason    TEXT,
    
    -- Metadata
    uploaded_at         TIMESTAMP DEFAULT NOW(),
    expires_at          TIMESTAMP,
    
    CONSTRAINT fk_kyc_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_kyc_user ON kyc_documents(user_id);
CREATE INDEX idx_kyc_status ON kyc_documents(verification_status);
CREATE INDEX idx_kyc_type ON kyc_documents(document_type);
```

### 5. USER_PREFERENCES

```sql
CREATE TABLE user_preferences (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL UNIQUE,
    
    -- Notification Preferences
    email_notifications BOOLEAN DEFAULT TRUE,
    sms_notifications   BOOLEAN DEFAULT TRUE,
    push_notifications  BOOLEAN DEFAULT TRUE,
    marketing_emails    BOOLEAN DEFAULT FALSE,
    
    -- Trading Preferences
    default_order_type  VARCHAR(20) DEFAULT 'LIMIT',
    auto_square_off     BOOLEAN DEFAULT FALSE,
    margin_warning_level DECIMAL(5,2) DEFAULT 80.00,
    confirm_before_order BOOLEAN DEFAULT TRUE,
    
    -- UI Preferences
    theme               VARCHAR(20) DEFAULT 'LIGHT',  -- LIGHT, DARK
    dashboard_layout    JSONB,  -- Custom dashboard config
    chart_type          VARCHAR(20) DEFAULT 'CANDLESTICK',
    
    -- Privacy
    show_profile_public BOOLEAN DEFAULT FALSE,
    allow_referrals     BOOLEAN DEFAULT TRUE,
    
    -- Metadata
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT fk_pref_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### 6. USER_WATCHLISTS (Trading Specific)

```sql
CREATE TABLE user_watchlists (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    name                VARCHAR(100) NOT NULL,
    symbols             JSONB NOT NULL,  -- ["AAPL", "GOOGL", "TSLA"]
    color_tag           VARCHAR(20),
    is_default          BOOLEAN DEFAULT FALSE,
    display_order       INT DEFAULT 0,
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT fk_watchlist_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_watchlist_user ON user_watchlists(user_id);
```

### 7. USER_BANK_ACCOUNTS

```sql
CREATE TABLE user_bank_accounts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    
    -- Bank Details
    account_number      VARCHAR(50) NOT NULL,
    ifsc_code           VARCHAR(20),
    swift_code          VARCHAR(20),
    bank_name           VARCHAR(100) NOT NULL,
    branch_name         VARCHAR(100),
    account_type        VARCHAR(20),  -- SAVINGS, CURRENT
    account_holder_name VARCHAR(200) NOT NULL,
    
    -- Verification
    is_verified         BOOLEAN DEFAULT FALSE,
    verified_at         TIMESTAMP,
    verification_method VARCHAR(50),  -- PENNY_DROP, MANUAL
    
    -- Status
    is_primary          BOOLEAN DEFAULT FALSE,
    is_active           BOOLEAN DEFAULT TRUE,
    
    -- Metadata
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT fk_bank_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_bank_user ON user_bank_accounts(user_id);
CREATE INDEX idx_bank_verified ON user_bank_accounts(is_verified);
```

### 8. USER_ACTIVITY_LOG (User Actions Audit)

```sql
CREATE TABLE user_activity_log (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             UUID NOT NULL,
    activity_type       VARCHAR(50) NOT NULL,  -- PROFILE_UPDATE, KYC_UPLOAD, PREFERENCE_CHANGE
    activity_data       JSONB,
    ip_address          VARCHAR(45),
    user_agent          TEXT,
    created_at          TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_activity_user ON user_activity_log(user_id);
CREATE INDEX idx_activity_type ON user_activity_log(activity_type);
CREATE INDEX idx_activity_time ON user_activity_log(created_at DESC);
```

---

## Service Communication

### 📡 Communication Patterns

```
┌─────────────────────────────────────────────────────────────────┐
│                    SERVICE INTERACTION FLOWS                     │
└─────────────────────────────────────────────────────────────────┘
```

### 1. User Registration Flow

```
Client
  │
  ├─→ POST /auth/register
  │   {email, password, firstName, lastName}
  │
  ▼
Auth Service
  │
  ├─→ 1. Hash password with BCrypt
  ├─→ 2. Generate user_id (UUID)
  ├─→ 3. Store in auth_credentials
  │
  ├─→ Feign/RestTemplate Call
  │   POST /internal/users
  │   {user_id, firstName, lastName, email}
  │
  ▼
User Service
  │
  ├─→ 4. Create users record
  ├─→ 5. Create user_profiles record
  ├─→ 6. Create user_preferences with defaults
  │
  ◄─── Return success
  │
Auth Service
  │
  ├─→ 7. Generate JWT tokens
  ├─→ 8. Store refresh token in Redis
  │
  ◄─── Return to Client
  │   {accessToken, refreshToken, user: {id, firstName, ...}}
```

### 2. Login Flow

```
Client
  │
  ├─→ POST /auth/login {email, password}
  │
  ▼
Auth Service
  │
  ├─→ 1. Find auth_credentials by email
  ├─→ 2. Check account lock status (Redis)
  ├─→ 3. Validate password (BCrypt)
  ├─→ 4. Check if 2FA enabled
  │
  ├─→ If 2FA Required:
  │   ├─→ Generate OTP
  │   ├─→ Store in Redis (5 min TTL)
  │   └─→ Return {requires2FA: true, tempToken}
  │
  ├─→ Fetch user details from User Service
  │   GET /internal/users/{user_id}
  │
  ▼
User Service
  │
  ├─→ Return user profile
  │
  ◄───
Auth Service
  │
  ├─→ 5. Generate JWT with user_id + roles
  ├─→ 6. Create refresh token → Store in Redis
  ├─→ 7. Reset failed attempts counter
  ├─→ 8. Log login_history
  │
  ◄─── Return to Client
       {accessToken, refreshToken, user: {...}}
```

### 3. Profile Update Flow

```
Client
  │
  ├─→ PUT /users/{id}
  │   Authorization: Bearer <JWT>
  │   {firstName, lastName, mobileNumber}
  │
  ▼
API Gateway
  │
  ├─→ 1. Extract JWT from header
  ├─→ 2. Validate JWT signature
  ├─→ 3. Check token blacklist (Redis)
  ├─→ 4. Route to User Service
  │
  ▼
User Service
  │
  ├─→ 5. Update users table
  ├─→ 6. Update updated_at timestamp
  ├─→ 7. Clear user cache (Redis)
  ├─→ 8. Log activity to user_activity_log
  │
  ◄─── Return updated profile
```

### 4. Password Change Flow

```
Client
  │
  ├─→ POST /auth/change-password
  │   {oldPassword, newPassword}
  │
  ▼
Auth Service
  │
  ├─→ 1. Validate old password
  ├─→ 2. Hash new password (BCrypt)
  ├─→ 3. Update auth_credentials.password_hash
  ├─→ 4. Update last_password_change
  ├─→ 5. Revoke all refresh tokens (Redis)
  ├─→ 6. Blacklist current JWT
  ├─→ 7. Log security_events
  │
  ├─→ Publish Event (Optional)
  │   UserPasswordChangedEvent(user_id)
  │
  ▼
User Service (Event Listener)
  │
  ├─→ Log activity to user_activity_log
  ├─→ Send notification email
  │
  ◄─── Success response
```

### 5. Email Update Flow (Sync Required)

```
Client
  │
  ├─→ PUT /users/{id}/email
  │   {newEmail}
  │
  ▼
User Service
  │
  ├─→ 1. Validate email format
  ├─→ 2. Check if email exists
  ├─→ 3. Update users.email
  │
  ├─→ Notify Auth Service
  │   PUT /internal/auth/update-email
  │   {user_id, newEmail}
  │
  ▼
Auth Service
  │
  ├─→ 4. Update auth_credentials.email
  ├─→ 5. Set is_email_verified = FALSE
  ├─→ 6. Send verification email
  │
  ◄─── Success
```

---

## Benefits of Separation

### 1. ⚡ Independent Scaling

```
┌─────────────────────────────────────────────┐
│         TRAFFIC PATTERNS                    │
├─────────────────────────────────────────────┤
│                                             │
│  Auth Service:                              │
│  ████████████ 10,000 RPS (9:15 AM - market open)
│  ███ 3,000 RPS (rest of day)               │
│                                             │
│  User Service:                              │
│  ████ 1,000 RPS (steady throughout)        │
│                                             │
│  ✅ Scale auth-service independently!       │
└─────────────────────────────────────────────┘
```

**Scaling Strategy:**
- Auth Service: 5 instances (scale to 10 during peak)
- User Service: 2 instances (consistent)

### 2. 🔒 Security Isolation

```
┌─────────────────────────────────────────────┐
│         DATABASE BREACH SCENARIO            │
├─────────────────────────────────────────────┤
│                                             │
│  Scenario A: Auth DB Compromised           │
│  ❌ Passwords exposed                       │
│  ✅ User profiles safe                      │
│  ✅ Can revoke all tokens                   │
│  ✅ Force password reset                    │
│                                             │
│  Scenario B: User DB Compromised           │
│  ✅ Passwords safe                          │
│  ❌ Profile data exposed                    │
│  ✅ Auth still works                        │
│  ✅ No authentication bypass                │
│                                             │
│  ✅ Defense in Depth!                       │
└─────────────────────────────────────────────┘
```

### 3. 👥 Team Autonomy

```
Security Team (Auth Service)
├── Updates: Quarterly
├── Focus: Security patches, compliance
├── Changes: Low risk, thoroughly tested
└── Deploys: During low-traffic hours

Product Team (User Service)
├── Updates: Weekly/Bi-weekly
├── Focus: New features, UX improvements
├── Changes: High frequency, A/B tested
└── Deploys: Anytime

✅ No deployment conflicts!
✅ Different release cycles!
```

### 4. 🔧 Technology Flexibility

```
Auth Service
├── Specialized: Spring Security, OAuth libraries
├── Heavy Redis usage for sessions/tokens
├── Security-first architecture
└── Immutable audit logs

User Service
├── Flexible: Business logic changes
├── Moderate caching strategy
├── Feature-first architecture
└── S3 integration for documents
```

### 5. 🚀 Deployment Independence

```
┌─────────────────────────────────────────────┐
│         DEPLOYMENT SCENARIOS                │
├─────────────────────────────────────────────┤
│                                             │
│  Add KYC verification feature               │
│  → Deploy only User Service                 │
│  → Zero downtime for Auth                   │
│                                             │
│  Add OAuth provider (Apple Login)           │
│  → Deploy only Auth Service                 │
│  → Zero downtime for User                   │
│                                             │
│  Critical security patch                    │
│  → Deploy Auth Service immediately          │
│  → User Service unaffected                  │
│                                             │
└─────────────────────────────────────────────┘
```

### 6. 📊 Monitoring & Debugging

```
Auth Service Metrics
├── Login success rate
├── Failed login attempts
├── Token generation rate
├── 2FA verification rate
└── Password reset requests

User Service Metrics
├── Profile update frequency
├── KYC approval rate
├── Document upload success
└── User growth rate

✅ Clear metric boundaries!
✅ Easier to debug issues!
```

---

## Implementation Guide

### Step 1: Service Communication Setup

#### Feign Client Configuration (Auth Service → User Service)

```java
// services/auth-service/src/main/java/com/sinha/ecom_system/auth_service/client/UserServiceClient.java

package com.sinha.ecom_system.auth_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "user-service")
public interface UserServiceClient {
    
    @PostMapping("/internal/users")
    UserDTO createUser(@RequestBody CreateUserRequest request);
    
    @GetMapping("/internal/users/{userId}")
    UserDTO getUserById(@PathVariable("userId") String userId);
    
    @PutMapping("/internal/users/{userId}/email")
    void updateEmail(@PathVariable("userId") String userId, 
                    @RequestParam String email);
}
```

#### Feign Client Configuration (User Service → Auth Service)

```java
// services/user-service/src/main/java/com/sinha/ecom_system/user_service/client/AuthServiceClient.java

package com.sinha.ecom_system.user_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {
    
    @PutMapping("/internal/auth/update-email")
    void updateEmail(@RequestParam String userId, @RequestParam String newEmail);
    
    @PostMapping("/internal/auth/notify-profile-update")
    void notifyProfileUpdate(@RequestParam String userId);
}
```

### Step 2: Enable Feign in Application

```java
// services/auth-service/src/main/java/com/sinha/ecom_system/auth_service/AuthServiceApplication.java

package com.sinha.ecom_system.auth_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
```

### Step 3: Internal API Controllers

```java
// services/user-service/src/main/java/com/sinha/ecom_system/user_service/controller/InternalUserController.java

package com.sinha.ecom_system.user_service.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {
    
    // Only accessible from other services (not through API Gateway)
    
    @PostMapping
    public UserDTO createUser(@RequestBody CreateUserRequest request) {
        // Create user logic
    }
    
    @GetMapping("/{userId}")
    public UserDTO getUserById(@PathVariable String userId) {
        // Get user logic
    }
}
```

### Step 4: API Gateway Routing

```yaml
# services/api-gateway/src/main/resources/application.yaml

spring:
  cloud:
    gateway:
      routes:
        # Auth Service Routes (Public)
        - id: auth-service-public
          uri: lb://auth-service
          predicates:
            - Path=/auth/**
          filters:
            - StripPrefix=0
        
        # User Service Routes (Protected)
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/users/**
          filters:
            - StripPrefix=0
            - JwtAuthenticationFilter  # Custom filter
        
        # Block internal routes from external access
        - id: block-internal-auth
          uri: no://op
          predicates:
            - Path=/internal/auth/**
          filters:
            - SetStatus=403
        
        - id: block-internal-user
          uri: no://op
          predicates:
            - Path=/internal/users/**
          filters:
            - SetStatus=403
```

---

## When to Combine Services

### ✅ You CAN Combine Them If:

1. **Small Application**
   - < 10,000 users
   - < 100 concurrent users
   - Simple authentication (no OAuth, 2FA)

2. **Single Team**
   - < 5 developers
   - Full-stack team
   - No dedicated security team

3. **Low Traffic**
   - < 100 requests/second
   - No peak traffic patterns
   - Consistent load

4. **Cost-Sensitive**
   - Limited infrastructure budget
   - Shared database acceptable
   - Minimal scaling needs

5. **MVP/Prototype**
   - Quick time-to-market
   - Prove concept first
   - Can refactor later

### Combined Service Structure

```
identity-service/
├── controller/
│   ├── AuthController.java
│   └── UserController.java
├── service/
│   ├── AuthService.java
│   └── UserService.java
├── repository/
│   ├── AuthRepository.java
│   └── UserRepository.java
└── model/
    ├── AuthCredentials.java
    └── User.java
```

---

## Real-World Examples

### 🏢 Industry Comparisons

| Company | Auth Service | User Service |
|---------|-------------|--------------|
| **Zerodha** | Kite Login Service | User Profile & KYC Service |
| **Netflix** | Netflix Account Login | User Preferences & Profiles |
| **Amazon** | Amazon Sign In | Account Details & Addresses |
| **Google** | Google Identity Platform | Gmail, Drive User Data |
| **Stripe** | Stripe Auth API | Stripe Customer API |
| **GitHub** | GitHub OAuth | GitHub User API |
| **Spotify** | Spotify Accounts | User Playlists & Preferences |

### Traffic Patterns (Real Trading Platform Example)

```
Time: 9:00 AM (Pre-market)
└─ Auth Service: 5,000 RPS
└─ User Service: 500 RPS

Time: 9:15 AM (Market Opens)
└─ Auth Service: 15,000 RPS (Peak)
└─ User Service: 1,200 RPS

Time: 12:00 PM (Mid-day)
└─ Auth Service: 2,000 RPS
└─ User Service: 800 RPS

Time: 3:30 PM (Market Closes)
└─ Auth Service: 8,000 RPS
└─ User Service: 1,500 RPS
```

---

## Redis Usage Comparison

### Auth Service Redis Keys

```
# High Write/Read Frequency
refresh_token:{token_id} → user_id (TTL: 7d)
session:{session_id} → user_data (TTL: 15m)
blacklist:{jti} → "revoked" (TTL: token expiry)
login_attempt:{email} → count (TTL: 15m)
account_lock:{email} → "locked" (TTL: 15m)
otp:{email} → "123456" (TTL: 5m)
2fa_temp:{token} → user_id (TTL: 5m)

# Cache
user_roles:{user_id} → roles[] (TTL: 10m)
```

### User Service Redis Keys

```
# Moderate Read Frequency, Low Write
user_profile:{user_id} → profile_json (TTL: 30m)
user_kyc_status:{user_id} → "VERIFIED" (TTL: 1h)
user_preferences:{user_id} → preferences_json (TTL: 1h)
```

---

## Performance Metrics

### Database Operations

| Operation | Auth Service | User Service |
|-----------|--------------|--------------|
| **Reads/sec** | 10,000+ | 2,000+ |
| **Writes/sec** | 5,000+ | 500+ |
| **Avg Query Time** | < 5ms | < 10ms |
| **Table Size** | Small (credentials) | Large (profiles) |
| **Index Strategy** | Heavy (email, user_id) | Moderate |

---

## Security Considerations

### Auth Service Security

```
✅ Password hashing: BCrypt (strength 12)
✅ Token signing: RS256 (asymmetric)
✅ Rate limiting: 5 attempts per 15 minutes
✅ Account lockout: 15 minutes after 5 failed attempts
✅ Token expiry: 15 minutes (access), 7 days (refresh)
✅ 2FA: TOTP (Time-based OTP)
✅ Audit logging: All authentication events
✅ IP tracking: Login history with geolocation
```

### User Service Security

```
✅ Authorization: JWT validation on all endpoints
✅ Resource ownership: Users can only access own data
✅ Data validation: Input sanitization
✅ KYC encryption: Sensitive docs encrypted at rest (S3)
✅ PII protection: Masked in logs
✅ Audit trail: All profile changes logged
```

---

## Migration Strategy

### From Monolith to Microservices

```
Phase 1: Separate Databases
├── Create auth_db (copy credentials from main DB)
├── Create user_db (copy user data from main DB)
└── Run both in parallel

Phase 2: Deploy Services
├── Deploy auth-service (reads from auth_db)
├── Deploy user-service (reads from user_db)
└── API Gateway routes 50% traffic to new services

Phase 3: Gradual Migration
├── Monitor metrics & errors
├── Increase traffic to 100%
└── Deprecate monolith endpoints

Phase 4: Data Cleanup
├── Remove user data from main DB
└── Remove auth data from main DB
```

---

## Conclusion

### Key Takeaways

✅ **Separation provides:**
- Independent scaling
- Security isolation
- Team autonomy
- Deployment flexibility
- Clear boundaries

✅ **Use separate services when:**
- > 10,000 users
- High traffic variance
- Multiple teams
- Security-critical application
- Long-term scalability needed

✅ **Combine services when:**
- MVP/Prototype
- < 10,000 users
- Single team
- Cost constraints
- Simple requirements

---

## Additional Resources

- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [Microservices Patterns](https://microservices.io/patterns/index.html)
- [Redis Documentation](https://redis.io/documentation)
- [OAuth 2.0 Specification](https://oauth.net/2/)

---

**Last Updated:** December 2024  
**Author:** Trading Platform Architecture Team  
**Version:** 1.0

