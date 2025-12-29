# 📊 Market Data Service - Complete Design Document

## Table of Contents
1. [Overview](#overview)
2. [High-Level Design](#high-level-design)
3. [Low-Level Design](#low-level-design)
4. [Technology Stack](#technology-stack)
5. [Database Schema](#database-schema)
6. [API Design](#api-design)
7. [Implementation Phases](#implementation-phases)
8. [Integration Points](#integration-points)
9. [Performance Considerations](#performance-considerations)
10. [Deployment Strategy](#deployment-strategy)

---

## Overview

### Purpose
The Market Data Service is the **heart of real-time data delivery** in the trading platform. It provides:
- ✅ Real-time stock/crypto prices
- ✅ Historical OHLCV (Open, High, Low, Close, Volume) data
- ✅ Market depth (order book snapshots)
- ✅ Tick-by-tick data
- ✅ Technical indicators
- ✅ Market status (open/closed/pre-market)

### Key Characteristics
- **Ultra-low latency**: Sub-10ms response time for cached data
- **High throughput**: Handle 100K+ price updates per second
- **WebSocket streaming**: Push real-time updates to connected clients
- **Event-driven**: Publish price changes to Kafka for other services
- **Multi-source**: Aggregate data from multiple market data providers

---

## High-Level Design

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         External Data Sources                    │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  NSE/BSE API │  │ Crypto APIs  │  │  Forex APIs  │          │
│  │  (Stocks)    │  │(Binance/etc) │  │   (Yahoo)    │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
└─────────┼──────────────────┼──────────────────┼──────────────────┘
          │                  │                  │
          └──────────────────┼──────────────────┘
                             │
                    ┌────────▼────────┐
                    │  Data Ingestion │
                    │   Module (REST/ │
                    │   WebSocket)    │
                    └────────┬────────┘
                             │
          ┏━━━━━━━━━━━━━━━━━┻━━━━━━━━━━━━━━━━━┓
          ┃     MARKET DATA SERVICE             ┃
          ┃                                     ┃
          ┃  ┌─────────────────────────────┐   ┃
          ┃  │   Price Aggregator          │   ┃
          ┃  │   - Normalize data          │   ┃
          ┃  │   - Validate & deduplicate  │   ┃
          ┃  └──────────┬──────────────────┘   ┃
          ┃             │                       ┃
          ┃  ┌──────────▼──────────────────┐   ┃
          ┃  │   Redis Cache Manager       │   ┃
          ┃  │   - Latest price (TTL 5s)   │   ┃
          ┃  │   - Order book (TTL 2s)     │   ┃
          ┃  │   - Market status           │   ┃
          ┃  └──────────┬──────────────────┘   ┃
          ┃             │                       ┃
          ┃  ┌──────────▼──────────────────┐   ┃
          ┃  │   Kafka Event Publisher     │   ┃
          ┃  │   - price-events topic      │   ┃
          ┃  └──────────┬──────────────────┘   ┃
          ┃             │                       ┃
          ┃  ┌──────────▼──────────────────┐   ┃
          ┃  │   TimescaleDB Writer        │   ┃
          ┃  │   - Historical OHLCV        │   ┃
          ┃  │   - Tick data (async)       │   ┃
          ┃  └─────────────────────────────┘   ┃
          ┃                                     ┃
          ┃  ┌─────────────────────────────┐   ┃
          ┃  │   WebSocket Manager         │   ┃
          ┃  │   - Per-symbol subscriptions│   ┃
          ┃  │   - Push to connected users │   ┃
          ┃  └─────────────────────────────┘   ┃
          ┗━━━━━━━━━━━━━━┳━━━━━━━━━━━━━━━━━━━━┛
                         │
         ┌───────────────┼───────────────┐
         │               │               │
    ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
    │ REST    │    │WebSocket│    │ Kafka   │
    │ API     │    │Streaming│    │Publisher│
    └────┬────┘    └────┬────┘    └────┬────┘
         │              │              │
         └──────────────┼──────────────┘
                        │
              ┌─────────▼─────────┐
              │   API Gateway     │
              └─────────┬─────────┘
                        │
              ┌─────────▼─────────┐
              │  Client Apps      │
              │  (Web/Mobile)     │
              └───────────────────┘
```

### Data Flow Patterns

#### 1. Real-Time Price Update Flow
```
External API → Ingestion Module → Price Aggregator → Redis Cache
                                                    ↓
                                          Kafka (price-events)
                                                    ↓
                                          WebSocket Push to Clients
                                                    ↓
                                     TimescaleDB (Async Batch Write)
```

#### 2. Historical Data Query Flow
```
Client → API Gateway → Market Data Service → Check Redis Cache
                                              ↓ (cache miss)
                                        TimescaleDB Query
                                              ↓
                                        Update Redis Cache
                                              ↓
                                        Return to Client
```

### Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Redis for latest prices** | Sub-millisecond reads, TTL for auto-expiry |
| **TimescaleDB for history** | Optimized for time-series data, compression |
| **WebSocket for streaming** | Bidirectional, low overhead, scalable |
| **Kafka for events** | Decouple consumers, replay capability |
| **Async DB writes** | Don't block real-time updates |
| **Multi-source aggregation** | Redundancy, best-price selection |

---

## Low-Level Design

### Component Architecture

```
market-data-service/
├── src/main/java/com/trading/marketdata/
│   ├── MarketDataApplication.java
│   │
│   ├── config/
│   │   ├── RedisConfig.java
│   │   ├── TimescaleDBConfig.java
│   │   ├── KafkaProducerConfig.java
│   │   ├── WebSocketConfig.java
│   │   ├── RestTemplateConfig.java
│   │   └── SchedulerConfig.java
│   │
│   ├── controller/
│   │   ├── MarketDataController.java         # REST endpoints
│   │   └── WebSocketController.java           # WebSocket handlers
│   │
│   ├── service/
│   │   ├── PriceService.java                  # Core price logic
│   │   ├── OHLCVService.java                  # Historical candles
│   │   ├── OrderBookService.java              # Market depth
│   │   ├── TickerService.java                 # 24h stats
│   │   ├── WebSocketBroadcaster.java          # Push notifications
│   │   └── MarketStatusService.java           # Open/Close tracking
│   │
│   ├── ingestion/
│   │   ├── DataIngestionScheduler.java        # Scheduled polling
│   │   ├── provider/
│   │   │   ├── MarketDataProvider.java        # Interface
│   │   │   ├── NSEDataProvider.java           # NSE implementation
│   │   │   ├── BinanceDataProvider.java       # Crypto
│   │   │   └── YahooFinanceProvider.java      # Fallback
│   │   └── normalizer/
│   │       └── PriceNormalizer.java           # Standardize formats
│   │
│   ├── kafka/
│   │   ├── producer/
│   │   │   └── PriceEventProducer.java
│   │   └── event/
│   │       ├── PriceUpdatedEvent.java
│   │       ├── OrderBookUpdatedEvent.java
│   │       └── MarketStatusEvent.java
│   │
│   ├── cache/
│   │   ├── RedisCacheService.java             # Wrapper for Redis ops
│   │   └── CacheKeyGenerator.java             # Consistent key naming
│   │
│   ├── repository/
│   │   ├── PriceHistoryRepository.java        # TimescaleDB
│   │   ├── OHLCVRepository.java
│   │   ├── TickDataRepository.java
│   │   └── MarketSymbolRepository.java        # Metadata (PostgreSQL)
│   │
│   ├── model/
│   │   ├── entity/
│   │   │   ├── MarketSymbol.java              # Symbol metadata
│   │   │   ├── PriceHistory.java              # Tick data
│   │   │   └── OHLCV.java                     # Candle data
│   │   └── enums/
│   │       ├── MarketType.java                # STOCK, CRYPTO, FOREX
│   │       ├── Interval.java                  # 1m, 5m, 1h, 1d
│   │       └── MarketStatus.java              # OPEN, CLOSED, PRE_MARKET
│   │
│   ├── dto/
│   │   ├── request/
│   │   │   ├── HistoricalDataRequest.java
│   │   │   └── SubscriptionRequest.java
│   │   └── response/
│   │       ├── PriceResponse.java
│   │       ├── OHLCVResponse.java
│   │       ├── OrderBookResponse.java
│   │       └── TickerResponse.java
│   │
│   ├── websocket/
│   │   ├── handler/
│   │   │   └── MarketDataWebSocketHandler.java
│   │   ├── session/
│   │   │   └── WebSocketSessionManager.java   # Track subscriptions
│   │   └── message/
│   │       ├── SubscribeMessage.java
│   │       ├── UnsubscribeMessage.java
│   │       └── PriceUpdateMessage.java
│   │
│   ├── scheduler/
│   │   ├── PricePollingScheduler.java         # Poll external APIs
│   │   ├── OHLCVAggregationScheduler.java     # Generate candles
│   │   └── MarketStatusScheduler.java         # Update market hours
│   │
│   ├── util/
│   │   ├── PriceCalculator.java               # Calculate % change, etc.
│   │   └── TimeUtils.java                     # Timezone handling
│   │
│   └── exception/
│       ├── DataProviderException.java
│       ├── InvalidSymbolException.java
│       └── MarketClosedException.java
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   └── timescale-schema.sql
│
└── pom.xml
```

### Core Classes Design

#### 1. PriceService.java
```java
@Service
public class PriceService {
    
    private final RedisCacheService cacheService;
    private final PriceEventProducer kafkaProducer;
    private final PriceHistoryRepository historyRepository;
    private final WebSocketBroadcaster broadcaster;
    
    /**
     * Get latest price for a symbol
     * Flow: Redis → TimescaleDB (if miss) → External API (if outdated)
     */
    public PriceResponse getLatestPrice(String symbol);
    
    /**
     * Update price from external source
     * Flow: Validate → Redis → Kafka → WebSocket → Async DB
     */
    public void updatePrice(String symbol, BigDecimal price, Instant timestamp);
    
    /**
     * Get prices for multiple symbols (bulk operation)
     */
    public Map<String, PriceResponse> getMultiplePrices(List<String> symbols);
    
    /**
     * Check if price is stale (TTL expired)
     */
    private boolean isPriceStale(PriceResponse cached);
}
```

#### 2. DataIngestionScheduler.java
```java
@Component
public class DataIngestionScheduler {
    
    private final List<MarketDataProvider> providers;
    private final PriceService priceService;
    
    /**
     * Poll active symbols every 1 second during market hours
     * Reduced to every 5 seconds after hours
     */
    @Scheduled(fixedDelay = 1000)
    public void pollActivePrices();
    
    /**
     * Poll order book for top symbols every 2 seconds
     */
    @Scheduled(fixedDelay = 2000)
    public void pollOrderBooks();
    
    /**
     * Sync market symbols metadata (new listings) - once per hour
     */
    @Scheduled(cron = "0 0 * * * *")
    public void syncMarketSymbols();
}
```

#### 3. WebSocketBroadcaster.java
```java
@Service
public class WebSocketBroadcaster {
    
    private final WebSocketSessionManager sessionManager;
    
    /**
     * Broadcast price update to all subscribers of a symbol
     */
    public void broadcastPriceUpdate(String symbol, PriceUpdateMessage message);
    
    /**
     * Send order book update to subscribers
     */
    public void broadcastOrderBook(String symbol, OrderBookResponse orderBook);
    
    /**
     * Notify all sessions about market status change
     */
    public void broadcastMarketStatus(MarketStatus status);
    
    /**
     * Get active subscription count for a symbol
     */
    public int getSubscriberCount(String symbol);
}
```

---

## Technology Stack

### Core Technologies

| Technology | Version | Purpose | Why This Choice? |
|------------|---------|---------|------------------|
| **Java** | 17 | Runtime | LTS, performance, ecosystem |
| **Spring Boot** | 3.2.x | Framework | Production-ready, microservices support |
| **Spring WebSocket** | — | Real-time | Native Spring integration |
| **Redis** | 7.x | Cache | Sub-ms latency, pub/sub support |
| **TimescaleDB** | 2.x | Time-series DB | PostgreSQL + time-series optimization |
| **Kafka** | 3.x | Event streaming | High throughput, decoupling |
| **Docker** | — | Containerization | Consistent environments |

### Key Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-websocket</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- Kafka -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    
    <!-- TimescaleDB (PostgreSQL driver) -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    
    <!-- WebSocket STOMP -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-reactor-netty</artifactId>
    </dependency>
    
    <!-- HTTP Client for external APIs -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    
    <!-- JSON Processing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
    
    <!-- API Documentation -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.3.0</version>
    </dependency>
    
    <!-- Metrics & Monitoring -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
    
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### External Data Providers

| Provider | Type | Use Case | Free Tier? |
|----------|------|----------|------------|
| **Yahoo Finance API** | Stocks | US & Global markets | ✅ Yes |
| **Alpha Vantage** | Stocks | Historical data | ✅ Limited |
| **Binance API** | Crypto | Real-time crypto prices | ✅ Yes |
| **CoinGecko** | Crypto | Fallback for crypto | ✅ Yes |
| **NSE/BSE** | Stocks | Indian markets | ⚠️ Registration needed |

---

## Database Schema

### TimescaleDB Schema (Time-Series Data)

#### 1. price_history (Hypertable)
```sql
-- Tick-by-tick price data
CREATE TABLE price_history (
    time                TIMESTAMPTZ NOT NULL,
    symbol              VARCHAR(20) NOT NULL,
    price               NUMERIC(18, 8) NOT NULL,
    volume              NUMERIC(18, 8),
    bid_price           NUMERIC(18, 8),
    ask_price           NUMERIC(18, 8),
    market_type         VARCHAR(10), -- STOCK, CRYPTO, FOREX
    source              VARCHAR(50), -- NSE, BINANCE, etc.
    PRIMARY KEY (time, symbol)
);

-- Convert to hypertable (TimescaleDB specific)
SELECT create_hypertable('price_history', 'time');

-- Create indexes
CREATE INDEX idx_price_history_symbol ON price_history (symbol, time DESC);
CREATE INDEX idx_price_history_market_type ON price_history (market_type, time DESC);

-- Retention policy: Keep tick data for 30 days
SELECT add_retention_policy('price_history', INTERVAL '30 days');

-- Compression policy: Compress data older than 7 days
ALTER TABLE price_history SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'symbol'
);
SELECT add_compression_policy('price_history', INTERVAL '7 days');
```

#### 2. ohlcv (Hypertable)
```sql
-- Candlestick data (1m, 5m, 15m, 1h, 1d)
CREATE TABLE ohlcv (
    time                TIMESTAMPTZ NOT NULL,
    symbol              VARCHAR(20) NOT NULL,
    interval            VARCHAR(5) NOT NULL, -- 1m, 5m, 15m, 1h, 1d
    open                NUMERIC(18, 8) NOT NULL,
    high                NUMERIC(18, 8) NOT NULL,
    low                 NUMERIC(18, 8) NOT NULL,
    close               NUMERIC(18, 8) NOT NULL,
    volume              NUMERIC(18, 8) NOT NULL,
    trades_count        INTEGER,
    PRIMARY KEY (time, symbol, interval)
);

SELECT create_hypertable('ohlcv', 'time');
CREATE INDEX idx_ohlcv_symbol_interval ON ohlcv (symbol, interval, time DESC);

-- Retention: Keep forever (or 2 years for non-major symbols)
-- Compress after 30 days
ALTER TABLE ohlcv SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'symbol, interval'
);
SELECT add_compression_policy('ohlcv', INTERVAL '30 days');
```

#### 3. order_book_snapshot (Hypertable)
```sql
-- Order book snapshots (market depth)
CREATE TABLE order_book_snapshot (
    time                TIMESTAMPTZ NOT NULL,
    symbol              VARCHAR(20) NOT NULL,
    bids                JSONB NOT NULL, -- [{price, quantity}, ...]
    asks                JSONB NOT NULL,
    spread              NUMERIC(18, 8),
    PRIMARY KEY (time, symbol)
);

SELECT create_hypertable('order_book_snapshot', 'time');
CREATE INDEX idx_orderbook_symbol ON order_book_snapshot (symbol, time DESC);

-- Retention: 7 days only (large data volume)
SELECT add_retention_policy('order_book_snapshot', INTERVAL '7 days');
```

#### 4. Continuous Aggregates (Materialized Views)
```sql
-- Pre-compute daily stats for fast queries
CREATE MATERIALIZED VIEW daily_market_stats
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', time) AS day,
    symbol,
    first(price, time) AS open,
    max(price) AS high,
    min(price) AS low,
    last(price, time) AS close,
    sum(volume) AS volume,
    count(*) AS tick_count
FROM price_history
GROUP BY day, symbol;

-- Refresh policy: Update every hour
SELECT add_continuous_aggregate_policy('daily_market_stats',
    start_offset => INTERVAL '3 days',
    end_offset => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour'
);
```

### PostgreSQL Schema (Metadata)

#### 1. market_symbol
```sql
CREATE TABLE market_symbol (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    symbol              VARCHAR(20) UNIQUE NOT NULL,
    name                VARCHAR(200) NOT NULL,
    market_type         VARCHAR(10) NOT NULL, -- STOCK, CRYPTO, FOREX
    exchange            VARCHAR(50), -- NSE, BSE, BINANCE
    is_active           BOOLEAN DEFAULT true,
    tradable            BOOLEAN DEFAULT true,
    tick_size           NUMERIC(18, 8),
    lot_size            INTEGER,
    metadata            JSONB, -- Additional info
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_market_symbol_type ON market_symbol (market_type, is_active);
CREATE INDEX idx_market_symbol_exchange ON market_symbol (exchange);
```

#### 2. market_status
```sql
CREATE TABLE market_status (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exchange            VARCHAR(50) NOT NULL,
    status              VARCHAR(20) NOT NULL, -- OPEN, CLOSED, PRE_MARKET, POST_MARKET
    market_date         DATE NOT NULL,
    open_time           TIMESTAMPTZ,
    close_time          TIMESTAMPTZ,
    is_holiday          BOOLEAN DEFAULT false,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(exchange, market_date)
);
```

### Redis Data Structures

```
# Latest price (Hash)
Key: price:{symbol}
Fields:
  - price: "150.75"
  - timestamp: "2024-01-15T10:30:00Z"
  - volume: "1500000"
  - change_percent: "2.5"
  - bid: "150.70"
  - ask: "150.80"
TTL: 5 seconds

# Order Book (Sorted Set)
Key: orderbook:{symbol}:bids
Members: score=price, value=quantity
TTL: 2 seconds

Key: orderbook:{symbol}:asks
Members: score=price, value=quantity
TTL: 2 seconds

# 24h Ticker Stats (Hash)
Key: ticker:{symbol}:24h
Fields:
  - open: "147.50"
  - high: "151.20"
  - low: "146.80"
  - volume: "5000000"
TTL: 60 seconds

# Active Symbols List (Set)
Key: symbols:active
Members: ["AAPL", "GOOGL", "TSLA", ...]
TTL: Never (managed manually)

# Market Status (String)
Key: market:status:{exchange}
Value: "OPEN" | "CLOSED" | "PRE_MARKET"
TTL: 60 seconds
```

---

## API Design

### REST Endpoints

#### 1. Get Latest Price
```http
GET /api/v1/market-data/price/{symbol}

Response 200:
{
  "symbol": "AAPL",
  "price": 150.75,
  "timestamp": "2024-01-15T10:30:00Z",
  "volume": 1500000,
  "change": 2.50,
  "changePercent": 1.68,
  "bid": 150.70,
  "ask": 150.80,
  "marketType": "STOCK",
  "source": "YAHOO_FINANCE"
}
```

#### 2. Get Multiple Prices (Bulk)
```http
GET /api/v1/market-data/prices?symbols=AAPL,GOOGL,TSLA

Response 200:
{
  "data": {
    "AAPL": { ... },
    "GOOGL": { ... },
    "TSLA": { ... }
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### 3. Get Historical OHLCV
```http
GET /api/v1/market-data/ohlcv/{symbol}
Query Params:
  - interval: 1m, 5m, 15m, 1h, 1d (required)
  - from: ISO timestamp (required)
  - to: ISO timestamp (optional, default: now)
  - limit: max candles (optional, default: 100, max: 1000)

Example:
GET /api/v1/market-data/ohlcv/AAPL?interval=1h&from=2024-01-01T00:00:00Z&limit=24

Response 200:
{
  "symbol": "AAPL",
  "interval": "1h",
  "candles": [
    {
      "time": "2024-01-01T00:00:00Z",
      "open": 147.50,
      "high": 148.20,
      "low": 147.10,
      "close": 148.00,
      "volume": 500000
    },
    ...
  ]
}
```

#### 4. Get Order Book (Market Depth)
```http
GET /api/v1/market-data/orderbook/{symbol}
Query Params:
  - depth: 5, 10, 20 (default: 10)

Response 200:
{
  "symbol": "AAPL",
  "timestamp": "2024-01-15T10:30:00Z",
  "bids": [
    { "price": 150.70, "quantity": 1000 },
    { "price": 150.65, "quantity": 1500 },
    ...
  ],
  "asks": [
    { "price": 150.80, "quantity": 800 },
    { "price": 150.85, "quantity": 1200 },
    ...
  ],
  "spread": 0.10
}
```

#### 5. Get 24h Ticker Stats
```http
GET /api/v1/market-data/ticker/{symbol}

Response 200:
{
  "symbol": "AAPL",
  "open24h": 147.50,
  "high24h": 151.20,
  "low24h": 146.80,
  "volume24h": 5000000,
  "priceChange24h": 3.25,
  "priceChangePercent24h": 2.20
}
```

#### 6. Get Market Status
```http
GET /api/v1/market-data/status
Query Params:
  - exchange: NSE, BSE, NYSE, BINANCE (optional, default: all)

Response 200:
{
  "statuses": [
    {
      "exchange": "NSE",
      "status": "OPEN",
      "marketDate": "2024-01-15",
      "openTime": "2024-01-15T03:45:00Z",
      "closeTime": "2024-01-15T10:00:00Z"
    },
    ...
  ]
}
```

#### 7. Get Active Symbols
```http
GET /api/v1/market-data/symbols
Query Params:
  - marketType: STOCK, CRYPTO, FOREX (optional)
  - exchange: NSE, BINANCE, etc. (optional)
  - page: page number (default: 0)
  - size: page size (default: 50)

Response 200:
{
  "symbols": [
    {
      "symbol": "AAPL",
      "name": "Apple Inc.",
      "marketType": "STOCK",
      "exchange": "NASDAQ",
      "isActive": true
    },
    ...
  ],
  "totalElements": 500,
  "page": 0,
  "size": 50
}
```

### WebSocket Protocol

#### Connection
```
ws://localhost:8083/ws/market-data
(or via Gateway: ws://localhost:8765/market-data-service/ws/market-data)
```

#### Subscribe to Symbol
```json
// Client sends:
{
  "action": "SUBSCRIBE",
  "symbols": ["AAPL", "GOOGL"],
  "types": ["PRICE", "ORDERBOOK"] // Optional, default: ["PRICE"]
}

// Server confirms:
{
  "type": "SUBSCRIPTION_CONFIRMED",
  "symbols": ["AAPL", "GOOGL"],
  "timestamp": "2024-01-15T10:30:00Z"
}
```

#### Receive Price Updates
```json
// Server pushes:
{
  "type": "PRICE_UPDATE",
  "symbol": "AAPL",
  "price": 150.75,
  "timestamp": "2024-01-15T10:30:00.123Z",
  "volume": 1000,
  "change": 0.25,
  "changePercent": 0.17
}
```

#### Unsubscribe
```json
// Client sends:
{
  "action": "UNSUBSCRIBE",
  "symbols": ["AAPL"]
}

// Server confirms:
{
  "type": "UNSUBSCRIPTION_CONFIRMED",
  "symbols": ["AAPL"],
  "timestamp": "2024-01-15T10:31:00Z"
}
```

#### Heartbeat/Ping
```json
// Client sends every 30s:
{
  "action": "PING"
}

// Server responds:
{
  "type": "PONG",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Kafka Events Published

#### 1. PriceUpdatedEvent
```json
Topic: price-events
Key: symbol (for partitioning)

{
  "eventType": "PRICE_UPDATED",
  "eventId": "uuid",
  "timestamp": "2024-01-15T10:30:00.123Z",
  "symbol": "AAPL",
  "price": 150.75,
  "previousPrice": 150.50,
  "volume": 1000,
  "marketType": "STOCK",
  "source": "YAHOO_FINANCE"
}
```

#### 2. OrderBookUpdatedEvent
```json
Topic: orderbook-events

{
  "eventType": "ORDERBOOK_UPDATED",
  "eventId": "uuid",
  "timestamp": "2024-01-15T10:30:00.123Z",
  "symbol": "AAPL",
  "topBid": 150.70,
  "topAsk": 150.80,
  "spread": 0.10
}
```

#### 3. MarketStatusEvent
```json
Topic: market-status-events

{
  "eventType": "MARKET_STATUS_CHANGED",
  "eventId": "uuid",
  "timestamp": "2024-01-15T03:45:00Z",
  "exchange": "NSE",
  "status": "OPEN", // OPEN, CLOSED, PRE_MARKET
  "marketDate": "2024-01-15"
}
```

---

## Implementation Phases

### Phase 1: Foundation (Week 1) ⭐ **START HERE**

**Goal**: Basic REST API with Redis caching

#### Tasks:
1. ✅ **Project Setup**
   - Create Spring Boot project
   - Configure Maven dependencies
   - Setup package structure

2. ✅ **Database Setup**
   - Configure TimescaleDB connection
   - Run schema creation scripts
   - Setup Redis connection

3. ✅ **Core Entities**
   - `MarketSymbol` entity
   - `PriceHistory` entity
   - `OHLCV` entity

4. ✅ **Basic REST API**
   - GET `/price/{symbol}` - Latest price
   - GET `/symbols` - List symbols
   - Basic error handling

5. ✅ **Redis Integration**
   - Cache latest prices
   - TTL configuration
   - Cache-aside pattern

**Deliverables**:
- Service starts successfully
- Can fetch price (mock data)
- Redis caching works
- API returns JSON responses

**Testing**:
```bash
# Start service
mvn spring-boot:run

# Test endpoints
curl http://localhost:8083/api/v1/market-data/price/AAPL
curl http://localhost:8083/api/v1/market-data/symbols
```

---

### Phase 2: External Data Integration (Week 2)

**Goal**: Connect to real market data providers

#### Tasks:
1. ✅ **Data Provider Interface**
   - Abstract `MarketDataProvider` interface
   - Error handling & retries

2. ✅ **Provider Implementations**
   - Yahoo Finance integration
   - Binance API integration
   - Response parsing & normalization

3. ✅ **Scheduled Data Ingestion**
   - Poll prices every 1 second
   - Store in Redis
   - Async write to TimescaleDB

4. ✅ **Price Service Enhancement**
   - Real-time price updates
   - Multi-source aggregation
   - Fallback logic

**Deliverables**:
- Live prices from Yahoo Finance
- Crypto prices from Binance
- Scheduler running continuously
- Data persisted to TimescaleDB

**Testing**:
```bash
# Check logs for ingestion
tail -f logs/market-data-service.log | grep "Price updated"

# Query TimescaleDB
psql -h localhost -p 5433 -U postgres -d market_data
SELECT * FROM price_history WHERE symbol = 'AAPL' ORDER BY time DESC LIMIT 10;
```

---

### Phase 3: Historical Data & OHLCV (Week 3)

**Goal**: Provide candlestick data and charts

#### Tasks:
1. ✅ **OHLCV Aggregation**
   - Generate 1m, 5m, 15m, 1h, 1d candles
   - Scheduled job to compute from ticks
   - Store in `ohlcv` table

2. ✅ **Historical API**
   - GET `/ohlcv/{symbol}` endpoint
   - Query TimescaleDB
   - Pagination support

3. ✅ **Continuous Aggregates**
   - Setup TimescaleDB continuous aggregates
   - Pre-compute daily stats

4. ✅ **Caching Strategy**
   - Cache recent OHLCV data in Redis
   - Invalidation on new candle

**Deliverables**:
- OHLCV data available
- Charts can be rendered
- Fast query performance

---

### Phase 4: WebSocket Streaming (Week 4) ⭐ **HIGH PRIORITY**

**Goal**: Real-time price push to clients

#### Tasks:
1. ✅ **WebSocket Configuration**
   - STOMP over WebSocket
   - Connection handling

2. ✅ **Subscription Management**
   - Per-user symbol subscriptions
   - Session tracking

3. ✅ **Broadcast Mechanism**
   - Push price updates to subscribers
   - Efficient message routing

4. ✅ **Load Testing**
   - 1000 concurrent connections
   - 100 updates/sec per symbol

**Deliverables**:
- WebSocket server running
- Clients receive live updates
- Handles reconnections gracefully

**Testing**:
```javascript
// JavaScript client test
const ws = new WebSocket('ws://localhost:8083/ws/market-data');

ws.onopen = () => {
  ws.send(JSON.stringify({
    action: 'SUBSCRIBE',
    symbols: ['AAPL', 'GOOGL']
  }));
};

ws.onmessage = (event) => {
  console.log('Price update:', JSON.parse(event.data));
};
```

---

### Phase 5: Kafka Event Publishing (Week 5)

**Goal**: Enable event-driven architecture

#### Tasks:
1. ✅ **Kafka Producer**
   - Configure Kafka producer
   - Publish `PriceUpdatedEvent`

2. ✅ **Event Schema**
   - Define event models
   - Serialization (JSON/Avro)

3. ✅ **Async Publishing**
   - Non-blocking Kafka send
   - Error handling

4. ✅ **Dead Letter Queue**
   - Handle failed publishes

**Deliverables**:
- Events published to Kafka
- Other services can consume
- Reliable delivery

---

### Phase 6: Order Book & Market Depth (Week 6)

**Goal**: Provide bid/ask data

#### Tasks:
1. ✅ **Order Book Data Model**
   - Store top 10 bids/asks
   - JSONB in TimescaleDB

2. ✅ **Order Book API**
   - GET `/orderbook/{symbol}`
   - Real-time updates via WebSocket

3. ✅ **Provider Integration**
   - Fetch from Binance (crypto)
   - Mock for stocks (unless premium API)

**Deliverables**:
- Order book data available
- Updated every 2 seconds

---

### Phase 7: Advanced Features (Week 7-8)

**Goal**: Production-ready enhancements

#### Tasks:
1. ✅ **Ticker Stats**
   - 24h high/low/volume
   - GET `/ticker/{symbol}`

2. ✅ **Market Status**
   - Track market hours
   - Auto-adjust polling frequency

3. ✅ **Symbol Search**
   - Fuzzy search by name
   - Elasticsearch integration (optional)

4. ✅ **Rate Limiting**
   - Per-user request limits
   - Redis-backed counters

5. ✅ **Monitoring**
   - Prometheus metrics
   - Grafana dashboards

**Deliverables**:
- Full feature parity
- Production monitoring

---

## Integration Points

### With Other Services

#### 1. Order Service
**Consumes**: Latest prices for order validation
```
Order Service → REST API → Market Data Service
GET /price/{symbol}
```

**Use Case**: Validate limit order price is within market range

---

#### 2. Portfolio Service
**Consumes**: Kafka `price-events`
```
Market Data Service → Kafka → Portfolio Service
Topic: price-events
```

**Use Case**: Real-time P&L calculation on price updates

---

#### 3. Risk Service
**Consumes**: Kafka `price-events`
```
Market Data Service → Kafka → Risk Service
Topic: price-events
```

**Use Case**: Margin call calculation, position limits

---

#### 4. Notification Service
**Consumes**: Kafka `price-events`
```
Market Data Service → Kafka → Notification Service
Topic: price-events
```

**Use Case**: Price alerts when target reached

---

#### 5. Analytics Service
**Consumes**: Kafka `price-events`, Historical data via REST
```
Market Data Service → Kafka → Kinesis → Analytics Service
```

**Use Case**: Trading volume analysis, market trends

---

#### 6. API Gateway
**Routes**: All external client requests
```
Client → API Gateway → Market Data Service
```

**Auth**: JWT validation at gateway, service trusts gateway

---

## Performance Considerations

### Throughput Targets

| Metric | Target | Strategy |
|--------|--------|----------|
| **Price updates/sec** | 100,000+ | Redis, async writes |
| **WebSocket connections** | 10,000+ | Netty, efficient session mgmt |
| **API latency (cached)** | <10ms | Redis, no DB hit |
| **API latency (DB)** | <50ms | TimescaleDB indexes, partitioning |
| **Kafka throughput** | 50,000 msgs/sec | Batching, compression |

### Optimization Strategies

#### 1. Caching Layer
```
Request Flow:
1. Check Redis (5s TTL)
2. If miss → Query TimescaleDB
3. Update Redis
4. Return to client

Cache Hit Ratio Target: >95%
```

#### 2. Database Optimization
```sql
-- TimescaleDB optimizations
SET timescaledb.max_background_workers = 8;
SET shared_buffers = '4GB';
SET effective_cache_size = '12GB';

-- Parallel queries
SET max_parallel_workers_per_gather = 4;
```

#### 3. Connection Pooling
```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

#### 4. Async Processing
```java
@Async("marketDataExecutor")
public CompletableFuture<Void> writeToTimescaleDB(PriceHistory price) {
    // Non-blocking write
    historyRepository.save(price);
    return CompletableFuture.completedFuture(null);
}
```

### Monitoring Metrics

```yaml
# Key metrics to track
metrics:
  - name: market_data_ingestion_rate
    type: counter
    description: Price updates ingested per second
  
  - name: market_data_cache_hit_ratio
    type: gauge
    description: Redis cache hit percentage
  
  - name: market_data_api_latency
    type: histogram
    description: API response time (p50, p95, p99)
  
  - name: market_data_websocket_connections
    type: gauge
    description: Active WebSocket connections
  
  - name: market_data_kafka_publish_rate
    type: counter
    description: Events published to Kafka per second
```

---

## Deployment Strategy

### Local Development
```yaml
# docker-compose.yml
version: '3.8'
services:
  timescaledb:
    image: timescale/timescaledb:latest-pg14
    ports:
      - "5433:5432"
    environment:
      POSTGRES_DB: market_data
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    volumes:
      - timescale_data:/var/lib/postgresql/data
      - ./timescale-schema.sql:/docker-entrypoint-initdb.d/init.sql
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    command: redis-server --maxmemory 512mb --maxmemory-policy allkeys-lru
  
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    # ... (see existing docker-compose)
  
  market-data-service:
    build: ./services/market-data-service
    ports:
      - "8083:8083"
    depends_on:
      - timescaledb
      - redis
      - kafka
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://timescaledb:5432/market_data
      SPRING_REDIS_HOST: redis
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
```

### AWS Deployment

```
┌─────────────────────────────────────────────────┐
│                  ALB (HTTPS)                    │
└─────────────────┬───────────────────────────────┘
                  │
      ┌───────────▼───────────┐
      │    API Gateway        │
      └───────────┬───────────┘
                  │
      ┌───────────▼───────────────┐
      │  EKS - Market Data Pods   │
      │  (3 replicas, HPA)        │
      └───┬──────────────┬────────┘
          │              │
   ┌──────▼──────┐  ┌───▼────────┐
   │ ElastiCache │  │  MSK       │
   │ (Redis)     │  │  (Kafka)   │
   └─────────────┘  └────────────┘
          │
   ┌──────▼──────────────┐
   │ TimescaleDB on EC2  │
   │ (r6g.2xlarge)       │
   └─────────────────────┘
```

**Scaling Configuration**:
```yaml
# k8s/hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: market-data-service-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: market-data-service
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Pods
    pods:
      metric:
        name: websocket_connections
      target:
        type: AverageValue
        averageValue: "2000"
```

---

## Summary

### Implementation Priority

1. **Week 1**: Basic REST API + Redis ✅ **Start Here**
2. **Week 2**: External data providers (Yahoo, Binance)
3. **Week 3**: Historical OHLCV data
4. **Week 4**: WebSocket real-time streaming ⭐ **Critical**
5. **Week 5**: Kafka event publishing
6. **Week 6**: Order book & market depth
7. **Week 7-8**: Advanced features & monitoring

### Success Criteria

| Metric | Target |
|--------|--------|
| API latency (cached) | <10ms |
| WebSocket connections | 10,000+ |
| Price updates/sec | 100,000+ |
| Cache hit ratio | >95% |
| Uptime | 99.9% |

### Key Takeaways

✅ **Redis** for ultra-fast price reads  
✅ **TimescaleDB** for efficient time-series storage  
✅ **WebSocket** for real-time client updates  
✅ **Kafka** for event-driven architecture  
✅ **Async writes** to avoid blocking real-time flow  
✅ **Multi-source** data aggregation for reliability  

---

## Next Steps

1. Create the Spring Boot project structure
2. Setup TimescaleDB and Redis via Docker Compose
3. Implement Phase 1 (Basic REST API)
4. Test with mock data
5. Proceed to Phase 2 (External APIs)

**Reference**: See `02_QUICK_START_GUIDE.md` for environment setup.

---

*Last Updated: 2024-01-15*
*Document Version: 1.0*

