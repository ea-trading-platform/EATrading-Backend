package com.eatrading.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.eatrading.api.dto.Quote;
import com.eatrading.api.dto.CandlesResponse;
import com.eatrading.api.services.QuoteService;
import com.eatrading.api.services.FauxnanceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/market")
public class MarketDataController {
    
    private static final Logger logger = LoggerFactory.getLogger(MarketDataController.class);
    
    private final QuoteService quoteService;
    private final FauxnanceClient fauxnanceClient;

    public MarketDataController(QuoteService quoteService, FauxnanceClient fauxnanceClient) {
        this.quoteService = quoteService;
        this.fauxnanceClient = fauxnanceClient;
    }

    /**
     * Get a quote for a symbol
     * GET /api/v1/market/quotes/{symbol}
     */
    @GetMapping("/quotes/{symbol}")
    public ResponseEntity<Quote> getQuote(@PathVariable String symbol) {
        try {
            Quote quote = quoteService.getQuote(symbol);
            return ResponseEntity.ok(quote);
        } catch (Exception e) {
            logger.error("Error fetching quote for {}: {}", symbol, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get bid-ask spread for a symbol
     * GET /api/v1/market/spread/{symbol}
     */
    @GetMapping("/spread/{symbol}")
    public ResponseEntity<Map<String, Double>> getSpread(@PathVariable String symbol) {
        try {
            Map<String, Double> spread = quoteService.getSpread(symbol);
            return ResponseEntity.ok(spread);
        } catch (Exception e) {
            logger.error("Error fetching spread for {}: {}", symbol, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get change metrics for a symbol
     * GET /api/v1/market/changes/{symbol}
     */
    @GetMapping("/changes/{symbol}")
    public ResponseEntity<Map<String, Double>> getChangeMetrics(@PathVariable String symbol) {
        try {
            Map<String, Double> metrics = quoteService.getChangeMetrics(symbol);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            logger.error("Error fetching change metrics for {}: {}", symbol, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get historical candles (OHLCV data)
     * GET /api/v1/market/candles/{symbol}?from=2026-01-01&to=2026-09-28
     */
    @GetMapping("/candles/{symbol}")
    public ResponseEntity<CandlesResponse> getCandles(
            @PathVariable String symbol,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        try {
            // If dates not provided, default to last 30 days
            if (from == null || to == null) {
                java.time.LocalDate today = java.time.LocalDate.now();
                java.time.LocalDate fromDate = today.minusDays(30);
                from = fromDate.toString();
                to = today.toString();
            }
            
            CandlesResponse candles = fauxnanceClient.getCandles(symbol, from, to);
            return ResponseEntity.ok(candles);
        } catch (Exception e) {
            logger.error("Error fetching candles for {}: {}", symbol, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get current price for a symbol
     * GET /api/v1/market/price/{symbol}
     */
    @GetMapping("/price/{symbol}")
    public ResponseEntity<Map<String, Double>> getCurrentPrice(@PathVariable String symbol) {
        try {
            double price = quoteService.getCurrentPrice(symbol);
            return ResponseEntity.ok(Map.of("symbol", (double) symbol.hashCode(), "price", price));
        } catch (Exception e) {
            logger.error("Error fetching price for {}: {}", symbol, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Check API health
     * GET /api/v1/market/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        try {
            String health = fauxnanceClient.checkHealth();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
