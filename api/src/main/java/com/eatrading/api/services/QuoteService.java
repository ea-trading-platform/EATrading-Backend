package com.eatrading.api.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.eatrading.api.dto.Quote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QuoteService {
    
    private static final Logger logger = LoggerFactory.getLogger(QuoteService.class);
    
    @Value("${fauxnance.api.cache.ttl:300}")
    private long cacheTtlSeconds;
    
    private final FauxnanceClient fauxnanceClient;
    private final Map<String, CachedQuote> quoteCache = new ConcurrentHashMap<>();

    public QuoteService(FauxnanceClient fauxnanceClient) {
        this.fauxnanceClient = fauxnanceClient;
    }

    /**
     * Get a quote with caching. Stores quotes for cacheTtlSeconds.
     */
    public Quote getQuote(String symbol) {
        String upperSymbol = symbol.toUpperCase();
        
        // Check cache
        CachedQuote cached = quoteCache.get(upperSymbol);
        if (cached != null && !cached.isExpired(cacheTtlSeconds)) {
            logger.debug("Cache hit for symbol: {}", upperSymbol);
            return cached.quote;
        }
        
        // Fetch from API
        logger.info("Cache miss - Fetching live market data from Fauxnance API for symbol: {}", upperSymbol);
        Quote quote = fauxnanceClient.getQuote(upperSymbol);
        logger.info("Successfully fetched quote for {}: price={}, bid={}, ask={}, asOf={}", 
            upperSymbol, quote.getPrice(), quote.getBid(), quote.getAsk(), quote.getAsOf());
        quoteCache.put(upperSymbol, new CachedQuote(quote, System.currentTimeMillis()));
        
        return quote;
    }

    /**
     * Get current price for a symbol
     */
    public double getCurrentPrice(String symbol) {
        logger.info("Getting current price for symbol: {}", symbol);
        Quote quote = getQuote(symbol);
        double price = quote.getPrice();
        logger.info("Current market price for {}: ${}", symbol, price);
        return price;
    }

    /**
     * Get bid-ask spread information
     */
    public Map<String, Double> getSpread(String symbol) {
        Quote quote = getQuote(symbol);
        Map<String, Double> spread = new HashMap<>();
        spread.put("bid", quote.getBid());
        spread.put("ask", quote.getAsk());
        spread.put("spread", quote.getAsk() - quote.getBid());
        spread.put("spreadBps", quote.getSpreadBps());
        return spread;
    }

    /**
     * Get change metrics for a symbol
     */
    public Map<String, Double> getChangeMetrics(String symbol) {
        Quote quote = getQuote(symbol);
        Map<String, Double> metrics = new HashMap<>();
        if (quote.getChange() != null) {
            metrics.put("change", quote.getChange());
        }
        if (quote.getChangePercent() != null) {
            metrics.put("changePercent", quote.getChangePercent());
        }
        if (quote.getPreviousClose() != null) {
            metrics.put("previousClose", quote.getPreviousClose());
        }
        metrics.put("currentPrice", quote.getPrice());
        return metrics;
    }

    /**
     * Clear quote cache
     */
    public void clearCache() {
        quoteCache.clear();
        logger.info("Quote cache cleared");
    }

    /**
     * Clear specific quote from cache
     */
    public void clearCacheForSymbol(String symbol) {
        quoteCache.remove(symbol.toUpperCase());
        logger.debug("Cache cleared for symbol: {}", symbol);
    }

    /**
     * Get cache size
     */
    public int getCacheSize() {
        return quoteCache.size();
    }

    // Inner class for cached quotes
    private static class CachedQuote {
        Quote quote;
        long timestamp;

        CachedQuote(Quote quote, long timestamp) {
            this.quote = quote;
            this.timestamp = timestamp;
        }

        boolean isExpired(long ttlSeconds) {
            return (System.currentTimeMillis() - timestamp) > (ttlSeconds * 1000);
        }
    }
}
