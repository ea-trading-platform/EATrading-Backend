# Fauxnance Market Data API Integration

This document describes the integration of the Fauxnance Market Data API into the EA Trading Backend.

## Overview

The Fauxnance API provides real-time and historical market data including:
- **Quotes**: Latest price, bid/ask spreads, and market state
- **Candles**: OHLCV (Open, High, Low, Close, Volume) data for technical analysis
- **Symbols**: Asset metadata and coverage information
- **Health & Usage**: API health checks and quota management

API Base URL: `https://y4t9nq2bqf.execute-api.eu-west-2.amazonaws.com/v1`

## Setup

### 1. Environment Configuration

Add to your `.env` file:

```env
# Fauxnance Market Data API Configuration
FAUXNANCE_API_URL=https://y4t9nq2bqf.execute-api.eu-west-2.amazonaws.com/v1
FAUXNANCE_API_KEY=your-api-key-here
```

See `.env.example` for the complete template.

### 2. Application Properties

The following properties are automatically configured in `application.properties`:

```properties
fauxnance.api.url=${FAUXNANCE_API_URL:https://y4t9nq2bqf.execute-api.eu-west-2.amazonaws.com/v1}
fauxnance.api.key=${FAUXNANCE_API_KEY}
fauxnance.api.timeout=10000
fauxnance.api.cache.ttl=300
```

## Architecture

### Core Components

1. **FauxnanceClient** (`services/FauxnanceClient.java`)
   - Low-level API client using Spring WebClient
   - Handles HTTP requests to Fauxnance API
   - Manages authentication via X-Api-Key header
   - Methods: `getQuote()`, `getCandles()`, `checkHealth()`, `getUsage()`

2. **QuoteService** (`services/QuoteService.java`)
   - High-level service with built-in caching
   - Caches quotes for 5 minutes (configurable)
   - Convenience methods for market data analysis
   - Thread-safe using ConcurrentHashMap

3. **OrderProcessor** (`services/OrderProcessor.java`)
   - Updated to validate orders against real market data
   - Checks order price variance (default 5% tolerance)
   - Logs all validation decisions

4. **MarketDataController** (`controller/MarketDataController.java`)
   - REST API endpoints for market data
   - Base path: `/api/v1/market`

### Data Transfer Objects (DTOs)

- **Quote**: Latest price quote with bid/ask spreads
- **QuoteResponse**: Wrapped quote with metadata
- **Candle**: OHLCV data for a single day
- **CandlesResponse**: Wrapped candles with metadata

## API Endpoints

### Market Data Endpoints

All endpoints are prefixed with `/api/v1/market`

#### Get Quote
```http
GET /api/v1/market/quotes/{symbol}

Response:
{
  "symbol": "AAPL",
  "price": 313.53,
  "bid": 313.5,
  "ask": 313.56,
  "currency": "USD",
  "change": 3.63,
  "changePercent": 1.1713,
  "previousClose": 309.9,
  "asOf": "2026-08-26T16:13:24Z",
  "marketState": "open"
}
```

#### Get Bid-Ask Spread
```http
GET /api/v1/market/spread/{symbol}

Response:
{
  "bid": 313.5,
  "ask": 313.56,
  "spread": 0.06,
  "spreadBps": 1.8587
}
```

#### Get Change Metrics
```http
GET /api/v1/market/changes/{symbol}

Response:
{
  "change": 3.63,
  "changePercent": 1.1713,
  "previousClose": 309.9,
  "currentPrice": 313.53
}
```

#### Get Current Price
```http
GET /api/v1/market/price/{symbol}

Response:
{
  "symbol": -123456789,
  "price": 313.53
}
```

#### Get Historical Candles
```http
GET /api/v1/market/candles/{symbol}?from=2026-01-01&to=2026-09-28

Query Parameters:
- from (optional): Start date (YYYY-MM-DD), defaults to 30 days ago
- to (optional): End date (YYYY-MM-DD), defaults to today

Response:
{
  "symbol": "AAPL",
  "interval": "1d",
  "currency": "USD",
  "candles": [
    {
      "date": "2026-08-26",
      "open": 310.12,
      "high": 314.8,
      "low": 309.55,
      "close": 313.53,
      "adjclose": 313.53,
      "volume": 42317890,
      "synthetic": false
    }
  ]
}
```

#### API Health
```http
GET /api/v1/market/health
```

#### API Usage
```http
GET /api/v1/market/usage
```

#### Cache Management
```http
POST /api/v1/market/cache/clear
GET /api/v1/market/cache/stats
```

## Usage Examples

### Java Code Examples

#### Get a Quote
```java
@Autowired
private QuoteService quoteService;

public void analyzeStock(String symbol) {
    Quote quote = quoteService.getQuote("AAPL");
    System.out.println("Price: " + quote.getPrice());
    System.out.println("Bid: " + quote.getBid());
    System.out.println("Ask: " + quote.getAsk());
}
```

#### Get Spread Information
```java
Map<String, Double> spread = quoteService.getSpread("AAPL");
double bidAskSpread = spread.get("spread");
double spreadBps = spread.get("spreadBps");
```

#### Get Historical Data
```java
@Autowired
private FauxnanceClient fauxnanceClient;

public void analyzeHistory(String symbol) {
    CandlesResponse response = fauxnanceClient.getCandlesLastDays("AAPL", 30);
    List<Candle> candles = response.getData().getCandles();
    
    for (Candle candle : candles) {
        System.out.println("Date: " + candle.getDate());
        System.out.println("Close: " + candle.getClose());
        System.out.println("Volume: " + candle.getVolume());
    }
}
```

