package com.eatrading.api.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.eatrading.api.dto.Quote;
import com.eatrading.api.dto.QuoteResponse;
import com.eatrading.api.dto.CandlesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class FauxnanceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(FauxnanceClient.class);
    private static final String API_KEY_HEADER = "X-Api-Key";
    
    @Value("${fauxnance.api.url}")
    private String apiUrl;
    
    @Value("${fauxnance.api.key}")
    private String apiKey;
    
    @Value("${fauxnance.api.timeout:10000}")
    private int timeout = 10000;

    @Value("${fauxnance.api.candles.cache.ttl:21600}")
    private long candlesCacheTtlSeconds = 21600;

    @Value("${fauxnance.api.daily-limit:2000}")
    private int dailyLimit = 2000;

    @Value("${fauxnance.api.daily-safety-reserve:100}")
    private int dailySafetyReserve = 100;

    private final WebClient webClient;
    private final Map<String, CachedCandles> candlesCache = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<CandlesResponse>> inFlightCandles = new ConcurrentHashMap<>();
    private final AtomicInteger callsToday = new AtomicInteger();
    private volatile String callCountDate = LocalDate.now(ZoneOffset.UTC).toString();

    public FauxnanceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Get a single quote for a symbol
     * Endpoint: GET /quotes/{symbol}
     */
    public Quote getQuote(String symbol) {
        try {
            reserveCall();
            String requestUrl = apiUrl + "/quotes/" + symbol;
            logger.info("Calling Fauxnance API: GET {} (with auth header)", requestUrl);
            
            QuoteResponse response = webClient.get()
                .uri(apiUrl + "/quotes/{symbol}", symbol)
                .header(API_KEY_HEADER, apiKey)
                .retrieve()
                .bodyToMono(QuoteResponse.class)
                .timeout(Duration.ofMillis(timeout))
                .block();
            
            if (response != null && response.getData() != null) {
                Quote quote = response.getData();
                logger.info("✓ API Response received for {}: price={}, bid={}, ask={}, asOf={}", 
                    symbol, quote.getPrice(), quote.getBid(), quote.getAsk(), quote.getAsOf());
                return quote;
            }
            
            logger.warn("No quote data returned for symbol: {}", symbol);
            return null;
            
        } catch (WebClientResponseException e) {
            logger.error("API error fetching quote for {}: {} - {}", symbol, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch quote for symbol: " + symbol, e);
        } catch (Exception e) {
            logger.error("Error fetching quote for symbol: {}", symbol, e);
            throw new RuntimeException("Failed to fetch quote for symbol: " + symbol, e);
        }
    }

    /**
     * Get historical candles (OHLCV data) for a symbol
     * Endpoint: GET /candles/{symbol}
     */
    public CandlesResponse getCandles(String symbol, String from, String to) {
        try {
            String cacheKey = String.join("|", symbol.toUpperCase(), from, to, "1d");
            CachedCandles cached = candlesCache.get(cacheKey);
            if (cached != null && !cached.isExpired(candlesCacheTtlSeconds)) {
                logger.debug("Candle cache hit for {}", cacheKey);
                return cached.response;
            }

            CompletableFuture<CandlesResponse> request = inFlightCandles.computeIfAbsent(cacheKey,
                    key -> CompletableFuture.supplyAsync(() -> fetchCandles(symbol, from, to, key)));
            try {
                return request.join();
            } finally {
                inFlightCandles.remove(cacheKey, request);
            }
        } catch (WebClientResponseException e) {
            logger.error("API error fetching candles for {}: {} - {}", symbol, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch candles for symbol: " + symbol, e);
        } catch (Exception e) {
            logger.error("Error fetching candles for symbol: {}", symbol, e);
            throw new RuntimeException("Failed to fetch candles for symbol: " + symbol, e);
        }
    }

    private CandlesResponse fetchCandles(String symbol, String from, String to, String cacheKey) {
        reserveCall();
        CandlesResponse response = webClient.get()
                .uri(apiUrl + "/candles/{symbol}?from={from}&to={to}&interval=1d", symbol, from, to)
                .header(API_KEY_HEADER, apiKey)
                .retrieve()
                .bodyToMono(CandlesResponse.class)
                .timeout(Duration.ofMillis(timeout))
                .block();
            
            if (response != null) {
                candlesCache.put(cacheKey, new CachedCandles(response, System.currentTimeMillis()));
                logger.info("Retrieved {} candles for symbol: {} from {} to {}", 
                    response.getData().getCandles().size(), symbol, from, to);
                return response;
            }
            
            logger.warn("No candles data returned for symbol: {}", symbol);
            return null;
    }

    /**
     * Get historical candles for the last N days (convenience method)
     */
    public CandlesResponse getCandlesLastDays(String symbol, int days) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate from = today.minusDays(days);
        
        return getCandles(symbol, from.toString(), today.toString());
    }

    /**
     * Check API health and market data freshness
     * Endpoint: GET /health
     */
    public String checkHealth() {
        try {
            reserveCall();
            String response = webClient.get()
                .uri(apiUrl + "/health")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeout))
                .block();
            
            logger.info("API Health check passed");
            return response;
            
        } catch (Exception e) {
            logger.error("Health check failed", e);
            throw new RuntimeException("Failed to check API health", e);
        }
    }

    /**
     * Get the calling key's daily quota status
     * Endpoint: GET /usage
     */
    public String getUsage() {
        try {
            reserveCall();
            String response = webClient.get()
                .uri(apiUrl + "/usage")
                .header(API_KEY_HEADER, apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeout))
                .block();
            
            logger.info("Retrieved API usage information");
            return response;
            
        } catch (WebClientResponseException e) {
            logger.error("API error fetching usage: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch usage information", e);
        } catch (Exception e) {
            logger.error("Error fetching usage", e);
            throw new RuntimeException("Failed to fetch usage information", e);
        }
    }

    private void reserveCall() {
        String today = LocalDate.now(ZoneOffset.UTC).toString();
        synchronized (this) {
            if (!today.equals(callCountDate)) {
                callCountDate = today;
                callsToday.set(0);
            }
            int limit = Math.max(1, dailyLimit - Math.max(0, dailySafetyReserve));
            int calls = callsToday.incrementAndGet();
            if (calls > limit) {
                callsToday.decrementAndGet();
                throw new IllegalStateException("Fauxnance daily API safety limit reached: " + limit);
            }
        }
    }

}
