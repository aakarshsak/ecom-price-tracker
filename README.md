# 📈 Cloud-Native Event-Driven Trading Platform

A production-grade microservices trading system similar to what companies like **Zerodha**, **Groww**, **Upstox**, **Binance**, or **Robinhood** run internally.

---

## 🎯 Core Principles (Important for Interviews)

| Principle | Status |
|-----------|--------|
| No monolith — microservices from Day 1 | ✅ |
| Event-driven (async first) | ✅ |
| Real-time data streaming | ✅ |
| Container-first (Docker + EKS) | ✅ |
| Infra as Code | ✅ |
| Observability + security baked in | ✅ |

---

## 🧱 Microservices Breakdown (Spring Boot)

| Service | Responsibility | DB |
|---------|---------------|-----|
| **Gateway Service** | Routing, rate limiting, auth filter | — |
| **Auth Service** | User auth, JWT, 2FA, sessions | Redis + RDS |
| **User Service** | Profile, KYC, preferences | RDS |
| **Market Data Service** | Real-time prices, charts, historical data | Redis + TimescaleDB |
| **Order Service** | Order placement, modification, cancellation | RDS |
| **Matching Engine** | Order matching, trade execution | In-Memory + RDS |
| **Portfolio Service** | Holdings, P&L, positions | RDS |
| **Wallet Service** | Funds, deposits, withdrawals | RDS |
| **Risk Service** | Margin checks, position limits, circuit breakers | Redis |
| **Notification Service** | Trade alerts, price alerts, SMS/Push | — |
| **Analytics Service** | Trading reports, performance metrics | Kinesis + S3 |

---

## 🗺️ Trading Platform Architecture

```
                           Client Apps
                    (Web / Mobile / Trading Terminal)
                                │
                       ┌────────▼────────┐
                       │   API Gateway   │
                       │ (Rate Limiting) │
                       └────────┬────────┘
                                │
                       ┌────────▼────────┐
                       │  Spring Cloud   │
                       │    Gateway      │
                       └────────┬────────┘
                                │
    ┌───────────────────────────┼───────────────────────────┐
    │                           │                           │
┌───▼───┐  ┌───────┐  ┌────────▼────────┐  ┌───────┐  ┌────▼────┐
│ Auth  │  │ User  │  │  Market Data    │  │ Order │  │ Wallet  │
│Service│  │Service│  │    Service      │  │Service│  │ Service │
└───────┘  └───────┘  └────────┬────────┘  └───┬───┘  └─────────┘
                               │               │
                        WebSocket          ┌───▼───────────┐
                        (Real-time)        │Matching Engine│
                               │           └───────┬───────┘
                               │                   │
                    ┌──────────▼───────────────────▼──────────┐
                    │              Kafka Cluster              │
                    │  (Order Events, Trade Events, Prices)   │
                    └──────────────────┬──────────────────────┘
                                       │
           ┌───────────────────────────┼───────────────────────────┐
           │                           │                           │
    ┌──────▼──────┐           ┌────────▼────────┐         ┌───────▼───────┐
    │  Portfolio  │           │   Risk Service  │         │ Notification  │
    │   Service   │           │ (Margin/Limits) │         │    Service    │
    └─────────────┘           └─────────────────┘         └───────────────┘
```

---

## 🔧 AWS Services — Purpose Mapping

### 🖥 Compute & Containers

#### EC2
- Worker nodes for EKS
- High-performance instances for Matching Engine

#### EKS (Primary)
- Runs all Spring Boot microservices
- HPA for market hours scaling

#### ECS + Fargate
- Notification service (serverless containers)
- Scheduled jobs

---

### 🗄 Databases & Storage

#### RDS (PostgreSQL)
- Users, Orders, Trades
- Wallet transactions
- Audit logs

#### Redis (ElastiCache)
- Real-time price cache
- Session management
- Rate limiting counters
- Order book snapshots

#### TimescaleDB (on EC2/RDS)
- Historical price data (OHLCV)
- Time-series analytics

#### S3
- Trade reports
- KYC documents
- Historical data archives

---

### ⚡ Cache & Real-time

#### Redis (ElastiCache)
- Market data cache (sub-millisecond reads)
- User session tokens
- Position cache
- Circuit breaker states

