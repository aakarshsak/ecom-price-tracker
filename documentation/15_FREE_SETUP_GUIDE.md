# 💰 Free Setup Guide - Build Without Spending Money

## Overview

This guide shows you how to build the **entire trading platform for $0**. Everything runs locally on your laptop using Docker, and all market data comes from free APIs.

**Total Monthly Cost: $0.00**

---

## 🎯 What You'll Build (100% Free)

✅ Complete microservices architecture  
✅ Real-time market data streaming  
✅ WebSocket price updates  
✅ Event-driven with Kafka  
✅ Redis caching layer  
✅ TimescaleDB for time-series data  
✅ JWT authentication  
✅ API Gateway with rate limiting  

**All running on your laptop, no cloud costs!**

---

## 💻 Hardware Requirements

| Component | Minimum | Recommended | Your Laptop Probably Has |
|-----------|---------|-------------|--------------------------|
| **RAM** | 4GB | 8GB+ | ✅ Most modern laptops |
| **Disk Space** | 10GB | 20GB+ | ✅ Easy |
| **CPU** | 2 cores | 4+ cores | ✅ Common |
| **OS** | Windows/Mac/Linux | Any | ✅ You're good |

**Bottom line**: If your laptop can run VS Code, it can run this project!

---

## 🆓 Free Software Stack

### Development Tools (All Free)

