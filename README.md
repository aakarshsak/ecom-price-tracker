# 🚀 Cloud-Native Event-Driven Order Management Platform

A production-grade microservices system similar to what companies like **Amazon**, **Razorpay**, **Swiggy**, or **Flipkart** run internally.

---

## 🎯 Core Principles (Important for Interviews)

| Principle | Status |
|-----------|--------|
| No monolith — microservices from Day 1 | ❌ |
| Event-driven (async first) | ✅ |
| Container-first (Docker + EKS) | ✅ |
| Infra as Code | ✅ |
| Observability + security baked in | ✅ |

---

## 🧱 Microservices Breakdown (Spring Boot)

| Service | Responsibility | DB |
|---------|---------------|-----|
| **Auth Service** | User auth, JWT, roles | RDS |
| **User Service** | Profile & preferences | RDS |
| **Catalog Service** | Products, pricing | DynamoDB |
| **Order Service** | Order lifecycle | RDS |
| **Payment Service** | Payment orchestration | RDS |
| **Inventory Service** | Stock mgmt | DynamoDB |
| **Notification Service** | Email/SMS/Push | — |
| **Analytics Service** | Event processing | Kinesis |
| **Gateway Service** | Routing, auth filter | — |

---

## 🗺️ AWS Architecture (Microservices-Only)

```
                        Client
                          |
                    API Gateway (AWS)
                          |
                Spring Cloud Gateway (EKS)
                          |
    ---------------------------------------------------------
    | Auth | User | Catalog | Order | Payment | Inventory  |
    ---------------------------------------------------------
                          |
                  EventBridge / SNS
                          |
            Notification | Analytics | Lambda
```

---

## 🔧 AWS Services — Purpose Mapping

### 🖥 Compute & Containers

#### EC2
- Worker nodes for EKS
- Auto Scaling Groups

#### EKS (Primary)
- Runs all Spring Boot microservices
- HPA for traffic spikes

#### ECS + Fargate
- Notification service (serverless containers)

#### Elastic Beanstalk (Optional)
- ❌ Skip deployment
- ✅ Use only to understand config, not architecture

---

### 🗄 Databases & Storage

#### RDS (MySQL/Postgres)
- Transactions
- Orders
- Users

#### DynamoDB
- Catalog
- Inventory (high write throughput)

#### S3
- Invoices
- Logs
- Static assets

---

### ⚡ Cache

#### Redis (ElastiCache)
- Product cache
- JWT blacklist
- Rate limiting

---

### 🌐 Networking & Security

#### VPC
- **Public:** ALB, NAT
- **Private:** EKS, RDS, Redis

#### IAM
- IRSA for pods
- Least privilege

---

### 📩 Messaging & Streaming

#### SQS
- Order processing queue
- Payment retries

#### SNS
- Order events fan-out

#### EventBridge
- Domain event routing
- Decoupling services

#### Kinesis
- Analytics stream

---

### 🧠 Serverless

#### Lambda
- Invoice generation
- Image resize
- Async email trigger

---

### 🔍 Observability & Auditing

#### CloudWatch
- Logs
- Alarms
- Dashboards

#### X-Ray
- Distributed tracing across services

#### CloudTrail
- Audit AWS API calls

---

### 🔄 CI/CD

#### CodePipeline
- Git → Build → Deploy

#### CodeBuild
- Test + Docker image

#### CodeDeploy
- Blue-Green deployment (EKS)

---

### 🏗 Infrastructure as Code

#### Terraform (Recommended)
- VPC
- EKS
- RDS
- IAM
- Redis
- SQS/SNS

#### OR

#### AWS CDK
- Java-friendly option

---

## 🧪 Example Event Flow (Interview-Gold)

### Order Placement Flow

```
1. Client → API Gateway → Order Service
2. Order saved in RDS
3. OrderCreated event → SNS/EventBridge
4. Inventory Service consumes → updates stock
5. Payment Service consumes → charges user
6. Notification Service sends confirmation
7. Analytics Service streams to Kinesis
```

---

## 🧾 Resume-Ready Description