#### ElastiCache for Redis Cluster
- Distributed caching for high availability

---

### 🌐 Networking & Security

#### VPC
- **Public:** ALB, NAT Gateway
- **Private:** EKS, RDS, Redis, Matching Engine

#### IAM
- IRSA for pods
- Least privilege access
- MFA enforcement

#### WAF
- DDoS protection
- Rate limiting at edge

#### Secrets Manager
- API keys
- Database credentials
- Third-party integrations

---

### 📩 Messaging & Streaming

#### Kafka (MSK)
- Order events
- Trade executions
- Price updates
- Risk alerts

#### SQS
- Notification queue
- Report generation queue
- Withdrawal processing

#### SNS
- Trade confirmations fan-out
- Alert broadcasting

#### Kinesis
- Real-time analytics stream
- Market data ingestion

---

### 🧠 Serverless

#### Lambda
- Report generation (PDF)
- KYC document processing
- Price alert triggers
- EOD reconciliation

---

### 🔍 Observability & Auditing

#### CloudWatch
- Logs
- Custom metrics (orders/sec, latency)
- Alarms

#### X-Ray
- Distributed tracing (order → trade flow)

#### CloudTrail
- Audit all AWS API calls
- Compliance logging

---

### 🔄 CI/CD

#### CodePipeline
- Git → Build → Deploy

#### CodeBuild
- Test + Docker image

#### CodeDeploy
- Blue-Green deployment (zero downtime)

---

## 🧪 Example Event Flows (Interview-Gold)

### 📊 Order Placement → Trade Execution Flow

```
1. Client → Gateway → Order Service (Place Buy Order)
2. Order Service → Risk Service (Sync: Margin check)
3. Risk Service validates → Returns OK/REJECT
4. Order Service saves order (Status: PENDING)
5. Order Service → Kafka: "OrderCreated" event
6. Matching Engine consumes → Matches with sell orders
7. Matching Engine → Kafka: "TradeExecuted" event
8. Portfolio Service consumes → Updates holdings
9. Wallet Service consumes → Debits funds
10. Notification Service → Sends trade confirmation
11. Analytics Service → Streams to Kinesis
```

### 📈 Real-time Price Update Flow

```
1. External Feed → Market Data Service (WebSocket/REST)
2. Market Data Service → Redis (Update price cache)
3. Market Data Service → Kafka: "PriceUpdated" event
4. Risk Service consumes → Checks margin calls
5. Notification Service → Triggers price alerts
6. WebSocket Server → Pushes to subscribed clients
```

### 💰 Withdrawal Request Flow

```
1. Client → Wallet Service (Withdrawal request)
2. Wallet Service → User Service (KYC verification)
3. Wallet Service → Risk Service (Check open positions)
4. Wallet Service saves request (Status: PENDING)
5. Wallet Service → SQS: Withdrawal queue
6. Lambda processes → Initiates bank transfer
7. Wallet Service updates → Status: COMPLETED
8. Notification Service → Sends confirmation
```

---

## 🧾 Resume-Ready Description

> Designed and implemented a cloud-native, event-driven **real-time trading platform** using **Java 17 & Spring Boot**, deployed on **AWS EKS**, featuring a **high-performance matching engine**, **real-time market data streaming** via WebSockets, leveraging **Redis**, **PostgreSQL**, **Kafka (MSK)**, **Kinesis**, and **Terraform**, with comprehensive **risk management**, **CI/CD pipelines**, and distributed tracing via **CloudWatch & X-Ray**.

---

## 💡 Why This Project Is Strong

| Strength | ✔ |
|----------|---|
| Real-time data streaming | ✔ |
| High-throughput matching engine | ✔ |
| Event-driven architecture | ✔ |
| Financial domain complexity | ✔ |
| Clear microservice boundaries | ✔ |
| Production-grade observability | ✔ |
| Resume + interview friendly | ✔ |

---

# 📆 Development Plan: Local First → AWS Deployment

## 🎯 Strategy: Build Locally, Deploy to Cloud

**Local-first development** is the smart approach — it's faster, cheaper, and easier to debug.

---

## 📁 Project Structure