| Tool | Purpose | Download | Cost |
|------|---------|----------|------|
| **Java 17 JDK** | Runtime | [Adoptium](https://adoptium.net/) | FREE |
| **Maven** | Build tool | [maven.apache.org](https://maven.apache.org/) | FREE |
| **Docker Desktop** | Containers | [docker.com](https://www.docker.com/products/docker-desktop/) | FREE |
| **Git** | Version control | [git-scm.com](https://git-scm.com/) | FREE |
| **VS Code** | Code editor | [code.visualstudio.com](https://code.visualstudio.com/) | FREE |
| **IntelliJ Community** | Java IDE | [jetbrains.com](https://www.jetbrains.com/idea/download/) | FREE |
| **Postman** | API testing | [postman.com](https://www.postman.com/) | FREE |
| **DBeaver** | Database client | [dbeaver.io](https://dbeaver.io/) | FREE |

### Infrastructure (All Free via Docker)

| Service | Docker Image | Purpose | Cost |
|---------|--------------|---------|------|
| **PostgreSQL** | `postgres:15-alpine` | Main database | FREE |
| **TimescaleDB** | `timescale/timescaledb:latest-pg14` | Time-series data | FREE |
| **Redis** | `redis:7-alpine` | Caching | FREE |
| **Kafka** | `confluentinc/cp-kafka:7.5.0` | Event streaming | FREE |
| **Zookeeper** | `confluentinc/cp-zookeeper:7.5.0` | Kafka coordination | FREE |
| **Kafka UI** | `provectuslabs/kafka-ui:latest` | Kafka monitoring | FREE |

---

## 📊 Free Market Data Sources

### Stock Market Data (FREE)

#### 1. Yahoo Finance API ⭐ **RECOMMENDED**
```
✅ Completely FREE
✅ No API key required
✅ Unlimited requests (reasonable use)
✅ Real-time quotes (15-min delay for free)
✅ Historical data
✅ Global coverage (US, India, Europe, Asia)

Java Library:
<dependency>
    <groupId>com.yahoofinance-api</groupId>
    <artifactId>YahooFinanceAPI</artifactId>
    <version>3.17.0</version>
</dependency>

Symbols: AAPL, GOOGL, TSLA, RELIANCE.NS, INFY.NS, etc.
```

**Example Usage**:
```java
import yahoofinance.YahooFinance;
import yahoofinance.Stock;

// Get real-time price (FREE)
Stock stock = YahooFinance.get("AAPL");
BigDecimal price = stock.getQuote().getPrice();
BigDecimal change = stock.getQuote().getChangeInPercent();

// Get historical data (FREE)
Calendar from = Calendar.getInstance();
from.add(Calendar.YEAR, -1);
Calendar to = Calendar.getInstance();
Map<String, HistoricalQuote> history = 
    stock.getHistory(from, to, Interval.DAILY);
```

#### 2. Alpha Vantage
```
✅ FREE tier: 25 requests/day
✅ API key required (free signup)
✅ Good for historical data
✅ Technical indicators

Sign up: https://www.alphavantage.co/support/#api-key

Limits: 25 calls/day (free), 5 calls/minute
```

#### 3. Finnhub
```
✅ FREE tier: 60 calls/minute
✅ API key required (free signup)
✅ Real-time quotes
✅ Company news, earnings

Sign up: https://finnhub.io/register

Limits: 60 calls/min (free)
```

#### 4. IEX Cloud
```
✅ FREE tier: 50,000 credits/month
✅ API key required (free signup)
✅ US stocks
✅ Good for development

Sign up: https://iexcloud.io/

Limits: 50K credits/month
```

### Cryptocurrency Data (FREE)

#### 1. Binance Public API ⭐ **RECOMMENDED**
```
✅ Completely FREE
✅ No API key required (for public data)
✅ Unlimited requests (within rate limits)
✅ Real-time crypto prices
✅ Order book data
✅ Historical klines (candlesticks)

Base URL: https://api.binance.com

Rate Limits:
- 1200 requests per minute
- 20 orders per second

Symbols: BTCUSDT, ETHUSDT, BNBUSDT, etc.
```

**Example Usage**:
```java
// Get current price (FREE, no API key)
String url = "https://api.binance.com/api/v3/ticker/price?symbol=BTCUSDT";
WebClient client = WebClient.create();
String response = client.get()
    .uri(url)
    .retrieve()
    .bodyToMono(String.class)
    .block();
// Returns: {"symbol":"BTCUSDT","price":"43250.50"}

// Get 24h ticker stats (FREE)
String tickerUrl = "https://api.binance.com/api/v3/ticker/24hr?symbol=BTCUSDT";
// Returns: high, low, volume, priceChange, etc.

// Get historical candles (FREE)
String klinesUrl = "https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=1h&limit=24";
```

#### 2. CoinGecko API
```
✅ FREE tier: 10-50 calls/minute
✅ No API key required
✅ Crypto prices, market data
✅ Good as fallback

Base URL: https://api.coingecko.com/api/v3

Rate Limits:
- 10-50 calls/minute (free)
```

#### 3. CryptoCompare
```
✅ FREE tier: 100,000 calls/month
✅ API key required (free signup)
✅ Historical data
✅ Multiple exchanges

Sign up: https://www.cryptocompare.com/cryptopian/api-keys
```

### Indian Stock Market (FREE)

#### 1. NSE India (Unofficial)
```
⚠️ No official API, but public endpoints work
✅ FREE
✅ No API key
✅ Real-time NSE/BSE data

Example: https://www.nseindia.com/api/quote-equity?symbol=RELIANCE
```

#### 2. Yahoo Finance India
```
✅ FREE via Yahoo Finance API
✅ NSE/BSE symbols with .NS or .BO suffix

Examples:
- RELIANCE.NS (Reliance Industries)
- INFY.NS (Infosys)
- TCS.NS (Tata Consultancy)
- HDFCBANK.NS (HDFC Bank)
```

---

## 🚀 Step-by-Step Setup

### Step 1: Install Prerequisites (FREE)

#### Install Java 17
```bash
# Download from: https://adoptium.net/
# Or use package manager:

# macOS
brew install openjdk@17

# Windows (Chocolatey)
choco install openjdk17

# Linux (Ubuntu)
sudo apt install openjdk-17-jdk

# Verify
java -version
# Should show: openjdk version "17.x.x"
```

#### Install Maven
```bash
# Download from: https://maven.apache.org/download.cgi

# macOS
brew install maven

# Windows (Chocolatey)
choco install maven

# Linux (Ubuntu)
sudo apt install maven

# Verify
mvn -version
```

#### Install Docker Desktop
```bash
# Download from: https://www.docker.com/products/docker-desktop/

# macOS: Download .dmg and install
# Windows: Download .exe and install
# Linux: Follow instructions for your distro

# Verify
docker --version
docker-compose --version
```

#### Install Git
```bash
# Download from: https://git-scm.com/

# macOS (usually pre-installed)
git --version

# Windows: Download installer
# Linux
sudo apt install git

# Verify
git --version
```

---

### Step 2: Create Docker Compose (FREE Infrastructure)

Create `docker-compose.yml` in your project root:

```yaml
version: '3.8'

services:
  # PostgreSQL - Main database (FREE)
  postgres:
    image: postgres:15-alpine
    container_name: trading-postgres
    ports:
      - "5432:5432"
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: trading_db
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - trading-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  # TimescaleDB - Time-series database (FREE)
  timescaledb:
    image: timescale/timescaledb:latest-pg14
    container_name: trading-timescale
    ports:
      - "5433:5432"
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: market_data
    volumes:
      - timescale_data:/var/lib/postgresql/data
    networks:
      - trading-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis - Caching (FREE)
  redis:
    image: redis:7-alpine
    container_name: trading-redis
    ports:
      - "6379:6379"
    command: redis-server --maxmemory 512mb --maxmemory-policy allkeys-lru
    volumes:
      - redis_data:/data
    networks:
      - trading-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Zookeeper - Required for Kafka (FREE)
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: trading-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    networks:
      - trading-network

  # Kafka - Event streaming (FREE)
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: trading-kafka
    ports:
      - "9092:9092"
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
    networks:
      - trading-network
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
      interval: 30s
      timeout: 10s
      retries: 5

  # Kafka UI - Monitor Kafka (FREE)
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: trading-kafka-ui
    ports:
      - "8090:8080"
    depends_on:
      - kafka
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
      KAFKA_CLUSTERS_0_ZOOKEEPER: zookeeper:2181
    networks:
      - trading-network

  # pgAdmin - Database UI (OPTIONAL, FREE)
  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: trading-pgadmin
    ports:
      - "5050:80"
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@trading.com
      PGADMIN_DEFAULT_PASSWORD: admin
    volumes:
      - pgadmin_data:/var/lib/pgadmin
    networks:
      - trading-network

  # Redis Commander - Redis UI (OPTIONAL, FREE)
  redis-commander:
    image: rediscommander/redis-commander:latest
    container_name: trading-redis-ui
    ports:
      - "8081:8081"
    environment:
      REDIS_HOSTS: local:redis:6379
    networks:
      - trading-network

networks:
  trading-network:
    driver: bridge

volumes:
  postgres_data:
  timescale_data:
  redis_data:
  pgadmin_data:
```

**Start everything**:
```bash
# Start all services (first time will download images)
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f

# Stop everything
docker-compose down

# Stop and remove all data
docker-compose down -v
```

**Access UIs**:
- Kafka UI: http://localhost:8090
- pgAdmin: http://localhost:5050 (login: admin@trading.com / admin)
- Redis Commander: http://localhost:8081

---

### Step 3: Configure Environment Variables (FREE)

Create `.env` file in project root:

```bash
# Database Configuration
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=trading_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# TimescaleDB Configuration
TIMESCALE_HOST=localhost
TIMESCALE_PORT=5433
TIMESCALE_DB=market_data
TIMESCALE_USER=postgres
TIMESCALE_PASSWORD=postgres

# Redis Configuration
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka Configuration
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# JWT Configuration
JWT_SECRET=your-super-secret-key-change-this-in-production-min-256-bits
JWT_EXPIRATION=86400000

# Service Ports
AUTH_SERVICE_PORT=8081
USER_SERVICE_PORT=8082
MARKET_DATA_SERVICE_PORT=8083
ORDER_SERVICE_PORT=8084
GATEWAY_PORT=8765

# External API Keys (ALL FREE - Optional)
# Yahoo Finance: No key needed!
# Binance: No key needed for public data!

# Optional: If you want to use these (all have free tiers)
ALPHA_VANTAGE_API_KEY=your_free_key_from_alphavantage.co
FINNHUB_API_KEY=your_free_key_from_finnhub.io
IEX_CLOUD_API_KEY=your_free_key_from_iexcloud.io
```

---

### Step 4: Create Free Market Data Service

Example implementation using **FREE APIs**:

#### DataProviderFactory.java
```java
package com.trading.marketdata.ingestion.provider;

import org.springframework.stereotype.Component;

@Component
public class DataProviderFactory {
    
    /**
     * Get appropriate provider based on market type
     * ALL PROVIDERS ARE FREE!
     */
    public MarketDataProvider getProvider(MarketType marketType) {
        return switch (marketType) {
            case STOCK -> new YahooFinanceProvider();     // FREE
            case CRYPTO -> new BinanceDataProvider();     // FREE
            case FOREX -> new YahooFinanceProvider();     // FREE (supports forex)
        };
    }
}
```

#### YahooFinanceProvider.java (FREE - No API Key)
```java
package com.trading.marketdata.ingestion.provider;

import yahoofinance.YahooFinance;
import yahoofinance.Stock;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class YahooFinanceProvider implements MarketDataProvider {
    
    @Override
    public PriceData getLatestPrice(String symbol) throws DataProviderException {
        try {
            // FREE - No API key needed!
            Stock stock = YahooFinance.get(symbol);
            
            if (stock == null || stock.getQuote() == null) {
                throw new DataProviderException("No data for symbol: " + symbol);
            }
            
            return PriceData.builder()
                .symbol(symbol)
                .price(stock.getQuote().getPrice())
                .volume(stock.getQuote().getVolume())
                .bid(stock.getQuote().getBid())
                .ask(stock.getQuote().getAsk())
                .change(stock.getQuote().getChange())
                .changePercent(stock.getQuote().getChangeInPercent())
                .timestamp(Instant.now())
                .source("YAHOO_FINANCE")
                .build();
                
        } catch (Exception e) {
            log.error("Error fetching from Yahoo Finance: {}", e.getMessage());
            throw new DataProviderException("Yahoo Finance error", e);
        }
    }
    
    @Override
    public List<OHLCVData> getHistoricalData(String symbol, Interval interval, 
                                               Instant from, Instant to) {
        try {
            Stock stock = YahooFinance.get(symbol);
            
            Calendar calFrom = Calendar.getInstance();
            calFrom.setTimeInMillis(from.toEpochMilli());
            
            Calendar calTo = Calendar.getInstance();
            calTo.setTimeInMillis(to.toEpochMilli());
            
            // FREE historical data!
            List<HistoricalQuote> history = stock.getHistory(calFrom, calTo, 
                convertInterval(interval));
            
            return history.stream()
                .map(this::convertToOHLCV)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            throw new DataProviderException("Error fetching historical data", e);
        }
    }
}
```

#### BinanceDataProvider.java (FREE - No API Key for Public Data)
```java
package com.trading.marketdata.ingestion.provider;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class BinanceDataProvider implements MarketDataProvider {
    
    private final WebClient webClient;
    private static final String BASE_URL = "https://api.binance.com";
    
    public BinanceDataProvider() {
        // FREE - No API key needed for public endpoints!
        this.webClient = WebClient.builder()
            .baseUrl(BASE_URL)
            .build();
    }
    
    @Override
    public PriceData getLatestPrice(String symbol) throws DataProviderException {
        try {
            // FREE endpoint - no authentication required!
            String endpoint = "/api/v3/ticker/24hr?symbol=" + symbol;
            
            BinanceTicker ticker = webClient.get()
                .uri(endpoint)
                .retrieve()
                .bodyToMono(BinanceTicker.class)
                .block();
            
            if (ticker == null) {
                throw new DataProviderException("No data for symbol: " + symbol);
            }
            
            return PriceData.builder()
                .symbol(symbol)
                .price(new BigDecimal(ticker.getLastPrice()))
                .volume(new BigDecimal(ticker.getVolume()))
                .high(new BigDecimal(ticker.getHighPrice()))
                .low(new BigDecimal(ticker.getLowPrice()))
                .change(new BigDecimal(ticker.getPriceChange()))
                .changePercent(new BigDecimal(ticker.getPriceChangePercent()))
                .timestamp(Instant.now())
                .source("BINANCE")
                .build();
                
        } catch (Exception e) {
            log.error("Error fetching from Binance: {}", e.getMessage());
            throw new DataProviderException("Binance error", e);
        }
    }
    
    @Override
    public List<OHLCVData> getHistoricalData(String symbol, Interval interval, 
                                               Instant from, Instant to) {
        try {
            // FREE endpoint - candlestick data!
            String endpoint = String.format(
                "/api/v3/klines?symbol=%s&interval=%s&startTime=%d&endTime=%d",
                symbol,
                convertInterval(interval),
                from.toEpochMilli(),
                to.toEpochMilli()
            );
            
            List<List<Object>> klines = webClient.get()
                .uri(endpoint)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<List<Object>>>() {})
                .block();
            
            return klines.stream()
                .map(this::convertToOHLCV)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            throw new DataProviderException("Error fetching Binance klines", e);
        }
    }
    
    /**
     * Convert interval to Binance format
     * 1m, 5m, 15m, 1h, 4h, 1d, 1w, 1M
     */
    private String convertInterval(Interval interval) {
        return switch (interval) {
            case ONE_MINUTE -> "1m";
            case FIVE_MINUTES -> "5m";
            case FIFTEEN_MINUTES -> "15m";
            case ONE_HOUR -> "1h";
            case FOUR_HOURS -> "4h";
            case ONE_DAY -> "1d";
            case ONE_WEEK -> "1w";
            case ONE_MONTH -> "1M";
        };
    }
}
```

---

### Step 5: Run Services Locally (FREE)

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Build common library
cd common-lib
mvn clean install

# 3. Start Eureka (service discovery)
cd services/naming-server
mvn spring-boot:run &

# 4. Start services (each in separate terminal or background)
cd services/auth-service
mvn spring-boot:run &

cd services/user-service
mvn spring-boot:run &

cd services/market-data-service
mvn spring-boot:run &

cd services/order-service
mvn spring-boot:run &

# 5. Start API Gateway (last)
cd services/api-gateway
mvn spring-boot:run &

# Check if services are running
curl http://localhost:8761  # Eureka dashboard
curl http://localhost:8083/actuator/health  # Market Data Service
```

---

## 💡 Cost Comparison: Local vs Cloud

### Running Locally (Your Setup)
```
Infrastructure:        $0/month (Docker on laptop)
Market Data APIs:      $0/month (Yahoo Finance + Binance free)
Storage:              $0/month (local disk)
Development Tools:     $0/month (all open-source)
Internet:             $0/month (you already have it)
Electricity:          ~$2/month (if running 24/7)

TOTAL: $0-2/month
```

### Running on AWS (Production)
```
EKS Cluster:          $73/month
RDS PostgreSQL:       $45/month
ElastiCache Redis:    $25/month
MSK Kafka:            $250/month (minimum)
EC2 for TimescaleDB:  $50/month
Load Balancer:        $20/month
Data Transfer:        $10/month
Domain + SSL:         $2/month

TOTAL: ~$475/month
```

**Savings: $475/month by developing locally!**

---

## 🎓 For Portfolio/Interviews

### What You Can Say in Interviews:

> "I built a production-grade microservices trading platform using Spring Boot, Kafka, Redis, and TimescaleDB. I developed it entirely locally using Docker to minimize costs while maintaining professional infrastructure patterns. The system processes real-time market data from Yahoo Finance and Binance APIs, handles WebSocket streaming for live price updates, and uses event-driven architecture with Kafka for service communication."

### What to Include in README:

```markdown
## 🚀 Quick Start (Zero Cost)

### Prerequisites
- Java 17 (FREE)
- Docker Desktop (FREE)
- Maven (FREE)

### Run Locally
```bash
# Start infrastructure
docker-compose up -d

# Run services
./run-all-services.sh

# Access
- API Gateway: http://localhost:8765
- Eureka: http://localhost:8761
- Kafka UI: http://localhost:8090
- Swagger: http://localhost:8765/swagger-ui.html
```

### Market Data Sources
- **Stocks**: Yahoo Finance (FREE, unlimited)
- **Crypto**: Binance Public API (FREE, 1200 req/min)
- **Total API Cost**: $0/month
```

### Create Demo Video (FREE Tools):

1. **OBS Studio** (FREE) - Screen recording
   - Download: https://obsproject.com/
   - Record your services running
   - Show real-time price updates

2. **ScreenToGif** (FREE) - Create GIFs
   - Download: https://www.screentogif.com/
   - Create animated demos for README

3. **Draw.io** (FREE) - Architecture diagrams
   - https://app.diagrams.net/
   - Create professional diagrams

---

## 📊 Free Monitoring & Debugging Tools

### During Development (All FREE)

```bash
# View Docker logs
docker-compose logs -f [service-name]

# Monitor Redis
redis-cli
> MONITOR
> KEYS *
> GET price:AAPL

# Query TimescaleDB
docker exec -it trading-timescale psql -U postgres -d market_data
> SELECT * FROM price_history WHERE symbol = 'AAPL' ORDER BY time DESC LIMIT 10;

# View Kafka messages
docker exec -it trading-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic price-events \
  --from-beginning

# Spring Boot Actuator (FREE)
curl http://localhost:8083/actuator/health
curl http://localhost:8083/actuator/metrics
```

### Free Monitoring Stack (Optional)

Add to `docker-compose.yml`:

```yaml
  # Prometheus - Metrics (FREE)
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    networks:
      - trading-network

  # Grafana - Dashboards (FREE)
  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
    networks:
      - trading-network
```

Access Grafana: http://localhost:3000 (admin/admin)

---

## 🔧 Troubleshooting (Free Solutions)

### Issue 1: Docker Out of Memory
```bash
# Solution: Increase Docker memory limit
# Docker Desktop → Settings → Resources → Memory: 4GB+
```

### Issue 2: Port Already in Use
```bash
# Find process using port
lsof -i :8083  # macOS/Linux
netstat -ano | findstr :8083  # Windows

# Kill process
kill -9 <PID>  # macOS/Linux
taskkill /PID <PID> /F  # Windows

# Or change port in application.yml
server:
  port: 8084  # Use different port
```

### Issue 3: Yahoo Finance Rate Limited
```bash
# Solution: Add delays between requests
@Scheduled(fixedDelay = 2000)  # Poll every 2 seconds instead of 1
public void pollPrices() { ... }

# Or use multiple providers
if (yahooFinanceFails) {
    fallbackToAlphaVantage();
}
```

### Issue 4: Binance Rate Limit
```bash
# Solution: Binance allows 1200 req/min
# That's 20 req/sec - plenty for development!

# If you hit it, add retry logic:
@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
public PriceData getPrice(String symbol) { ... }
```

---

## 🎯 Optimization Tips (Still Free!)

### 1. Reduce Resource Usage
```yaml
# Limit Docker memory for non-critical services
services:
  redis:
    deploy:
      resources:
        limits:
          memory: 256M
  
  kafka-ui:
    deploy:
      resources:
        limits:
          memory: 512M
```

### 2. Only Run What You Need
```bash
# Don't need Kafka today? Don't start it!
docker-compose up -d postgres redis timescaledb

# Start specific services only
mvn spring-boot:run -pl services/market-data-service
```

### 3. Use Spring Profiles
```yaml
# application-dev.yml (minimal resources)
spring:
  jpa:
    show-sql: false  # Less console spam
  
logging:
  level:
    root: INFO  # Less verbose

# application-demo.yml (for presentations)
spring:
  jpa:
    show-sql: true
    
logging:
  level:
    com.trading: DEBUG
```

---

## 📚 Free Learning Resources

### Documentation
- ✅ Spring Boot: https://spring.io/guides (FREE)
- ✅ Kafka: https://kafka.apache.org/documentation/ (FREE)
- ✅ TimescaleDB: https://docs.timescale.com/ (FREE)
- ✅ Redis: https://redis.io/documentation (FREE)

### Video Tutorials
- ✅ YouTube: Spring Boot microservices (FREE)
- ✅ YouTube: Kafka tutorials (FREE)
- ✅ YouTube: Docker for Java developers (FREE)

### Communities (Get Free Help)
- ✅ Stack Overflow (FREE)
- ✅ Reddit: r/java, r/springboot (FREE)
- ✅ Discord: Spring Developer community (FREE)
- ✅ GitHub Discussions (FREE)

---

## ✅ What You Get for $0

After following this guide, you'll have:

| Feature | Status | Cost |
|---------|--------|------|
| ✅ Complete microservices platform | Working | $0 |
| ✅ Real-time market data | Live prices | $0 |
| ✅ WebSocket streaming | 1000+ connections | $0 |
| ✅ Event-driven architecture | Kafka events | $0 |
| ✅ Time-series database | TimescaleDB | $0 |
| ✅ Caching layer | Redis | $0 |
| ✅ API Gateway | Rate limiting, JWT | $0 |
| ✅ Service discovery | Eureka | $0 |
| ✅ API documentation | Swagger/OpenAPI | $0 |
| ✅ Monitoring | Actuator, logs | $0 |
| ✅ Portfolio-ready project | Impressive! | $0 |

**TOTAL INVESTMENT: $0.00**

---

## 🚀 Next Steps

1. **Start Infrastructure**
   ```bash
   docker-compose up -d
   ```

2. **Build Services**
   ```bash
   cd services/market-data-service
   mvn clean install
   mvn spring-boot:run
   ```

3. **Test Market Data**
   ```bash
   # Get stock price (FREE)
   curl http://localhost:8083/api/v1/market-data/price/AAPL
   
   # Get crypto price (FREE)
   curl http://localhost:8083/api/v1/market-data/price/BTCUSDT
   ```

4. **Create Your Portfolio**
   - Push to GitHub (FREE)
   - Add architecture diagrams (FREE - draw.io)
   - Record demo video (FREE - OBS Studio)
   - Write impressive README (FREE)

5. **Land That Job!** 💼
   - Show off your FREE project
   - Explain your design decisions
   - Demonstrate it running locally
   - Discuss scaling to cloud (without having to pay for it)

---

## 💰 Bottom Line

**You don't need money to build an impressive trading platform.**

Everything in this guide is:
- ✅ 100% Free
- ✅ Production-grade
- ✅ Portfolio-worthy
- ✅ Interview-ready

The only cost is your time and effort - which is an investment in yourself!

---

## 📞 Need Help?

Free support channels:
- 📖 Documentation in this repo
- 💬 GitHub Issues (FREE)
- 🐦 Twitter: #SpringBoot #Java (FREE)
- 💻 Stack Overflow (FREE)

---

**Remember**: Companies like Zerodha, Groww, and Robinhood started small. Your laptop is powerful enough to build a proof-of-concept that demonstrates your skills!

---

*Last Updated: 2024-01-15*  
*Total Cost of This Setup: $0.00*  
*Your Future Value: Priceless* 💎