### cURL Examples

```bash
# Get quote for AAPL
curl -X GET http://localhost:8099/api/v1/market/quotes/AAPL

# Get bid-ask spread
curl -X GET http://localhost:8099/api/v1/market/spread/AAPL

# Get 30-day candles
curl -X GET "http://localhost:8099/api/v1/market/candles/AAPL?from=2026-08-26&to=2026-09-28"

# Get API health
curl -X GET http://localhost:8099/api/v1/market/health

# Clear cache
curl -X POST http://localhost:8099/api/v1/market/cache/clear

# Get cache stats
curl -X GET http://localhost:8099/api/v1/market/cache/stats
```

## Order Validation

The OrderProcessor now validates orders against real market data:

### Validation Logic

1. **Price Variance Check**: Orders are rejected if the order price deviates more than 5% from the current market price
2. **Market Price Fetch**: Real market data is fetched via QuoteService for validation
3. **Logging**: All validation decisions are logged for audit trail
4. **Market Hours**: An explicit provider market state is honored. For `unknown`, validation uses the current time in the exchange timezone (weekdays, 09:30-16:00 America/New_York for USD and 09:15-15:30 Asia/Kolkata for INR, inclusive). The quote's `asOf` and the order timestamp do not determine whether the market is open. This fallback does not account for exchange holidays or early closes.

The market-hours check is not a quote-freshness check; no maximum quote-age policy is currently enforced. Updating the check does not replay previously consumed orders. The market-open batch runs once per weekday at each market's opening time, without intraday retries.

### Example Order Processing

```java
Order order = new Order(clientId, asset, quantity, true); // Buy order
OrderRequest request = new OrderRequest(order);
OrderResponse response = orderProcessor.process(request);

// Order will be validated against current market price
// If price variance > 5%, order is REJECTED
// Otherwise, order proceeds to execution
```

## Caching Strategy

The QuoteService implements a simple time-based cache:

- **TTL**: 300 seconds (5 minutes) - configurable via `fauxnance.api.cache.ttl`
- **Thread-Safe**: Uses ConcurrentHashMap for thread safety
- **Manual Invalidation**: Can clear cache manually via `clearCache()` or `clearCacheForSymbol(symbol)`

### Cache Configuration

Adjust cache TTL in `application.properties`:
```properties
fauxnance.api.cache.ttl=600  # 10 minutes
```

## Error Handling

The integration includes comprehensive error handling:

1. **API Errors**: WebClientResponseException captured and logged
2. **Network Timeouts**: Configurable timeout (default 10 seconds)
3. **Invalid Symbols**: Returns null or throws exception with detailed message
4. **Rate Limiting**: 429 errors returned by API when quota exceeded

### Handling Exceptions

```java
try {
    Quote quote = quoteService.getQuote("INVALID");
} catch (Exception e) {
    logger.error("Failed to fetch quote: " + e.getMessage());
    // Handle gracefully
}
```

## Supported Symbols

The API supports multiple asset classes:

- **US Equities**: AAPL, MSFT, GOOGL, etc.
- **India Equities**: INFY.NS, TCS.NS, etc.
- **Forex**: FX:EURUSD, FX:GBPUSD, etc.
- **Crypto**: X:BTC-USD, X:ETH-USD, etc.

Example symbols from API docs:
- AAPL (US Equity)
- INFY.NS (India NSE)
- FX:EURUSD (Forex)
- X:BTC-USD (Crypto)

## Important Notes

1. **Educational Data**: Fauxnance provides educational data, not for investment use
2. **Delayed Data**: Market data is delayed and may not reflect real-time prices
3. **Quota Management**: Track usage via `/usage` endpoint
4. **API Key**: Keep your API key secure - never commit to version control
5. **Rate Limits**: Student keys have daily quotas - monitor usage

## Troubleshooting

### Issue: "API Key not found"
**Solution**: Ensure FAUXNANCE_API_KEY is set in .env file

### Issue: "SYMBOL_NOT_FOUND"
**Solution**: Use correct symbol format (e.g., AAPL for US stocks, SYMBOL.NS for India NSE)

### Issue: "RATE_LIMITED"
**Solution**: Your daily quota is exhausted - wait until 00:00 UTC or check your limit

### Issue: Connection timeout
**Solution**: Check API URL is reachable and network connectivity is stable

## Testing

Run the integration tests:
```bash
cd api
mvn test -Dtest=MarketDataControllerTests
```

## Dependencies

Required Maven dependencies (already added to pom.xml):
- spring-boot-starter-webflux: Reactive HTTP client
- spring-boot-starter-web: REST endpoint support
- jackson-databind: JSON parsing

## Performance Considerations

1. **Caching**: Quote caching reduces API calls significantly
2. **Batch Quotes**: Use batch quote endpoint when fetching multiple symbols
3. **Connection Pooling**: WebClient handles connection reuse automatically
4. **Async Operations**: Consider using reactive flows for high-volume data

## Future Enhancements

Potential improvements to consider:
- [ ] Batch quote fetching for multiple symbols
- [ ] Real-time WebSocket connections for market updates
- [ ] Advanced caching with Redis
- [ ] Circuit breaker pattern for resilience
- [ ] Metrics collection for API usage monitoring
- [ ] Support for additional symbol formats and markets
