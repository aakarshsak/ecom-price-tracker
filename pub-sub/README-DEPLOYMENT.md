# 🚀 E-Commerce Price Tracker - AWS Deployment Guide

A complete step-by-step guide to deploy the entire Price Tracker application on AWS.

---

## 📋 Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Prerequisites](#prerequisites)
3. [Deployment Checklist](#deployment-checklist)
4. [Step 1: Create S3 Bucket](#step-1-create-s3-bucket-data-lake)
5. [Step 2: Create DynamoDB Table](#step-2-create-dynamodb-table)
6. [Step 3: Create IAM Role for Lambda](#step-3-create-iam-role-for-lambda)
7. [Step 4: Build & Deploy Lambda Consumer](#step-4-build--deploy-lambda-consumer)
8. [Step 5: Set Up Kafka](#step-5-set-up-kafka)
9. [Step 6: Connect Lambda to Kafka](#step-6-connect-lambda-to-kafka)
10. [Step 7: Deploy Producer](#step-7-deploy-producer)
11. [Step 8: End-to-End Testing](#step-8-end-to-end-testing)
12. [Monitoring & Troubleshooting](#monitoring--troubleshooting)
13. [Cost Estimation](#cost-estimation)
14. [Cleanup](#cleanup)

---

## Architecture Overview

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│    Producer     │     │     Kafka       │     │  Lambda         │
│  (Spring Boot)  │────►│  (MSK / EC2)    │────►│  Consumer       │
│   EC2 / Local   │     │                 │     │                 │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
                                         ┌───────────────┼───────────────┐
                                         │               │               │
                                         ▼               ▼               │
                                  ┌──────────┐   ┌──────────────┐       │
                                  │    S3    │   │  DynamoDB    │       │
                                  │Data Lake │   │Current State │       │
                                  └──────────┘   └──────────────┘       │
                                                         │               │
                                                         ▼               │
                                                 ┌──────────────┐       │
                                                 │ API Gateway  │◄──────┘
                                                 │  + Lambda    │ (Future)
                                                 └──────┬───────┘
                                                        │
                                                        ▼
                                                 ┌──────────────┐
                                                 │    React     │
                                                 │  Dashboard   │ (Future)
                                                 └──────────────┘
```

### Component Summary

| Component | Technology | AWS Service |
|-----------|------------|-------------|
| Producer | Spring Boot + Kafka | EC2 / ECS / Local |
| Message Broker | Apache Kafka | Amazon MSK / EC2 |
| Consumer | Java Lambda | AWS Lambda |
| Raw Storage | JSON files | Amazon S3 |
| Current State | Key-Value store | Amazon DynamoDB |
| API | REST | API Gateway + Lambda |
| Frontend | React | S3 + CloudFront |

---

## Prerequisites

### Required Tools

| Tool | Purpose | Installation |
|------|---------|--------------|
| AWS CLI | Deploy AWS resources | `winget install Amazon.AWSCLI` |
| Java 21 | Build applications | Download from Oracle/Adoptium |
| Maven | Build Java projects | `winget install Apache.Maven` |
| Docker | Run Kafka locally | Docker Desktop |

### Verify Installations

```powershell
aws --version
java -version
mvn -version
docker --version
```

### Configure AWS CLI

```powershell
aws configure
```

Enter:
- AWS Access Key ID
- AWS Secret Access Key
- Default region: `us-east-1` (or your preferred region)
- Default output: `json`

### Get Your AWS Account ID

```powershell
aws sts get-caller-identity --query Account --output text
```

Save this - you'll need it throughout the deployment.

---

## Deployment Checklist

| # | Task | Status |
|---|------|--------|
| 1 | Create S3 Bucket | ⬜ |
| 2 | Create DynamoDB Table | ⬜ |
| 3 | Create IAM Role | ⬜ |
| 4 | Deploy Lambda Function | ⬜ |
| 5 | Set up Kafka (MSK/EC2) | ⬜ |
| 6 | Create Kafka Topic | ⬜ |
| 7 | Connect Lambda to Kafka | ⬜ |
| 8 | Deploy/Run Producer | ⬜ |
| 9 | End-to-End Test | ⬜ |

---

## Step 1: Create S3 Bucket (Data Lake)

### Create the Bucket

```powershell
aws s3 mb s3://ecom-price-tracker-data-lake --region us-east-1
```

> **Note:** Bucket names must be globally unique. If this name is taken, add a unique suffix like `-yourname-123`

### Verify Creation

```powershell
aws s3 ls | findstr price-tracker
```

### (Optional) Enable Versioning

```powershell
aws s3api put-bucket-versioning `
    --bucket ecom-price-tracker-data-lake `
    --versioning-configuration Status=Enabled
```

### (Optional) Set Lifecycle Policy

Create `lifecycle.json`:
```json
{
  "Rules": [
    {
      "ID": "ArchiveOldData",
      "Status": "Enabled",
      "Filter": { "Prefix": "raw/" },
      "Transitions": [
        { "Days": 90, "StorageClass": "GLACIER" }
      ],
      "Expiration": { "Days": 365 }
    }
  ]
}
```

Apply:
```powershell
aws s3api put-bucket-lifecycle-configuration `
    --bucket ecom-price-tracker-data-lake `
    --lifecycle-configuration file://lifecycle.json
```

---

## Step 2: Create DynamoDB Table

### Create Table

```powershell
aws dynamodb create-table `
    --table-name PriceTracker `
    --attribute-definitions `
        AttributeName=PK,AttributeType=S `
        AttributeName=SK,AttributeType=S `
    --key-schema `
        AttributeName=PK,KeyType=HASH `
        AttributeName=SK,KeyType=RANGE `
    --billing-mode PAY_PER_REQUEST `
    --tags Key=Project,Value=EcomPriceTracker
```

### Wait for Table to be Active

```powershell
aws dynamodb wait table-exists --table-name PriceTracker
```

### Enable TTL (Time to Live)

```powershell
aws dynamodb update-time-to-live `
    --table-name PriceTracker `
    --time-to-live-specification Enabled=true,AttributeName=ttl
```

### Verify Table

```powershell
aws dynamodb describe-table --table-name PriceTracker --query "Table.TableStatus"
```

Expected output: `"ACTIVE"`

---

## Step 3: Create IAM Role for Lambda

### 3.1 Create Trust Policy

Create file `trust-policy.json`:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
```

### 3.2 Create the Role

```powershell
aws iam create-role `
    --role-name lambda-price-tracker-role `
    --assume-role-policy-document file://trust-policy.json
```

### 3.3 Attach Basic Execution Policy

```powershell
aws iam attach-role-policy `
    --role-name lambda-price-tracker-role `
    --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
```

### 3.4 Create Custom Policy for S3, DynamoDB, Kafka

Create file `custom-policy.json`:
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "S3Access",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::ecom-price-tracker-data-lake",
        "arn:aws:s3:::ecom-price-tracker-data-lake/*"
      ]
    },
    {
      "Sid": "DynamoDBAccess",
      "Effect": "Allow",
      "Action": [
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
        "dynamodb:GetItem",
        "dynamodb:Query",
        "dynamodb:Scan"
      ],
      "Resource": "arn:aws:dynamodb:*:*:table/PriceTracker"
    },
    {
      "Sid": "KafkaAccess",
      "Effect": "Allow",
      "Action": [
        "kafka:DescribeCluster",
        "kafka:DescribeClusterV2",
        "kafka:GetBootstrapBrokers",
        "kafka-cluster:Connect",
        "kafka-cluster:DescribeGroup",
        "kafka-cluster:AlterGroup",
        "kafka-cluster:DescribeTopic",
        "kafka-cluster:ReadData",
        "kafka-cluster:DescribeClusterDynamicConfiguration"
      ],
      "Resource": "*"
    },
    {
      "Sid": "VPCAccess",
      "Effect": "Allow",
      "Action": [
        "ec2:CreateNetworkInterface",
        "ec2:DescribeNetworkInterfaces",
        "ec2:DeleteNetworkInterface",
        "ec2:DescribeVpcs",
        "ec2:DescribeSubnets",
        "ec2:DescribeSecurityGroups"
      ],
      "Resource": "*"
    }
  ]
}
```

### 3.5 Attach Custom Policy

```powershell
aws iam put-role-policy `
    --role-name lambda-price-tracker-role `
    --policy-name price-tracker-permissions `
    --policy-document file://custom-policy.json
```

### 3.6 Get Role ARN (Save This!)

```powershell
aws iam get-role --role-name lambda-price-tracker-role --query "Role.Arn" --output text
```

Output example: `arn:aws:iam::123456789012:role/lambda-price-tracker-role`

---

## Step 4: Build & Deploy Lambda Consumer

### 4.1 Fix Code Issues First

Before building, ensure these fixes are applied to `PriceUpdateHandler.java`:

1. **Change Base64 import:**
   ```java
   // Remove: import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
   // Add: import java.util.Base64;
   ```

2. **Update Base64 decode call:**
   ```java
   String jsonValue = new String(Base64.getDecoder().decode(record.getValue()));
   ```

3. **Register JavaTimeModule:**
   ```java
   this.objectMapper = new ObjectMapper();
   this.objectMapper.registerModule(new JavaTimeModule());
   ```

4. **Fix S3 key typo:**
   ```java
   // Change month-%02d to month=%02d
   ```

### 4.2 Build the JAR

```powershell
cd consumer
mvn clean package -DskipTests
```

Verify JAR created:
```powershell
dir target\*.jar
```

### 4.3 Deploy Lambda Function

Replace `YOUR_ACCOUNT_ID` with your actual account ID:

```powershell
aws lambda create-function `
    --function-name PriceUpdateProcessor `
    --runtime java21 `
    --handler com.sinha.ecom_tracker.consumer.PriceUpdateHandler::handleRequest `
    --role arn:aws:iam::YOUR_ACCOUNT_ID:role/lambda-price-tracker-role `
    --zip-file fileb://target/consumer-0.0.1-SNAPSHOT.jar `
    --timeout 60 `
    --memory-size 512 `
    --environment "Variables={S3_BUCKET=ecom-price-tracker-data-lake,DYNAMODB_TABLE=PriceTracker}"
```

### 4.4 Verify Deployment

```powershell
aws lambda get-function --function-name PriceUpdateProcessor --query "Configuration.State"
```

Expected output: `"Active"`

### 4.5 Update Lambda Code (After Changes)

```powershell
mvn clean package -DskipTests
aws lambda update-function-code `
    --function-name PriceUpdateProcessor `
    --zip-file fileb://target/consumer-0.0.1-SNAPSHOT.jar
```

---

## Step 5: Set Up Kafka

Choose ONE of the following options:

### Option A: Amazon MSK (Managed - Production Ready)

> **Cost Warning:** MSK costs ~$0.10-0.20/hour even when idle. Use for production only.

```powershell
# This is a simplified example - MSK requires VPC setup
aws kafka create-cluster `
    --cluster-name price-tracker-kafka `
    --kafka-version 3.5.1 `
    --number-of-broker-nodes 2 `
    --broker-node-group-info file://msk-broker-config.json
```

### Option B: Kafka on EC2 (Cost-Effective for Dev)

#### Step 5B.1: Launch EC2 Instance

```powershell
# Create security group
aws ec2 create-security-group `
    --group-name kafka-sg `
    --description "Kafka Security Group"

# Allow SSH
aws ec2 authorize-security-group-ingress `
    --group-name kafka-sg `
    --protocol tcp --port 22 --cidr 0.0.0.0/0

# Allow Kafka
aws ec2 authorize-security-group-ingress `
    --group-name kafka-sg `
    --protocol tcp --port 9092 --cidr 0.0.0.0/0

# Launch instance (Amazon Linux 2023)
aws ec2 run-instances `
    --image-id ami-0c02fb55956c7d316 `
    --instance-type t3.medium `
    --key-name your-key-pair `
    --security-groups kafka-sg `
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=kafka-server}]"
```

#### Step 5B.2: Install Docker on EC2

SSH into your instance and run:

```bash
sudo yum update -y
sudo yum install -y docker
sudo service docker start
sudo usermod -a -G docker ec2-user

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

#### Step 5B.3: Create docker-compose.yml on EC2

```yaml
version: '3'
services:
  kafka:
    image: apache/kafka:latest
    hostname: broker
    container_name: broker
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://YOUR_EC2_PUBLIC_IP:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@broker:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_LOG_DIRS: /tmp/kraft-combined-logs
      CLUSTER_ID: MkU3OEVBNTcwNTJENDM2Qk
```

> **Important:** Replace `YOUR_EC2_PUBLIC_IP` with your actual EC2 public IP

#### Step 5B.4: Start Kafka

```bash
docker-compose up -d
```

### Option C: Local Kafka (Testing Only)

Run on your local machine using the `docker-compose.yml` in your project:

```powershell
cd C:\Users\Aakarsh.Sinha\personal\ecom-price-tracker
docker-compose up -d
```

---

## Step 6: Connect Lambda to Kafka

### 6.1 Create Kafka Topic First

On your Kafka server (EC2 or local):

```bash
docker exec -it broker kafka-topics --create `
    --topic product-price-updates `
    --bootstrap-server localhost:9092 `
    --partitions 3 `
    --replication-factor 1
```

### 6.2 For Amazon MSK

```powershell
aws lambda create-event-source-mapping `
    --function-name PriceUpdateProcessor `
    --event-source-arn arn:aws:kafka:us-east-1:YOUR_ACCOUNT_ID:cluster/price-tracker-kafka/CLUSTER_UUID `
    --topics product-price-updates `
    --starting-position LATEST `
    --batch-size 100 `
    --maximum-batching-window-in-seconds 5
```

### 6.3 For Self-Managed Kafka (EC2)

First, you need Lambda in a VPC that can reach your EC2 Kafka.

```powershell
# Get your VPC ID, Subnet IDs, and Security Group ID first
aws ec2 describe-vpcs --query "Vpcs[0].VpcId"
aws ec2 describe-subnets --query "Subnets[*].SubnetId"
aws ec2 describe-security-groups --query "SecurityGroups[*].GroupId"

# Create event source mapping
aws lambda create-event-source-mapping `
    --function-name PriceUpdateProcessor `
    --self-managed-event-source "{\"Endpoints\":{\"KAFKA_BOOTSTRAP_SERVERS\":[\"YOUR_EC2_IP:9092\"]}}" `
    --topics product-price-updates `
    --starting-position LATEST `
    --batch-size 100 `
    --source-access-configurations "[{\"Type\":\"VPC_SUBNET\",\"URI\":\"subnet-xxxxx\"},{\"Type\":\"VPC_SECURITY_GROUP\",\"URI\":\"sg-xxxxx\"}]"
```

### 6.4 Verify Event Source Mapping

```powershell
aws lambda list-event-source-mappings --function-name PriceUpdateProcessor
```

---

## Step 7: Deploy Producer

### 7.1 Update Producer Configuration

Edit `producer/src/main/resources/application.yaml`:

```yaml
spring:
  kafka:
    bootstrap-servers: YOUR_KAFKA_ADDRESS:9092  # EC2 IP or MSK endpoint
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3

app:
  kafka:
    topic:
      price-updates: product-price-updates
```

### 7.2 Build Producer

```powershell
cd producer
mvn clean package -DskipTests
```

### 7.3 Run Producer

#### Option A: Run Locally (For Testing)

```powershell
java -jar target/producer-0.0.1-SNAPSHOT.jar
```

#### Option B: Run on EC2

1. Copy JAR to EC2:
   ```powershell
   scp -i your-key.pem target/producer-0.0.1-SNAPSHOT.jar ec2-user@YOUR_EC2_IP:~/
   ```

2. SSH and run:
   ```bash
   java -jar producer-0.0.1-SNAPSHOT.jar
   ```

#### Option C: Run as Docker Container

Create `Dockerfile`:
```dockerfile
FROM eclipse-temurin:21-jre
COPY target/producer-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

Build and run:
```powershell
docker build -t price-tracker-producer .
docker run -p 8080:8080 price-tracker-producer
```

---

## Step 8: End-to-End Testing

### 8.1 Test Producer API

```powershell
# Send a test price update
curl -X POST http://localhost:8080/api/prices/update `
    -H "Content-Type: application/json" `
    -d '{
        "productId": "PROD-001",
        "vendorId": "VENDOR-AMAZON",
        "oldPrice": 79.99,
        "newPrice": 59.99,
        "currency": "USD",
        "eventType": "PRICE_DROP",
        "timeStamp": "2025-12-21T10:30:00"
    }'
```

### 8.2 Check Kafka Messages

```bash
docker exec -it broker kafka-console-consumer `
    --bootstrap-server localhost:9092 `
    --topic product-price-updates `
    --from-beginning
```

### 8.3 Check Lambda Logs

```powershell
aws logs tail /aws/lambda/PriceUpdateProcessor --follow
```

### 8.4 Verify S3 Data

```powershell
aws s3 ls s3://ecom-price-tracker-data-lake/raw/ --recursive
```

### 8.5 Verify DynamoDB Data

```powershell
# Scan all items
aws dynamodb scan --table-name PriceTracker

# Get specific item
aws dynamodb get-item `
    --table-name PriceTracker `
    --key '{"PK": {"S": "PRODUCT#PROD-001"}, "SK": {"S": "VENDOR#VENDOR-AMAZON"}}'
```

---

## Monitoring & Troubleshooting

### View Lambda Logs

```powershell
# Real-time logs
aws logs tail /aws/lambda/PriceUpdateProcessor --follow

# Last 100 log events
aws logs get-log-events `
    --log-group-name /aws/lambda/PriceUpdateProcessor `
    --log-stream-name "$(aws logs describe-log-streams --log-group-name /aws/lambda/PriceUpdateProcessor --order-by LastEventTime --descending --limit 1 --query 'logStreams[0].logStreamName' --output text)"
```

### Check Lambda Metrics

```powershell
aws cloudwatch get-metric-statistics `
    --namespace AWS/Lambda `
    --metric-name Invocations `
    --dimensions Name=FunctionName,Value=PriceUpdateProcessor `
    --start-time (Get-Date).AddHours(-1).ToString("yyyy-MM-ddTHH:mm:ssZ") `
    --end-time (Get-Date).ToString("yyyy-MM-ddTHH:mm:ssZ") `
    --period 300 `
    --statistics Sum
```

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Lambda timeout | Processing too slow | Increase timeout to 120s or increase memory |
| S3 Access Denied | IAM permissions | Check IAM role policy |
| DynamoDB Access Denied | IAM permissions | Check IAM role policy |
| Kafka connection failed | Network/Security group | Check VPC, security groups, Kafka is running |
| Deserialization error | JSON format mismatch | Ensure producer and consumer use same schema |
| Cold start slow | Java Lambda | Increase memory (also increases CPU) |

### Enable X-Ray Tracing (Optional)

```powershell
aws lambda update-function-configuration `
    --function-name PriceUpdateProcessor `
    --tracing-config Mode=Active
```

---

## Cost Estimation

### Development Environment (Monthly)

| Service | Configuration | Estimated Cost |
|---------|---------------|----------------|
| Lambda | 1M requests, 512MB | ~$0 (Free tier) |
| S3 | 1GB storage | ~$0.02 |
| DynamoDB | On-demand, 1GB | ~$0 (Free tier) |
| EC2 (Kafka) | t3.medium, 8 hrs/day | ~$25 |
| **Total** | | **~$25/month** |

### Production Environment (Monthly)

| Service | Configuration | Estimated Cost |
|---------|---------------|----------------|
| Lambda | 10M requests, 1024MB | ~$20 |
| S3 | 100GB storage | ~$2.30 |
| DynamoDB | On-demand, 25GB | ~$6 |
| MSK | 2 brokers, kafka.t3.small | ~$150 |
| **Total** | | **~$180/month** |

---

## Cleanup

### Delete All Resources

```powershell
# Delete Lambda event source mapping
aws lambda list-event-source-mappings --function-name PriceUpdateProcessor --query "EventSourceMappings[*].UUID" --output text | ForEach-Object { aws lambda delete-event-source-mapping --uuid $_ }

# Delete Lambda function
aws lambda delete-function --function-name PriceUpdateProcessor

# Delete DynamoDB table
aws dynamodb delete-table --table-name PriceTracker

# Empty and delete S3 bucket
aws s3 rm s3://ecom-price-tracker-data-lake --recursive
aws s3 rb s3://ecom-price-tracker-data-lake

# Delete IAM role (must detach policies first)
aws iam delete-role-policy --role-name lambda-price-tracker-role --policy-name price-tracker-permissions
aws iam detach-role-policy --role-name lambda-price-tracker-role --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
aws iam delete-role --role-name lambda-price-tracker-role

# Terminate EC2 instance (if created)
aws ec2 terminate-instances --instance-ids YOUR_INSTANCE_ID

# Delete security group
aws ec2 delete-security-group --group-name kafka-sg
```

---

## Next Steps

After deployment is complete:

1. ✅ **Verify end-to-end flow** - Producer → Kafka → Lambda → S3 + DynamoDB
2. 🔜 **Build API Gateway** - Expose DynamoDB data via REST API
3. 🔜 **Build React Dashboard** - Display trending price drops
4. 🔜 **Set up CI/CD** - Automate deployments with GitHub Actions
5. 🔜 **Add monitoring** - CloudWatch dashboards and alarms

---

## Quick Reference Commands

| Action | Command |
|--------|---------|
| Deploy Lambda | `aws lambda update-function-code --function-name PriceUpdateProcessor --zip-file fileb://target/consumer-0.0.1-SNAPSHOT.jar` |
| View logs | `aws logs tail /aws/lambda/PriceUpdateProcessor --follow` |
| Check S3 | `aws s3 ls s3://ecom-price-tracker-data-lake/raw/ --recursive` |
| Scan DynamoDB | `aws dynamodb scan --table-name PriceTracker` |
| List Kafka topics | `docker exec -it broker kafka-topics --list --bootstrap-server localhost:9092` |

---

*Happy Deploying! 🚀*