```
📁 trading-platform/
├── 📁 infrastructure/
│   ├── docker-compose.yml
│   ├── docker-compose.prod.yml
│   └── init-scripts/
├── 📁 services/
│   ├── gateway-service/
│   ├── auth-service/
│   ├── user-service/
│   ├── market-data-service/
│   ├── order-service/
│   ├── matching-engine/
│   ├── portfolio-service/
│   ├── wallet-service/
│   ├── risk-service/
│   └── notification-service/
├── 📁 common/
│   ├── common-dto/
│   ├── common-events/
│   └── common-utils/
└── 📁 deployment/
    ├── terraform/
    └── kubernetes/
```

---

## 🛠️ Tech Stack: Local vs AWS

| Layer | Local | AWS |
|-------|-------|-----|
| **Runtime** | Docker Compose | EKS |
| **Database** | PostgreSQL (Docker) | RDS PostgreSQL |
| **Time-series DB** | TimescaleDB (Docker) | TimescaleDB on EC2 |
| **Messaging** | Kafka (Docker) | MSK |
| **Cache** | Redis (Docker) | ElastiCache |
| **Gateway** | Spring Cloud Gateway | Same + ALB |
| **WebSocket** | Spring WebSocket | Same + API Gateway WebSocket |
| **Secrets** | .env files | AWS Secrets Manager |
| **Logs** | Console/File | CloudWatch |
| **Tracing** | Jaeger | X-Ray |
| **API Docs** | Swagger UI (SpringDoc) | Same |

---

## 📅 Phase 1: Local Infrastructure Setup (Week 1)

### Day 1-2: Docker Environment

```
📦 docker-compose.yml
├── PostgreSQL (port 5432)
├── TimescaleDB (port 5433)
├── Kafka + Zookeeper (port 9092)
├── Redis (port 6379)
├── Kafka UI (port 8090)
└── pgAdmin (optional - DB UI)
```

### Day 3-4: Project Structure Setup

Create the base project structure and configure Maven/Gradle multi-module project.

---

## 📅 Phase 2: Core Services Development (Week 2-4)

### Service Build Priority (In This Sequence)

| Order | Service | Why First? |
|-------|---------|------------|
| 1 | **Gateway Service** | Entry point, routing |
| 2 | **Auth Service** | JWT, 2FA, security foundation |
| 3 | **User Service** | Profile, KYC management |
| 4 | **Market Data Service** | Real-time prices, WebSocket |
| 5 | **Wallet Service** | Funds management |
| 6 | **Risk Service** | Margin checks, limits |
| 7 | **Order Service** | Order lifecycle |
| 8 | **Matching Engine** | Trade execution (core logic) |
| 9 | **Portfolio Service** | Holdings, P&L |
| 10 | **Notification Service** | Alerts, confirmations |

### Each Service Structure

```
📁 order-service/
├── src/main/java/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   ├── dto/
│   ├── kafka/
│   │   ├── producer/
│   │   └── consumer/
│   ├── websocket/
│   └── config/
│       └── SwaggerConfig.java
├── src/main/resources/
│   ├── application.yml
│   └── application-local.yml
├── Dockerfile
└── pom.xml
```

---

### 📖 Swagger/OpenAPI Integration (Implement in Phase 2)

> **When to implement:** Add Swagger to each service as you build it during Phase 2. This ensures API documentation is always up-to-date.

#### Step 1: Add SpringDoc Dependency (pom.xml)

```xml
<!-- For Spring Boot 3.x -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

#### Step 2: Configure Swagger (application.yml)

```yaml
springdoc:
  api-docs:
    enabled: true
    path: /api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
  info:
    title: ${spring.application.name}
    version: 1.0.0
    description: API Documentation for ${spring.application.name}
```

#### Step 3: Create SwaggerConfig.java

```java
package com.trading.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Trading Platform API")
                .version("1.0.0")
                .description("REST API for Trading Platform")
                .contact(new Contact()
                    .name("Your Name")
                    .email("your.email@example.com"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local"),
                new Server().url("https://api.yourdomain.com").description("Production")
            ));
    }
}
```

#### Step 4: Annotate Controllers

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order", description = "Order management APIs")
public class OrderController {

    @Operation(summary = "Place a new order", description = "Places a buy/sell order for a symbol")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Order placed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid order request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "422", description = "Insufficient funds/margin")
    })
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @Parameter(description = "Order details") @RequestBody OrderRequest request) {
        // implementation
    }

    @Operation(summary = "Get order by ID")
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "Order ID", required = true) @PathVariable String orderId) {
        // implementation
    }
}
```

