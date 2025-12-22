# ⚡ E-Commerce Price Tracker - AWS Lambda Consumer Guide

A step-by-step guide to building an AWS Lambda function that consumes Kafka messages and processes price updates.

---

## 📋 Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Prerequisites](#prerequisites)
3. [Project Setup](#project-setup)
4. [Lambda Handler Implementation](#lambda-handler-implementation)
5. [S3 Integration (Data Lake)](#s3-integration-data-lake)
6. [DynamoDB Integration (Current State)](#dynamodb-integration-current-state)
7. [MSK/Kafka Trigger Configuration](#mskkafka-trigger-configuration)
8. [Deployment](#deployment)
9. [Testing](#testing)
10. [Monitoring & Troubleshooting](#monitoring--troubleshooting)

---

## Architecture Overview

```
┌──────────────┐      ┌─────────────┐      ┌──────────────────────────────┐
│   Kafka      │─────►│ AWS Lambda  │─────►│  S3 (Raw Events/Data Lake)  │
│   Topic      │      │  Consumer   │      └──────────────────────────────┘
└──────────────┘      │             │      ┌──────────────────────────────┐
                      │  - Validate │─────►│  DynamoDB (Current State)   │
                      │  - Transform│      └──────────────────────────────┘
                      └─────────────┘
```

**Lambda Responsibilities:**
1. **Validate** incoming price update events
2. **Write raw events** to S3 for historical auditing
3. **Update current state** in DynamoDB (latest price per product)

---

## Prerequisites

Before you begin, ensure you have:

- **AWS Account** with appropriate permissions
- **AWS CLI** configured (`aws configure`)
- **Java 17 or 21** installed
- **Maven** for dependency management
- **Kafka cluster** (Amazon MSK or self-managed)

### Required AWS Services

| Service | Purpose |
|---------|---------|
| Lambda | Event processing |
| S3 | Raw event storage (Data Lake) |
| DynamoDB | Current state storage |
| MSK (or self-managed Kafka) | Message streaming |
| IAM | Permissions |
| CloudWatch | Logging & monitoring |

---

## Project Setup

### Step 1: Create Maven Project

Use a standard Maven project structure for Lambda:

```
lambda-consumer/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── com/example/pricetracker/
                ├── handler/
                │   └── PriceUpdateHandler.java
                ├── model/
                │   └── PriceUpdateEvent.java
                ├── service/
                │   ├── ValidationService.java
                │   ├── S3Service.java
                │   └── DynamoDBService.java
                └── util/
                    └── JsonUtil.java
```

### Step 2: Add Maven Dependencies

```xml
<properties>
    <java.version>17</java.version>
    <aws.sdk.version>2.21.0</aws.sdk.version>
</properties>

<dependencies>
    <!-- AWS Lambda Core -->
    <dependency>
        <groupId>com.amazonaws</groupId>
        <artifactId>aws-lambda-java-core</artifactId>
        <version>1.2.3</version>
    </dependency>

    <!-- AWS Lambda Events (for Kafka trigger) -->
    <dependency>
        <groupId>com.amazonaws</groupId>
        <artifactId>aws-lambda-java-events</artifactId>
        <version>3.11.4</version>
    </dependency>

    <!-- AWS SDK v2 - S3 -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
        <version>${aws.sdk.version}</version>
    </dependency>

    <!-- AWS SDK v2 - DynamoDB -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>dynamodb</artifactId>
        <version>${aws.sdk.version}</version>
    </dependency>

    <!-- AWS SDK v2 - DynamoDB Enhanced Client -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>dynamodb-enhanced</artifactId>
        <version>${aws.sdk.version}</version>
    </dependency>

    <!-- JSON Processing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.2</version>
    </dependency>

    <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
        <version>2.15.2</version>
    </dependency>

    <!-- Logging -->
    <dependency>
        <groupId>com.amazonaws</groupId>
        <artifactId>aws-lambda-java-log4j2</artifactId>
        <version>1.6.0</version>
    </dependency>
</dependencies>

<!-- Build plugin to create uber JAR -->
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.5.0</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

---

## Lambda Handler Implementation

### Step 3: Create the Event Model

```java
public class PriceUpdateEvent {
    private String productId;
    private String vendorId;
    private BigDecimal oldPrice;
    private BigDecimal newPrice;
    private String currency;
    private String eventType;
    private LocalDateTime timestamp;
    
    // Getters, setters, constructors
}
```

### Step 4: Create the Lambda Handler

The handler receives Kafka events and processes them:

```java
public class PriceUpdateHandler implements RequestHandler<KafkaEvent, String> {

    // Initialize services (reused across invocations)
    // private final ValidationService validationService;
    // private final S3Service s3Service;
    // private final DynamoDBService dynamoDBService;
    // private final ObjectMapper objectMapper;

    @Override
    public String handleRequest(KafkaEvent kafkaEvent, Context context) {
        // Step 1: Extract records from Kafka event
        // kafkaEvent.getRecords() returns Map<String, List<KafkaEventRecord>>
        
        // Step 2: Loop through each record
        //   - Decode Base64 value
        //   - Deserialize JSON to PriceUpdateEvent
        //   - Validate the event
        //   - Write to S3 (raw event)
        //   - Update DynamoDB (current state)
        
        // Step 3: Return success/failure summary
        return "Processed X records successfully";
    }
}
```

### Step 5: Processing Kafka Records

```java
// Kafka records come as Base64 encoded
private void processRecords(KafkaEvent kafkaEvent) {
    for (Map.Entry<String, List<KafkaEvent.KafkaEventRecord>> entry : 
            kafkaEvent.getRecords().entrySet()) {
        
        String topicPartition = entry.getKey();  // e.g., "topic-0"
        
        for (KafkaEvent.KafkaEventRecord record : entry.getValue()) {
            // Decode the message value (Base64)
            // String value = new String(Base64.getDecoder().decode(record.getValue()));
            
            // Deserialize to PriceUpdateEvent
            // PriceUpdateEvent event = objectMapper.readValue(value, PriceUpdateEvent.class);
            
            // Process the event...
        }
    }
}
```

---

## S3 Integration (Data Lake)

### Step 6: Create S3 Service

Store raw events for historical auditing and analytics.

```java
public class S3Service {
    
    // Initialize S3Client
    // private final S3Client s3Client;
    // private final String bucketName;

    // Method: writeRawEvent(PriceUpdateEvent event)
    //   - Create S3 key with date partitioning:
    //     raw/year=2025/month=12/day=19/hour=14/{eventId}.json
    //   - Convert event to JSON
    //   - Use PutObjectRequest to upload
}
```

### S3 Key Structure (Partitioned by Date)

```
s3://price-tracker-data-lake/
└── raw/
    └── year=2025/
        └── month=12/
            └── day=19/
                └── hour=14/
                    ├── event-uuid-1.json
                    ├── event-uuid-2.json
                    └── event-uuid-3.json
```

**Why partition by date?**
- Efficient queries with Athena
- Cost-effective lifecycle policies
- Easy to archive old data

### S3 Upload Pattern

```java
// Key pattern for partitioned storage
private String generateS3Key(PriceUpdateEvent event) {
    LocalDateTime ts = event.getTimestamp();
    return String.format("raw/year=%d/month=%02d/day=%02d/hour=%02d/%s-%s.json",
        ts.getYear(),
        ts.getMonthValue(),
        ts.getDayOfMonth(),
        ts.getHour(),
        event.getProductId(),
        UUID.randomUUID()
    );
}

// Upload to S3
// s3Client.putObject(
//     PutObjectRequest.builder()
//         .bucket(bucketName)
//         .key(s3Key)
//         .contentType("application/json")
//         .build(),
//     RequestBody.fromString(jsonContent)
// );
```

---

## DynamoDB Integration (Current State)

### Step 7: Create DynamoDB Table

**Table Design:**

| Attribute | Type | Description |
|-----------|------|-------------|
| `PK` (Partition Key) | String | `PRODUCT#{productId}` |
| `SK` (Sort Key) | String | `VENDOR#{vendorId}` |
| `currentPrice` | Number | Latest price |
| `currency` | String | Currency code |
| `lastUpdated` | String | ISO timestamp |
| `priceDirection` | String | `UP`, `DOWN`, `STABLE` |
| `ttl` | Number | Unix timestamp for expiry |

### Create Table via AWS CLI

```bash
aws dynamodb create-table \
    --table-name PriceTracker \
    --attribute-definitions \
        AttributeName=PK,AttributeType=S \
        AttributeName=SK,AttributeType=S \
    --key-schema \
        AttributeName=PK,KeyType=HASH \
        AttributeName=SK,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --tags Key=Project,Value=PriceTracker
```

### Enable TTL (Time to Live)

```bash
aws dynamodb update-time-to-live \
    --table-name PriceTracker \
    --time-to-live-specification Enabled=true,AttributeName=ttl
```

### Step 8: Create DynamoDB Service

```java
public class DynamoDBService {
    
    // Initialize DynamoDbClient or DynamoDbEnhancedClient
    // private final DynamoDbClient dynamoDbClient;
    // private final String tableName;

    // Method: updateCurrentPrice(PriceUpdateEvent event)
    //   - Create PK: "PRODUCT#" + productId
    //   - Create SK: "VENDOR#" + vendorId
    //   - Use UpdateItem with SET expressions
    //   - Calculate TTL for expiring deals
}
```

### DynamoDB Update Pattern

```java
// Update item pattern
// Map<String, AttributeValue> key = Map.of(
//     "PK", AttributeValue.fromS("PRODUCT#" + event.getProductId()),
//     "SK", AttributeValue.fromS("VENDOR#" + event.getVendorId())
// );

// Update expression
// String updateExpression = "SET currentPrice = :price, " +
//                           "currency = :currency, " +
//                           "lastUpdated = :timestamp, " +
//                           "priceDirection = :direction, " +
//                           "ttl = :ttl";

// Calculate TTL (e.g., 30 days from now)
// long ttlValue = Instant.now().plus(30, ChronoUnit.DAYS).getEpochSecond();
```

---

## MSK/Kafka Trigger Configuration

### Step 9: Configure Lambda Trigger

**Option A: Amazon MSK (Managed Kafka)**

```bash
aws lambda create-event-source-mapping \
    --function-name PriceUpdateProcessor \
    --event-source-arn arn:aws:kafka:region:account:cluster/msk-cluster/uuid \
    --topics product-price-updates \
    --starting-position LATEST \
    --batch-size 100 \
    --maximum-batching-window-in-seconds 5
```

**Option B: Self-Managed Kafka**

```bash
aws lambda create-event-source-mapping \
    --function-name PriceUpdateProcessor \
    --self-managed-event-source '{"Endpoints":{"KAFKA_BOOTSTRAP_SERVERS":["broker1:9092","broker2:9092"]}}' \
    --topics product-price-updates \
    --source-access-configurations '[{"Type":"VPC_SUBNET","URI":"subnet-xxx"},{"Type":"VPC_SECURITY_GROUP","URI":"sg-xxx"}]' \
    --starting-position LATEST \
    --batch-size 100
```

### Trigger Configuration Options

| Setting | Recommended Value | Description |
|---------|-------------------|-------------|
| `batch-size` | 100-500 | Max records per invocation |
| `batching-window` | 5 seconds | Wait time to fill batch |
| `starting-position` | LATEST or TRIM_HORIZON | Where to start reading |
| `parallelization-factor` | 1-10 | Concurrent batches per shard |

---

## Deployment

### Step 10: Build the JAR

```bash
mvn clean package
```

This creates: `target/lambda-consumer-1.0-SNAPSHOT.jar`

### Step 11: Create IAM Role

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject"
      ],
      "Resource": "arn:aws:s3:::price-tracker-data-lake/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
        "dynamodb:GetItem"
      ],
      "Resource": "arn:aws:dynamodb:*:*:table/PriceTracker"
    },
    {
      "Effect": "Allow",
      "Action": [
        "kafka:DescribeCluster",
        "kafka:GetBootstrapBrokers",
        "kafka-cluster:Connect",
        "kafka-cluster:DescribeTopic",
        "kafka-cluster:ReadData"
      ],
      "Resource": "*"
    }
  ]
}
```

### Step 12: Create Lambda Function

```bash
aws lambda create-function \
    --function-name PriceUpdateProcessor \
    --runtime java17 \
    --handler com.example.pricetracker.handler.PriceUpdateHandler::handleRequest \
    --role arn:aws:iam::ACCOUNT_ID:role/lambda-price-tracker-role \
    --zip-file fileb://target/lambda-consumer-1.0-SNAPSHOT.jar \
    --timeout 60 \
    --memory-size 512 \
    --environment Variables={S3_BUCKET=price-tracker-data-lake,DYNAMODB_TABLE=PriceTracker}
```

### Step 13: Update Function Code (After Changes)

```bash
aws lambda update-function-code \
    --function-name PriceUpdateProcessor \
    --zip-file fileb://target/lambda-consumer-1.0-SNAPSHOT.jar
```

---

## Testing

### Step 14: Local Testing

Create a test event file `test-kafka-event.json`:

```json
{
  "eventSource": "aws:kafka",
  "eventSourceArn": "arn:aws:kafka:us-east-1:123456789:cluster/test",
  "records": {
    "product-price-updates-0": [
      {
        "topic": "product-price-updates",
        "partition": 0,
        "offset": 1,
        "timestamp": 1703001600000,
        "timestampType": "CREATE_TIME",
        "key": "UFJPRC0wMDE=",
        "value": "eyJwcm9kdWN0SWQiOiJQUk9ELTAwMSIsInZlbmRvcklkIjoiVkVORE9SLUFNQVpPTiIsIm9sZFByaWNlIjo3OS45OSwibmV3UHJpY2UiOjU5Ljk5LCJjdXJyZW5jeSI6IlVTRCIsImV2ZW50VHlwZSI6IlBSSUNFX0RST1AiLCJ0aW1lc3RhbXAiOiIyMDI1LTEyLTE5VDEwOjMwOjAwIn0="
      }
    ]
  }
}
```

> **Note:** The `value` is Base64 encoded JSON

### Invoke Lambda Locally (AWS SAM)

```bash
sam local invoke PriceUpdateProcessor -e test-kafka-event.json
```

### Invoke Lambda in AWS

```bash
aws lambda invoke \
    --function-name PriceUpdateProcessor \
    --payload fileb://test-kafka-event.json \
    response.json
```

### Step 15: Verify S3 Data

```bash
aws s3 ls s3://price-tracker-data-lake/raw/ --recursive
```

### Step 16: Verify DynamoDB Data

```bash
aws dynamodb get-item \
    --table-name PriceTracker \
    --key '{"PK": {"S": "PRODUCT#PROD-001"}, "SK": {"S": "VENDOR#VENDOR-AMAZON"}}'
```

---

## Monitoring & Troubleshooting

### CloudWatch Logs

```bash
# View recent logs
aws logs tail /aws/lambda/PriceUpdateProcessor --follow
```

### Key Metrics to Monitor

| Metric | Alert Threshold | Description |
|--------|-----------------|-------------|
| `Errors` | > 0 | Failed invocations |
| `Duration` | > 30s | Slow processing |
| `Throttles` | > 0 | Hitting concurrency limits |
| `IteratorAge` | > 60000ms | Consumer falling behind |

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| **Timeout** | Processing too slow | Increase timeout, optimize code |
| **Memory Error** | Large batch size | Reduce batch size, increase memory |
| **Permission Denied** | IAM role missing permissions | Update IAM policy |
| **Connection Timeout** | VPC/Network issues | Check security groups, VPC config |
| **Deserialization Error** | Schema mismatch | Verify event format matches model |

### Enable X-Ray Tracing

```bash
aws lambda update-function-configuration \
    --function-name PriceUpdateProcessor \
    --tracing-config Mode=Active
```

---

## Project Structure Summary

```
lambda-consumer/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/pricetracker/
│       │       ├── handler/
│       │       │   └── PriceUpdateHandler.java    # Lambda entry point
│       │       ├── model/
│       │       │   ├── PriceUpdateEvent.java      # Event POJO
│       │       │   └── PriceRecord.java           # DynamoDB entity
│       │       ├── service/
│       │       │   ├── ValidationService.java     # Data validation
│       │       │   ├── S3Service.java             # S3 operations
│       │       │   └── DynamoDBService.java       # DynamoDB operations
│       │       └── util/
│       │           └── JsonUtil.java              # JSON utilities
│       └── resources/
│           └── log4j2.xml                         # Logging config
├── test-kafka-event.json                          # Test payload
└── template.yaml                                  # SAM template (optional)
```

---

## Next Steps

Once your Lambda consumer is working:

1. ✅ **Verify S3 writes** - Check raw events are stored
2. ✅ **Verify DynamoDB updates** - Check current state is correct
3. 🔜 **Build API Gateway** - Expose DynamoDB data via REST API
4. 🔜 **Build React Dashboard** - Display trending price drops

---

## Useful Resources

- [AWS Lambda Developer Guide](https://docs.aws.amazon.com/lambda/latest/dg/)
- [Using Lambda with Amazon MSK](https://docs.aws.amazon.com/lambda/latest/dg/with-msk.html)
- [AWS SDK for Java v2](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/)
- [DynamoDB Best Practices](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/best-practices.html)

---

*Happy Streaming! ⚡*

