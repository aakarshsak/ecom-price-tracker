# 🛡️ Security Architecture & Best Practices

Comprehensive security guide for the Trading Platform.

---

## 📋 Table of Contents

- [Security Architecture Overview](#security-architecture-overview)
- [Authentication & Authorization](#authentication--authorization)
- [Security Best Practices](#security-best-practices)
- [Threat Protection](#threat-protection)
- [Compliance & Auditing](#compliance--auditing)
- [Security Checklist](#security-checklist)

---

## Security Architecture Overview

### Defense in Depth

```
┌──────────────────────────────────────────────────────────┐
│                     CLIENT LAYER                         │
│  • HTTPS Only                                            │
│  • Token Storage (Memory/HTTP-Only Cookies)              │
│  • XSS Protection                                        │
└─────────────────────┬────────────────────────────────────┘
                      │
                      ▼
┌──────────────────────────────────────────────────────────┐
│                   API GATEWAY LAYER                      │
│  • Rate Limiting (Redis)                                 │
│  • JWT Validation                                        │
│  • CORS Configuration                                    │
│  • Request Logging                                       │
│  • DDoS Protection                                       │
└─────────────────────┬────────────────────────────────────┘
                      │
                      ▼
┌──────────────────────────────────────────────────────────┐
│                   SERVICE LAYER                          │
│  • JWT Authentication Filter                             │
│  • Role-Based Access Control (RBAC)                      │
│  • Input Validation                                      │
│  • Business Logic Security                               │
└─────────────────────┬────────────────────────────────────┘
                      │
                      ▼
┌──────────────────────────────────────────────────────────┐
│                   DATA LAYER                             │
│  • Encrypted at Rest                                     │
│  • Encrypted in Transit (TLS)                            │
│  • Password Hashing (BCrypt)                             │
│  • Token Hashing (SHA-256)                               │
│  • Audit Logging                                         │
└──────────────────────────────────────────────────────────┘
```

---

## Authentication & Authorization

### 1. Password Security

**Hashing Algorithm**: BCrypt with strength 12

```java
// Password hashing
String hashedPassword = passwordEncoder.encode(plainPassword);

// Password verification
boolean matches = passwordEncoder.matches(plainPassword, hashedPassword);
```

**Requirements**:
- Minimum 8 characters
- At least one uppercase letter
- At least one lowercase letter
- At least one number
- At least one special character

**Example**: `SecurePass@123`

### 2. JWT Token Security

**Access Token**:
- **Lifetime**: 15 minutes
- **Algorithm**: HS256 (HMAC with SHA-256)
- **Storage**: Client memory (not localStorage)
- **Contains**: User ID, email, roles, permissions

**Refresh Token**:
- **Lifetime**: 7 days
- **Storage**: Database (SHA-256 hash)
- **Revocable**: Yes (on logout)
- **One-time use**: Optional rotation

**Secret Key Requirements**:
- Minimum 256 bits (32 characters)
- Randomly generated
- Never commit to version control
- Rotate periodically (every 90 days)

### 3. Role-Based Access Control (RBAC)

**Default Roles**:

| Role | Description | Permissions |
|------|-------------|-------------|
| `ROLE_USER` | Default user | View market data, view portfolio |
| `ROLE_TRADER` | Active trader | Place orders, withdraw funds |
| `ROLE_ADMIN` | Administrator | Full access |
| `ROLE_SUPPORT` | Customer support | View user data, no financial operations |

**Permission Model**:

```java
@PreAuthorize("hasRole('TRADER')")
public OrderResponse placeOrder(OrderRequest request) {
    // Only traders can place orders
}

@PreAuthorize("hasPermission('WITHDRAW')")
public WithdrawalResponse withdraw(WithdrawalRequest request) {
    // Only users with WITHDRAW permission
}
```

---

## Security Best Practices

### 1. Input Validation

**Always validate and sanitize user input**:

```java
@Valid
public ResponseEntity<UserResponse> createUser(@RequestBody @Valid CreateUserRequest request) {
    // Spring Validation automatically validates
}
```

**Validation Rules**:
- Email format validation
- Phone number format validation
- SQL injection prevention (use JPA/prepared statements)
- XSS prevention (sanitize HTML)
- Path traversal prevention

### 2. Secure Headers

**Required Security Headers**:

```yaml
# application.yaml
server:
  servlet:
    security:
      headers:
        content-security-policy: "default-src 'self'"
        x-content-type-options: nosniff
        x-frame-options: DENY
        x-xss-protection: "1; mode=block"
        strict-transport-security: max-age=31536000; includeSubDomains
```

### 3. CORS Configuration

```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "https://yourdomain.com",
            "https://app.yourdomain.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

### 4. HTTPS Only

**Production Requirements**:
- Enforce HTTPS for all endpoints
- Use TLS 1.2 or higher
- Valid SSL certificate
- Redirect HTTP to HTTPS

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.requiresChannel(channel -> 
            channel.anyRequest().requiresSecure()
        );
        return http.build();
    }
}
```

### 5. Secrets Management

**Never hardcode secrets**:

```properties
# ❌ BAD - Hardcoded
jwt.secret-key=my-secret-key

# ✅ GOOD - Environment variable
jwt.secret-key=${JWT_SECRET_KEY}

# ✅ BEST - AWS Secrets Manager
jwt.secret-key=${aws:secretsmanager:jwt-secret}
```

---

## Threat Protection

### 1. Brute Force Protection

**Account Lockout**:
- 5 failed login attempts → Lock for 15 minutes
- Track failed attempts in database
- Reset counter on successful login

```java
public void handleFailedLogin(String email) {
    AuthCredential user = repository.findByEmail(email);
    user.incrementFailedAttempts();
    
    if (user.getFailedAttempts() >= 5) {
        user.lockAccount(15); // Lock for 15 minutes
    }
    
    repository.save(user);
}
```

### 2. Rate Limiting

**API Rate Limits** (per user/IP):

| Endpoint | Limit |
|----------|-------|
| `/auth/login` | 5 requests/minute |
| `/auth/register` | 3 requests/hour |
| `/auth/refresh` | 10 requests/minute |
| `/api/**` (authenticated) | 100 requests/minute |

**Implementation** (Redis-based):

```java
@Bean
public RateLimiter rateLimiter() {
    return RedisRateLimiter.create(
        100,  // replenishRate (requests per second)
        200   // burstCapacity (max concurrent requests)
    );
}
```

### 3. SQL Injection Prevention

**Use JPA/Hibernate** (never concatenate SQL):

```java
// ✅ GOOD - Parameterized query
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmail(@Param("email") String email);

// ❌ BAD - String concatenation
@Query("SELECT u FROM User u WHERE u.email = '" + email + "'")
```

### 4. XSS Protection

**Sanitize user input**:

```java
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public String sanitizeHtml(String input) {
    return Jsoup.clean(input, Safelist.basic());
}
```

### 5. CSRF Protection

**For REST APIs with JWT**: CSRF protection can be disabled (stateless)

**For cookie-based auth**: Enable CSRF protection

```java
http.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
);
```

### 6. Token Revocation

**Logout Flow**:
1. Mark refresh token as revoked in database
2. Optional: Blacklist access token in Redis (for immediate revocation)

```java
public void logout(UUID userId, String refreshToken) {
    // Revoke refresh token
    String tokenHash = hashToken(refreshToken);
    refreshTokenRepository.revokeToken(tokenHash);
    
    // Optional: Blacklist access token
    String accessTokenId = jwtUtil.getTokenId(accessToken);
    redisTemplate.opsForValue().set(
        "blacklist:" + accessTokenId,
        "revoked",
        15, TimeUnit.MINUTES
    );
}
```

---

## Compliance & Auditing

### 1. Audit Logging

**Log security events**:

```java
@Aspect
@Component
public class AuditAspect {
    
    @AfterReturning("@annotation(Audited)")
    public void logSecurityEvent(JoinPoint joinPoint) {
        String userId = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        String action = joinPoint.getSignature().getName();
        
        auditLogger.log(userId, action, LocalDateTime.now());
    }
}
```

**Events to log**:
- Login attempts (success/failure)
- Password changes
- Account lockouts
- Token generation
- Logout events
- Permission changes
- Failed authorization attempts

### 2. Data Encryption

**At Rest**:
- Database encryption (PostgreSQL TDE)
- Sensitive fields encrypted (credit cards, SSN)
- Backup encryption

**In Transit**:
- TLS 1.2+ for all communications
- Certificate pinning (mobile apps)

### 3. GDPR Compliance

**User Rights**:
- Right to access data (`GET /user/data`)
- Right to deletion (`DELETE /user/account`)
- Right to data portability (export JSON/CSV)
- Right to be forgotten (anonymization)

### 4. PCI-DSS Compliance

For trading platforms handling payments:
- Never store CVV
- Tokenize card numbers
- Use payment gateway (Stripe, Razorpay)
- Annual security audits

---

## Security Checklist

### Development Phase

- [ ] All passwords hashed with BCrypt (strength ≥ 12)
- [ ] JWT secret key is 256-bit minimum
- [ ] No hardcoded secrets in code
- [ ] Input validation on all endpoints
- [ ] SQL injection prevention (use JPA)
- [ ] XSS protection enabled
- [ ] CORS configured properly
- [ ] Rate limiting implemented
- [ ] Account lockout after failed attempts
- [ ] Token expiry configured (short-lived access tokens)
- [ ] Refresh token revocation implemented
- [ ] Audit logging for security events

### Testing Phase

- [ ] Penetration testing completed
- [ ] OWASP Top 10 vulnerabilities tested
- [ ] Load testing for DDoS resilience
- [ ] SQL injection testing
- [ ] XSS testing
- [ ] CSRF testing (if applicable)
- [ ] Authentication bypass testing
- [ ] Authorization bypass testing
- [ ] Token tampering testing

### Deployment Phase

- [ ] HTTPS enforced (no HTTP)
- [ ] Valid SSL certificate installed
- [ ] Security headers configured
- [ ] Secrets stored in vault (AWS Secrets Manager, HashiCorp Vault)
- [ ] Database encryption enabled
- [ ] Backup encryption enabled
- [ ] WAF configured (AWS WAF, Cloudflare)
- [ ] DDoS protection enabled
- [ ] Monitoring and alerting configured
- [ ] Incident response plan documented

### Production Monitoring

- [ ] Monitor failed login attempts
- [ ] Alert on multiple lockouts
- [ ] Alert on unusual token activity
- [ ] Monitor API rate limit violations
- [ ] Log and review security events daily
- [ ] Regular security audits (quarterly)
- [ ] Dependency vulnerability scanning (Dependabot)
- [ ] Penetration testing (annual)

---

## Common Vulnerabilities & Mitigations

| Vulnerability | Risk | Mitigation |
|---------------|------|------------|
| **Weak Passwords** | High | Enforce strong password policy, BCrypt hashing |
| **SQL Injection** | Critical | Use JPA/Hibernate, parameterized queries |
| **XSS** | High | Input sanitization, Content Security Policy |
| **CSRF** | Medium | CSRF tokens (for cookie auth), SameSite cookies |
| **Brute Force** | High | Account lockout, rate limiting, CAPTCHA |
| **Token Theft** | High | Short-lived tokens, HTTPS only, HTTP-only cookies |
| **Session Hijacking** | High | Stateless JWT, secure token storage |
| **Man-in-the-Middle** | Critical | HTTPS, certificate pinning |
| **Insecure Deserialization** | Critical | Validate all input, use safe serialization |
| **Broken Authentication** | Critical | Multi-factor authentication, strong session management |

---

## Security Resources

### Tools

- **Dependency Scanning**: Dependabot, Snyk
- **SAST**: SonarQube, Checkmarx
- **DAST**: OWASP ZAP, Burp Suite
- **Secrets Scanning**: GitGuardian, TruffleHog

### References

- **OWASP Top 10**: https://owasp.org/www-project-top-ten/
- **JWT Best Practices**: https://tools.ietf.org/html/rfc8725
- **Spring Security**: https://spring.io/projects/spring-security
- **CWE/SANS Top 25**: https://cwe.mitre.org/top25/

### Internal Documentation

- [Auth Service Architecture](./AUTH_SERVICE.md)
- [JWT Authentication Guide](./JWT_AUTHENTICATION.md)
- [Quick Start Guide](./QUICK_START_GUIDE.md)
- [API Gateway Configuration](./API_GATEWAY.md)

---

**Security is an ongoing process, not a one-time task. Stay vigilant!** 🛡️

Last Updated: December 25, 2024