#### Step 5: Annotate DTOs

```java
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Order Request DTO")
public class OrderRequest {

    @Schema(description = "Trading symbol", example = "AAPL", required = true)
    private String symbol;

    @Schema(description = "Order type", example = "LIMIT", allowableValues = {"MARKET", "LIMIT", "STOP_LOSS"})
    private OrderType orderType;

    @Schema(description = "Order side", example = "BUY", allowableValues = {"BUY", "SELL"})
    private OrderSide side;

    @Schema(description = "Quantity", example = "100", required = true)
    private Integer quantity;

    @Schema(description = "Limit price (required for LIMIT orders)", example = "150.50")
    private BigDecimal price;
}
```

#### Swagger UI URLs (Per Service)

| Service | Swagger UI URL | Port |
|---------|---------------|------|
| Gateway | http://localhost:8080/swagger-ui.html | 8080 |
| Auth | http://localhost:8081/swagger-ui.html | 8081 |
| User | http://localhost:8082/swagger-ui.html | 8082 |
| Market Data | http://localhost:8083/swagger-ui.html | 8083 |
| Order | http://localhost:8084/swagger-ui.html | 8084 |
| Matching Engine | http://localhost:8085/swagger-ui.html | 8085 |
| Portfolio | http://localhost:8086/swagger-ui.html | 8086 |
| Wallet | http://localhost:8087/swagger-ui.html | 8087 |
| Risk | http://localhost:8088/swagger-ui.html | 8088 |
| Notification | http://localhost:8089/swagger-ui.html | 8089 |

#### 🌐 Aggregated Swagger via Gateway (Optional - Advanced)

To view all service APIs from a single Swagger UI through the Gateway:

```yaml
# gateway-service application.yml
springdoc:
  swagger-ui:
    urls:
      - name: Auth Service
        url: /auth-service/api-docs
      - name: User Service
        url: /user-service/api-docs
      - name: Order Service
        url: /order-service/api-docs
      - name: Market Data Service
        url: /market-data-service/api-docs
      - name: Portfolio Service
        url: /portfolio-service/api-docs
```

This allows accessing all microservice documentation from: `http://localhost:8080/swagger-ui.html`

---

## 📅 Phase 3: Kafka Event Integration (Week 5)

### Kafka Topics

```
📨 Topics:
├── order-events           (OrderCreated, OrderModified, OrderCancelled)
├── trade-events           (TradeExecuted, TradeSettled)
├── price-events           (PriceUpdated, MarketOpen, MarketClose)
├── portfolio-events       (PositionUpdated, PnLCalculated)
├── wallet-events          (FundsDeposited, FundsWithdrawn, FundsHeld)
├── risk-events            (MarginCall, PositionLimitBreached)
├── notification-events    (SendAlert, SendConfirmation)
└── user-events            (UserCreated, KYCApproved)
```

### Event Flow Architecture

```
Order Service                    Kafka                       Consumers
     │                             │                              │
     ├── OrderCreated ──────────► order-events ──────────► Matching Engine
     │                             │                         Risk Service
     │                             │                              │
     │                             │                              │
Matching Engine                    │                              │
     │                             │                              │
     ├── TradeExecuted ─────────► trade-events ──────────► Portfolio Service
     │                             │                         Wallet Service
     │                             │                         Notification Service
     │                             │                              │
Risk Service                       │                              │
     │                             │                              │
     ├── MarginCall ────────────► risk-events ───────────► Notification Service
     │                             │                         Order Service (force close)
```

---

## 📅 Phase 4: Real-time Features (Week 6)

### WebSocket Implementation

- Real-time price streaming
- Order book updates
- Portfolio value updates
- Trade notifications

### Price Alert System

- User-defined price triggers
- Push notifications
- Email alerts

---

## 📅 Phase 5: Testing & Integration (Week 7)

### Testing Checklist

