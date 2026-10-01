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

@Service
public class FauxnanceClient {
    
    private static final Logger logger = LoggerFactory.getLogger(FauxnanceClient.class);
    
    @Value("${fauxnance.api.url}")
    private String apiUrl;
    
    @Value("${fauxnance.api.key}")
    private String apiKey;
    
    @Value("${fauxnance.api.timeout:10000}")
    private int timeout;
    
    private final WebClient webClient;

    public FauxnanceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    /**
     * Get a single quote for a symbol
     * Endpoint: GET /quotes/{symbol}
     */
    public Quote getQuote(String symbol) {
        try {
            String requestUrl = apiUrl + "/quotes/" + symbol;
            logger.info("Calling Fauxnance API: GET {} (with auth header)", requestUrl);
            
            QuoteResponse response = webClient.get()
                .uri(apiUrl + "/quotes/{symbol}", symbol)
                .header("X-Api-Key", apiKey)
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
            CandlesResponse response = webClient.get()
                .uri(apiUrl + "/candles/{symbol}?from={from}&to={to}&interval=1d", symbol, from, to)
                .header("X-Api-Key", apiKey)
                .retrieve()
                .bodyToMono(CandlesResponse.class)
                .timeout(Duration.ofMillis(timeout))
                .block();
            
            if (response != null) {
                logger.info("Retrieved {} candles for symbol: {} from {} to {}", 
                    response.getData().getCandles().size(), symbol, from, to);
                return response;
            }
            
            logger.warn("No candles data returned for symbol: {}", symbol);
            return null;
            
        } catch (WebClientResponseException e) {
            logger.error("API error fetching candles for {}: {} - {}", symbol, e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch candles for symbol: " + symbol, e);
        } catch (Exception e) {
            logger.error("Error fetching candles for symbol: {}", symbol, e);
            throw new RuntimeException("Failed to fetch candles for symbol: " + symbol, e);
        }
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
            String response = webClient.get()
                .uri(apiUrl + "/usage")
                .header("X-Api-Key", apiKey)
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
}
