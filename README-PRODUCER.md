# 🛒 E-Commerce Price Tracker - Kafka Producer Guide

A step-by-step guide to building a Spring Boot Kafka Producer that simulates real-time price and inventory updates.

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Project Setup](#project-setup)
3. [Kafka Configuration](#kafka-configuration)
4. [Creating the Message Model](#creating-the-message-model)
5. [Building the Producer Service](#building-the-producer-service)
6. [Simulating Events](#simulating-events)
7. [Testing Your Producer](#testing-your-producer)
8. [Production Considerations](#production-considerations)

---

## Prerequisites

Before you begin, ensure you have:

- **Java 17+** installed
- **Maven** or **Gradle** for dependency management
- **Apache Kafka** running locally (or Docker)
- **IDE** (IntelliJ IDEA recommended)

### Quick Kafka Setup with Docker

```bash
# Start Zookeeper and Kafka using Docker Compose
docker-compose up -d
```

Example `docker-compose.yml`:
```yaml
version: '3'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
```

---

## Project Setup

### Step 1: Initialize Spring Boot Project

Use [Spring Initializr](https://start.spring.io/) or create manually.

**Required Dependencies:**
- Spring Web
- Spring for Apache Kafka
- Lombok (optional, reduces boilerplate)
- Spring Boot DevTools (optional)

### Step 2: Add Maven Dependencies

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Kafka -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    
    <!-- JSON Processing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
    
    <!-- Lombok (optional) -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## Kafka Configuration

### Step 3: Configure Application Properties

```yaml
# application.yml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3

# Custom properties
app:
  kafka:
    topic:
      price-updates: product-price-updates
      inventory-updates: product-inventory-updates
```

### Step 4: Create Kafka Configuration Class

Create a configuration class that defines:
- **Producer Factory** - Creates producer instances
- **Kafka Template** - Spring's abstraction for sending messages
- **Topic beans** - Auto-create topics on startup

```java
@Configuration
public class KafkaProducerConfig {
    
    // Define ProducerFactory bean
    // Configure with bootstrap servers and serializers
    
    // Define KafkaTemplate bean
    // Inject the ProducerFactory
    
    // Define NewTopic beans for auto-creation
    // Specify partitions and replication factor
}
```

**Key Concepts:**
- `ProducerFactory<K, V>` - Factory for creating Kafka producers
- `KafkaTemplate<K, V>` - Thread-safe template for publishing messages
- `NewTopic` - Declaratively creates topics if they don't exist

---

## Creating the Message Model

### Step 5: Define the Event DTOs

Create POJO classes representing your Kafka messages.

**Price Update Event Structure:**
```java
public class PriceUpdateEvent {
    private String productId;      // Unique product identifier
    private String vendorId;       // Vendor/seller ID
    private BigDecimal oldPrice;   // Previous price
    private BigDecimal newPrice;   // Updated price
    private String currency;       // e.g., "USD", "EUR"
    private LocalDateTime timestamp;
    private String eventType;      // "PRICE_DROP", "PRICE_INCREASE"
    
    // Constructors, getters, setters
}
```

**Inventory Update Event Structure:**
```java
public class InventoryUpdateEvent {
    private String productId;
    private String vendorId;
    private Integer stockLevel;
    private String stockStatus;    // "IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"
    private LocalDateTime timestamp;
    
    // Constructors, getters, setters
}
```

**💡 Tip:** Use Lombok's `@Data`, `@Builder` annotations to reduce boilerplate.

---

## Building the Producer Service

### Step 6: Create the Producer Service

This service encapsulates all Kafka publishing logic.

```java
@Service
public class PriceUpdateProducer {
    
    // Inject KafkaTemplate
    
    // Method: sendPriceUpdate(PriceUpdateEvent event)
    //   - Use kafkaTemplate.send(topic, key, value)
    //   - Key = productId (ensures ordering per product)
    //   - Add callback for success/failure handling
    
    // Method: sendInventoryUpdate(InventoryUpdateEvent event)
    //   - Similar pattern
}
```

**Key Patterns:**

1. **Synchronous Send** (wait for acknowledgment):
```java
kafkaTemplate.send(topic, key, message).get();
```

2. **Asynchronous Send** (fire and forget with callback):
```java
CompletableFuture<SendResult<String, Object>> future = 
    kafkaTemplate.send(topic, key, message);

future.whenComplete((result, ex) -> {
    if (ex == null) {
        // Success handling
    } else {
        // Error handling
    }
});
```

3. **Using ProducerRecord** (more control):
```java
ProducerRecord<String, Object> record = new ProducerRecord<>(
    topic, partition, key, value, headers
);
kafkaTemplate.send(record);
```

---

## Simulating Events

### Step 7: Create Event Simulator

Build a component that generates realistic price fluctuations.

**Option A: Scheduled Task**
```java
@Component
public class PriceSimulator {
    
    // Inject PriceUpdateProducer
    
    // @Scheduled(fixedRate = 1000)  // Every second
    // public void generatePriceUpdates() {
    //     - Pick random product from catalog
    //     - Generate price change (+/- random percentage)
    //     - Build PriceUpdateEvent
    //     - Send via producer
    // }
}
```

**Option B: REST Endpoint for Manual Triggers**
```java
@RestController
@RequestMapping("/api/simulate")
public class SimulationController {
    
    // POST /api/simulate/price-update
    // - Accept event details or generate random
    // - Publish to Kafka
    // - Return confirmation
    
    // POST /api/simulate/bulk
    // - Generate N random events
    // - Useful for load testing
}
```

**Option C: Batch Generator for Load Testing**
```java
@Component
public class BulkEventGenerator {
    
    // Method: generateBulkUpdates(int count, Duration duration)
    //   - Spread N events over specified duration
    //   - Use ScheduledExecutorService or Project Reactor
}
```

### Sample Product Catalog

Create a static list of products to simulate:

```java
public class ProductCatalog {
    public static final List<Product> PRODUCTS = List.of(
        new Product("PROD-001", "Wireless Headphones", 79.99),
        new Product("PROD-002", "Mechanical Keyboard", 149.99),
        new Product("PROD-003", "4K Monitor", 399.99)
        // ... more products
    );
    
    public static Product getRandomProduct() {
        // Return random product from list
    }
}
```

---

## Testing Your Producer

### Step 8: Unit Testing

```java
@SpringBootTest
class PriceUpdateProducerTest {
    
    @Autowired
    private PriceUpdateProducer producer;
    
    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Test
    void shouldSendPriceUpdateEvent() {
        // Arrange: Create test event
        // Act: Call producer.sendPriceUpdate(event)
        // Assert: Verify kafkaTemplate.send() was called
    }
}
```

### Step 9: Integration Testing with Embedded Kafka

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"product-price-updates"})
class KafkaIntegrationTest {
    
    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;
    
    @Test
    void shouldPublishAndConsumeMessage() {
        // Publish message
        // Consume with test consumer
        // Verify message content
    }
}
```

### Step 10: Manual Testing with Kafka CLI

```bash
# List topics
kafka-topics --list --bootstrap-server localhost:9092

# Consume messages from topic (for verification)
kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic product-price-updates \
    --from-beginning \
    --property print.key=true
```

---

## Production Considerations

### Message Schema Evolution

Consider using **Apache Avro** with **Schema Registry** for:
- Schema validation
- Backward/forward compatibility
- Smaller message sizes

### Error Handling

Implement proper error handling:
```java
// Configure error handler
// Implement retry logic
// Dead letter topic for failed messages
```

### Monitoring

Add observability:
- **Micrometer metrics** for producer stats
- **Structured logging** for traceability
- **Health indicators** for Kafka connectivity

### Configuration for High Throughput

```yaml
spring:
  kafka:
    producer:
      batch-size: 16384          # Batch messages
      linger-ms: 5               # Wait for batch to fill
      buffer-memory: 33554432    # 32MB buffer
      compression-type: snappy   # Compress messages
```

---

## Project Structure

```
src/main/java/com/example/pricetracker/
├── config/
│   └── KafkaProducerConfig.java
├── model/
│   ├── PriceUpdateEvent.java
│   └── InventoryUpdateEvent.java
├── producer/
│   └── PriceUpdateProducer.java
├── simulator/
│   ├── PriceSimulator.java
│   └── ProductCatalog.java
├── controller/
│   └── SimulationController.java
└── PriceTrackerApplication.java
```

---

## Next Steps

Once your producer is working:

1. ✅ **Verify messages** are arriving in Kafka using console consumer
2. 🔜 **Set up AWS Lambda** consumer to process messages
3. 🔜 **Configure S3** for raw event storage
4. 🔜 **Set up DynamoDB** for current state storage
5. 🔜 **Build React dashboard** to visualize data

---

## Useful Resources

- [Spring for Apache Kafka Documentation](https://docs.spring.io/spring-kafka/docs/current/reference/html/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Confluent Kafka Tutorials](https://developer.confluent.io/)

---

*Happy Streaming! 🚀*