- [ ] Unit tests for each service
- [ ] Integration tests with TestContainers
- [ ] End-to-end order → trade flow testing
- [ ] Kafka consumer/producer testing
- [ ] WebSocket connection testing
- [ ] Load testing with K6 or JMeter
- [ ] Matching engine performance testing
- [ ] Swagger UI verification for all services
- [ ] API contract testing via OpenAPI specs

### Local Observability Stack

Add to `docker-compose.yml`:
- **Prometheus** — metrics collection
- **Grafana** — dashboards (orders/sec, latency, P&L)
- **Jaeger** — distributed tracing
- **ELK Stack** — logs (optional)

---

## 📅 Phase 6: Containerization (Week 8)

### Dockerfile Template

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose for All Services

```yaml
# docker-compose.services.yml
services:
  gateway:
    build: ./services/gateway-service
    ports: ["8080:8080"]
    depends_on: [auth-service]
    
  auth-service:
    build: ./services/auth-service
    depends_on: [postgres, redis]
    
  order-service:
    build: ./services/order-service
    depends_on: [postgres, kafka]
    
  matching-engine:
    build: ./services/matching-engine
    depends_on: [kafka, redis]
    
  market-data-service:
    build: ./services/market-data-service
    depends_on: [redis, kafka]
    
  # ... other services
```

---

## 📅 Phase 7: AWS Deployment (Week 9-10)

### Terraform Structure

```
📁 terraform/
├── modules/
│   ├── vpc/
│   ├── eks/
│   ├── rds/
│   ├── elasticache/
│   ├── msk/           # Managed Kafka
│   └── secrets-manager/
├── environments/
│   ├── dev/
│   └── prod/
└── main.tf
```

### AWS Services Mapping

| Local | AWS Equivalent |
|-------|---------------|
| PostgreSQL | RDS (PostgreSQL/Aurora) |
| TimescaleDB | TimescaleDB on EC2 |
| Kafka | MSK (Managed Kafka) |
| Redis | ElastiCache |
| Docker | EKS (Kubernetes) |

### Kubernetes Manifests

```
📁 kubernetes/
├── namespaces/
├── configmaps/
├── secrets/
├── deployments/
│   ├── gateway-deployment.yml
│   ├── auth-deployment.yml
│   ├── order-deployment.yml
│   ├── matching-engine-deployment.yml
│   └── ... (each service)
├── services/
├── ingress/
└── hpa/    # Auto-scaling
```

---

## 📅 Complete Timeline Summary

| Week | Phase | Focus | Deliverable |
|------|-------|-------|-------------|
| 1 | Phase 1 | Local infra + project setup | Docker Compose running |
| 2 | Phase 2 | Gateway + Auth + User services + **Swagger** | Basic auth working |
| 3 | Phase 2 | Market Data + Wallet + Risk services + **Swagger** | Core services ready |
| 4 | Phase 2 | Order + Matching Engine + Portfolio + **Swagger** | Trading flow working |
| 5 | Phase 3 | Kafka integration | Event-driven flow |
| 6 | Phase 4 | WebSocket + Real-time features | Live price streaming |
| 7 | Phase 5 | Testing + Notification service | Complete local system |
| 8 | Phase 6 | Dockerize all services | All containers running |
| 9 | Phase 7 | Terraform + AWS setup | Infra provisioned |
| 10 | Phase 7 | Deploy to EKS + CI/CD | Production ready |

---

## 🚀 Quick Start Commands

```bash
# 1. Create project structure
mkdir trading-platform
cd trading-platform
mkdir -p infrastructure services common deployment

# 2. Start local infrastructure
cd infrastructure
docker-compose up -d

# 3. Verify services are running
docker-compose ps

# 4. View Kafka UI
open http://localhost:8090

# 5. Connect to PostgreSQL
psql -h localhost -p 5432 -U postgres

# 6. Access Swagger UI (after services are running)
open http://localhost:8080/swagger-ui.html  # Gateway (aggregated)
open http://localhost:8084/swagger-ui.html  # Order Service
```

---

## 📚 Documentation

- [Producer Service Documentation](./README-PRODUCER.md)
- [Deployment Guide](./README-DEPLOYMENT.md)

---

## 🤝 Contributing

*Coming soon...*

## 📄 License

*Coming soon...*
