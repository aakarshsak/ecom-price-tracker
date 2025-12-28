# 📚 Trading Platform Documentation

Welcome to the comprehensive documentation for the Cloud-Native Event-Driven Trading Platform.

---

## 📖 Table of Contents

### 🚀 Getting Started
- [Quick Start Guide](./QUICK_START_GUIDE.md) - Get up and running in 5 minutes
- [Project Overview](../README.md) - Architecture and tech stack overview

### 🔐 Authentication & Security
- [Authentication Service](./AUTH_SERVICE.md) - Auth service architecture and responsibilities
- [JWT Authentication](./JWT_AUTHENTICATION.md) - Complete JWT implementation guide
- [Security Architecture](./SECURITY.md) - Security best practices and architecture

### 🌐 API & Gateway
- [API Gateway](./API_GATEWAY.md) - Gateway configuration and routing

### 🏗️ Architecture & Design
- [Microservices Architecture](../README.md#microservices-breakdown) - Service breakdown and responsibilities
- [Event-Driven Design](../README.md#example-event-flows) - Kafka event flows

---

## 🎯 Documentation by Role

### For Developers
1. Start with [Quick Start Guide](./QUICK_START_GUIDE.md)
2. Read [Authentication Service](./AUTH_SERVICE.md)
3. Implement [JWT Authentication](./JWT_AUTHENTICATION.md)
4. Review [Security Architecture](./SECURITY.md)

### For DevOps Engineers
1. [API Gateway](./API_GATEWAY.md) - Gateway setup
2. [Security Architecture](./SECURITY.md) - Security infrastructure
3. [Deployment Guide](../README.md#phase-7-aws-deployment) - AWS deployment

### For Architects
1. [Project Overview](../README.md) - High-level architecture
2. [Authentication Service](./AUTH_SERVICE.md) - Auth/User service separation
3. [Security Architecture](./SECURITY.md) - Security design patterns

---

## 📂 Documentation Structure

```
documentation/
├── README.md                    # This file - Documentation index
├── QUICK_START_GUIDE.md        # 5-minute setup guide
├── AUTH_SERVICE.md             # Auth service architecture
├── JWT_AUTHENTICATION.md       # JWT implementation & API reference
├── SECURITY.md                 # Security architecture & best practices
└── API_GATEWAY.md              # API Gateway implementation
```

---

## 🔄 Status of Implementation

| Component | Status | Documentation |
|-----------|--------|---------------|
| Auth Service | ✅ Implemented | [AUTH_SERVICE.md](./AUTH_SERVICE.md) |
| JWT Authentication | ✅ Implemented | [JWT_AUTHENTICATION.md](./JWT_AUTHENTICATION.md) |
| User Service | ⏳ Planned | [AUTH_SERVICE.md](./AUTH_SERVICE.md#user-service) |
| API Gateway | 📝 Design Phase | [API_GATEWAY.md](./API_GATEWAY.md) |
| Market Data Service | ⏳ Planned | ../README.md |
| Order Service | ⏳ Planned | ../README.md |

---

## 🆘 Need Help?

### Common Tasks

**I want to set up the project locally**
→ See [Quick Start Guide](./QUICK_START_GUIDE.md)

**I need to understand authentication**
→ See [JWT Authentication](./JWT_AUTHENTICATION.md)

**I'm implementing a new service**
→ See [Project Overview](../README.md) for service responsibilities

**I need API documentation**
→ See [JWT Authentication - API Reference](./JWT_AUTHENTICATION.md#api-endpoints)

**I'm having issues with JWT tokens**
→ See [JWT Troubleshooting](./JWT_AUTHENTICATION.md#troubleshooting)

---

## 📝 Contributing to Documentation

When adding new documentation:

1. **Keep it concise** - Break large docs into focused sections
2. **Avoid redundancy** - Link to existing docs instead of duplicating
3. **Use clear structure** - Follow the existing format
4. **Add examples** - Include code samples and API calls
5. **Update this index** - Add new docs to the table of contents

---

## 🔗 External Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)
- [JWT.io](https://jwt.io/) - JWT token decoder
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Docker Documentation](https://docs.docker.com/)

---

Last Updated: December 25, 2024

