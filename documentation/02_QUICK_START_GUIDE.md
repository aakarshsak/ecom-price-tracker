# ⚡ Quick Start Guide

Get the Trading Platform Auth Service running in 5 minutes!

---

## 🎯 Prerequisites

Ensure you have the following installed:

```bash
✅ Java 17 or higher
✅ PostgreSQL 14+
✅ Maven 3.8+
✅ Git
```

---

## 🚀 Setup Steps

### Step 1: Clone the Repository

```bash
git clone <repository-url>
cd ecom-price-tracker
```

### Step 2: Start PostgreSQL

```bash
# Using PostgreSQL service
sudo systemctl start postgresql  # Linux
brew services start postgresql   # Mac

# OR using Docker
docker run --name postgres-auth \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:14
```

### Step 3: Create Database

```bash
psql -U postgres -c "CREATE DATABASE auth_db;"
```

### Step 4: Configure Environment

Create `.env` file in `services/auth-service/`:

```properties
# Database Configuration
SPRING.DATASOURCE.URL=jdbc:postgresql://localhost:5432/auth_db
SPRING.DATASOURCE.USERNAME=postgres
SPRING.DATASOURCE.PASSWORD=password

# Server Configuration
SERVER_PORT=8081

# Eureka (Optional - can skip for local dev)
EUREKA.CLIENT.SERVICE_URL.DEFAULT_ZONE=http://localhost:8761/eureka

# JWT Configuration (⚠️ Change in production!)
JWT_SECRET_KEY=my-super-secret-jwt-key-must-be-at-least-256-bits-long-for-hs256
JWT_ACCESS_TOKEN_EXPIRY=900000
JWT_REFRESH_TOKEN_EXPIRY=604800000
JWT_ISSUER=trading-platform-auth-service

# Redis (Optional - for advanced features)
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=
```

### Step 5: Build and Run

```bash
cd services/auth-service
mvn clean install
mvn spring-boot:run
```

**✅ Service should start on http://localhost:8081**

---

## 🧪 Test the API

### 1. Check Health

```bash
curl http://localhost:8081/actuator/health
```

**Expected Response:**
```json
{"status":"UP"}
```

### 2. Register a User

```bash
curl -X POST http://localhost:8081/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "User",
    "email": "test@example.com",
    "password": "Test@123",
    "mobileNumber": "+1234567890",
    "dob": "1990-01-01"
  }'
```

**Save the tokens from response:**
```bash
export ACCESS_TOKEN="<your-access-token>"
export REFRESH_TOKEN="<your-refresh-token>"
```

### 3. Login

```bash
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test@123"
  }'
```

### 4. Access Protected Endpoint

```bash
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://localhost:8081/auth/profile
```

### 5. Refresh Token

```bash
curl -X POST http://localhost:8081/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}"
```

---

## 📋 Available Endpoints

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/auth/register` | POST | No | Register new user |
| `/auth/login` | POST | No | Login and get JWT tokens |
| `/auth/refresh` | POST | No | Refresh access token |
| `/auth/logout` | POST | Yes | Logout from current device |
| `/auth/logout-all` | POST | Yes | Logout from all devices |
| `/auth/profile` | GET | Yes | Get user profile |
| `/actuator/health` | GET | No | Health check |

---

## 🐛 Troubleshooting

### Service Won't Start

**Problem: Port 8081 already in use**
```bash
# Find and kill process
lsof -i :8081  # Mac/Linux
netstat -ano | findstr :8081  # Windows

# OR change port in .env
SERVER_PORT=8082
```

**Problem: Database connection failed**
```bash
# Verify PostgreSQL is running
psql -U postgres -c "SELECT 1;"

# Check credentials in .env
# Ensure database 'auth_db' exists
```

**Problem: Flyway migration failed**
```bash
# Drop and recreate database
psql -U postgres
DROP DATABASE auth_db;
CREATE DATABASE auth_db;
\q
```

### API Call Issues

**Problem: 401 Unauthorized**
- Ensure token is in format: `Authorization: Bearer <token>`
- Check if token has expired (15 min for access token)
- Use refresh token if access token expired

**Problem: Invalid JWT token**
- Verify `JWT_SECRET_KEY` is at least 32 characters long
- Ensure secret key hasn't changed between token generation and validation

---

## ✅ Verification Checklist

- [ ] PostgreSQL is running
- [ ] Database `auth_db` created
- [ ] `.env` file configured
- [ ] Service starts without errors
- [ ] Health check returns `{"status":"UP"}`
- [ ] Can register a new user
- [ ] Can login with credentials
- [ ] Can access protected endpoint with JWT
- [ ] Can refresh access token

---

## 📚 Next Steps

Now that your service is running:

1. **Understand the Architecture**  
   → Read [AUTH_SERVICE.md](./AUTH_SERVICE.md)

2. **Learn about JWT Implementation**  
   → Read [JWT_AUTHENTICATION.md](./JWT_AUTHENTICATION.md)

3. **Review Security Best Practices**  
   → Read [SECURITY.md](./SECURITY.md)

4. **Set up API Gateway**  
   → Read [API_GATEWAY.md](./API_GATEWAY.md)

5. **Deploy to Production**  
   → Read [Deployment Guide](../README.md#phase-7-aws-deployment)

---

## 💡 Quick Commands Reference

```bash
# Start service
cd services/auth-service && mvn spring-boot:run

# Build JAR
mvn clean package

# Run tests
mvn test

# Check logs
tail -f logs/auth-service.log

# Connect to database
psql -U postgres -d auth_db

# View all users
psql -U postgres -d auth_db -c "SELECT email, created_at FROM auth_credentials;"

# View refresh tokens
psql -U postgres -d auth_db -c "SELECT user_id, expires_at, revoked FROM refresh_tokens;"
```

---

## 🆘 Need Help?

- **Documentation Issues**: Check [documentation/README.md](./README.md)
- **JWT Token Problems**: See [JWT_AUTHENTICATION.md - Troubleshooting](./JWT_AUTHENTICATION.md#troubleshooting)
- **Architecture Questions**: See [AUTH_SERVICE.md](./AUTH_SERVICE.md)
- **Security Concerns**: See [SECURITY.md](./SECURITY.md)

---

**🎉 Congratulations! Your auth service is running!**

Start building the rest of the trading platform by following the [main README](../README.md).

