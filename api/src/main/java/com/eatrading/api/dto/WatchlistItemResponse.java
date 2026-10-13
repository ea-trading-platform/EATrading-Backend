package com.eatrading.api.dto;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for watchlist items with current market data
 */
public class WatchlistItemResponse {

    private String symbol;
    private String name;

    @JsonProperty("currentPrice")
    private Double currentPrice;

    @JsonProperty("priceChange")
    private Double priceChange;

    @JsonProperty("percentChange")
    private Double percentChange;

    private Instant timestamp;

    // Constructors
    public WatchlistItemResponse() {
    }

    public WatchlistItemResponse(String symbol, String name, Double currentPrice,
            Double priceChange, Double percentChange, Instant timestamp) {
        this.symbol = symbol;
        this.name = name;
        this.currentPrice = currentPrice;
        this.priceChange = priceChange;
        this.percentChange = percentChange;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Double getPriceChange() {
        return priceChange;
    }

    public void setPriceChange(Double priceChange) {
        this.priceChange = priceChange;
    }

    public Double getPercentChange() {
        return percentChange;
    }

    public void setPercentChange(Double percentChange) {
        this.percentChange = percentChange;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