> Designed and implemented a cloud-native, event-driven microservices platform using **Java 17 & Spring Boot**, deployed on **AWS EKS**, leveraging **Redis**, **RDS**, **DynamoDB**, **SQS**, **SNS**, **EventBridge**, **Lambda**, and **Terraform**, with full **CI/CD** and distributed tracing via **CloudWatch & X-Ray**.

---

## 💡 Why This Project Is Strong

| Strength | ✔ |
|----------|---|
| No monolith | ✔ |
| Real AWS usage | ✔ |
| Clear microservice boundaries | ✔ |
| Event-driven | ✔ |
| Resume + interview friendly | ✔ |

---

# 📆 Development Plan: Local First → AWS Deployment

## 🎯 Strategy: Build Locally, Deploy to Cloud

**Local-first development** is the smart approach — it's faster, cheaper, and easier to debug.

---

## 📁 Project Structure

```
📁 order-management-platform/
├── 📁 infrastructure/
│   ├── docker-compose.yml
│   ├── docker-compose.prod.yml
│   └── init-scripts/
├── 📁 services/
│   ├── gateway-service/
│   ├── auth-service/
│   ├── user-service/
│   ├── catalog-service/
│   ├── order-service/
│   ├── payment-service/
│   ├── inventory-service/
│   └── notification-service/
├── 📁 common/
│   ├── common-dto/
│   └── common-events/
└── 📁 deployment/
    ├── terraform/
    └── kubernetes/
```

---

## 🛠️ Tech Stack: Local vs AWS

| Layer | Local | AWS |
|-------|-------|-----|
| **Runtime** | Docker Compose | EKS |
| **Database** | MySQL (Docker) | RDS MySQL |
| **Messaging** | Kafka (Docker) | MSK |
| **Cache** | Redis (Docker) | ElastiCache |
| **Gateway** | Spring Cloud Gateway | Same + ALB |
| **Secrets** | .env files | AWS Secrets Manager |
| **Logs** | Console/File | CloudWatch |
| **Tracing** | Jaeger | X-Ray |
| **API Docs** | Swagger UI (SpringDoc) | Same |

---

## 📅 Phase 1: Local Infrastructure Setup (Week 1)

### Day 1-2: Docker Environment

```
📦 docker-compose.yml
├── MySQL (port 3306)
├── Kafka + Zookeeper (port 9092)
├── Redis (port 6379)
├── Kafka UI (for debugging)
└── phpMyAdmin/Adminer (optional - DB UI)
```

### Day 3-4: Project Structure Setup

Create the base project structure and configure Maven/Gradle multi-module project.

---

## 📅 Phase 2: Core Services Development (Week 2-3)

### Service Build Priority (In This Sequence)

| Order | Service | Why First? |
|-------|---------|------------|
| 1 | **Gateway Service** | Entry point, routing |
| 2 | **Auth Service** | JWT, security foundation |
| 3 | **User Service** | Profile management |
| 4 | **Catalog Service** | Products (read-heavy) |
| 5 | **Inventory Service** | Stock management |
| 6 | **Order Service** | Core business logic |
| 7 | **Payment Service** | Payment orchestration |
| 8 | **Notification Service** | Async notifications |

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
package com.order.config;

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
                .title("Order Service API")
                .version("1.0.0")
                .description("REST API for Order Management")
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

    @Operation(summary = "Create a new order", description = "Creates a new order and returns order details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Order created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Parameter(description = "Order details") @RequestBody OrderRequest request) {
        // implementation
    }

    @Operation(summary = "Get order by ID")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "Order ID", required = true) @PathVariable Long id) {
        // implementation
    }
}
```

#### Step 5: Annotate DTOs

```java
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Order Request DTO")
public class OrderRequest {

    @Schema(description = "Customer ID", example = "12345", required = true)
    private Long customerId;

    @Schema(description = "List of order items", required = true)
    private List<OrderItemRequest> items;

    @Schema(description = "Shipping address", example = "123 Main St, City")
    private String shippingAddress;
}
```

#### Swagger UI URLs (Per Service)

| Service | Swagger UI URL | API Docs URL |
|---------|---------------|--------------|
| Gateway | http://localhost:8080/swagger-ui.html | http://localhost:8080/api-docs |
| Auth | http://localhost:8081/swagger-ui.html | http://localhost:8081/api-docs |
| User | http://localhost:8082/swagger-ui.html | http://localhost:8082/api-docs |
| Catalog | http://localhost:8083/swagger-ui.html | http://localhost:8083/api-docs |
| Order | http://localhost:8084/swagger-ui.html | http://localhost:8084/api-docs |
| Payment | http://localhost:8085/swagger-ui.html | http://localhost:8085/api-docs |
| Inventory | http://localhost:8086/swagger-ui.html | http://localhost:8086/api-docs |
| Notification | http://localhost:8087/swagger-ui.html | http://localhost:8087/api-docs |

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
      - name: Catalog Service
        url: /catalog-service/api-docs
```

This allows accessing all microservice documentation from: `http://localhost:8080/swagger-ui.html`

---

## 📅 Phase 3: Kafka Event Integration (Week 4)

### Kafka Topics

```
📨 Topics:
├── order-events        (OrderCreated, OrderUpdated, OrderCancelled)
├── payment-events      (PaymentInitiated, PaymentSuccess, PaymentFailed)
├── inventory-events    (StockUpdated, StockLow)
├── notification-events (SendEmail, SendSMS)
└── user-events         (UserCreated, UserUpdated)
```

### Event Flow Architecture

```
Order Service                    Kafka                     Consumers
     │                             │                           │
     ├── OrderCreated ──────────► order-events ──────────► Inventory Service
     │                             │                       Payment Service
     │                             │                       Notification Service
     │                             │                           │
     │◄─── PaymentSuccess ◄────── payment-events ◄──────── Payment Service
     │                             │                           │
     ├── OrderConfirmed ────────► order-events ──────────► Notification Service
```

---

## 📅 Phase 4: Local Testing & Integration (Week 5)

### Testing Checklist

- [ ] Unit tests for each service
- [ ] Integration tests with TestContainers
- [ ] End-to-end flow testing
- [ ] Kafka consumer/producer testing
- [ ] Load testing with K6 or JMeter
- [ ] Swagger UI verification for all services
- [ ] API contract testing via OpenAPI specs

### Local Observability Stack

Add to `docker-compose.yml`:
- **Prometheus** — metrics collection
- **Grafana** — dashboards
- **Jaeger** — distributed tracing
- **ELK Stack** — logs (optional)

---

## 📅 Phase 5: Containerization (Week 6)

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
    depends_on: [mysql, redis]
    
  order-service:
    build: ./services/order-service
    depends_on: [mysql, kafka]
    
  # ... other services
```

---

## 📅 Phase 6: AWS Deployment (Week 7-8)

### Terraform Structure

```
📁 terraform/
├── modules/
│   ├── vpc/
│   ├── eks/
│   ├── rds/
│   ├── elasticache/
│   └── msk/          # Managed Kafka
├── environments/
│   ├── dev/
│   └── prod/
└── main.tf
```

### AWS Services Mapping

| Local | AWS Equivalent |
|-------|---------------|
| MySQL | RDS (MySQL/Aurora) |
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
| 2 | Phase 2 | Gateway + Auth + User services + **Swagger** | Basic auth working + API docs |
| 3 | Phase 2 | Catalog + Inventory + Order services + **Swagger** | Core flow working + API docs |
| 4 | Phase 3 | Kafka integration | Event-driven flow |
| 5 | Phase 4 | Payment + Notification + Testing | Complete local system |
| 6 | Phase 5 | Dockerize all services | All containers running |
| 7 | Phase 6 | Terraform + AWS setup | Infra provisioned |
| 8 | Phase 6 | Deploy to EKS + CI/CD | Production ready |

---

## 🚀 Quick Start Commands

```bash
# 1. Create project structure
mkdir order-management-platform
cd order-management-platform
mkdir -p infrastructure services common deployment

# 2. Start local infrastructure
cd infrastructure
docker-compose up -d

# 3. Verify services are running
docker-compose ps

# 4. View Kafka UI
open http://localhost:8090

# 5. Connect to MySQL
mysql -h localhost -P 3306 -u root -p

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
