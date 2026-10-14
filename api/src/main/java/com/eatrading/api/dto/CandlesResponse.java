package com.eatrading.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CandlesResponse {
    private CandleData data;
    private CandleMetadata meta;

    // Constructors
    public CandlesResponse() {}

    public CandlesResponse(CandleData data, CandleMetadata meta) {
        this.data = data;
        this.meta = meta;
    }

    // Getters and Setters
    public CandleData getData() {
        return data;
    }

    public void setData(CandleData data) {
        this.data = data;
    }

    public CandleMetadata getMeta() {
        return meta;
    }

    public void setMeta(CandleMetadata meta) {
        this.meta = meta;
    }

    // Inner class for candle data
    public static class CandleData {
        private String symbol;
        private String interval;
        private String currency;
        private List<Candle> candles;

        // Constructors
        public CandleData() {}

        public CandleData(String symbol, String interval, String currency, List<Candle> candles) {
            this.symbol = symbol;
            this.interval = interval;
            this.currency = currency;
            this.candles = candles;
        }

        // Getters and Setters
        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getInterval() {
            return interval;
        }

        public void setInterval(String interval) {
            this.interval = interval;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public List<Candle> getCandles() {
            return candles;
        }

        public void setCandles(List<Candle> candles) {
            this.candles = candles;
        }
    }

    // Inner class for metadata
    public static class CandleMetadata {
        @JsonProperty("asOf")
        private String asOf;
        
        private String disclaimer;
        private String symbol;
        private String source;
        private Boolean stale;
        private Boolean partial;
        
        @JsonProperty("availableFrom")
        private String availableFrom;

        // Constructors
        public CandleMetadata() { 
            // Required by Jackson for JSON deserialization. 
        }

        // Getters and Setters
        public String getAsOf() {
            return asOf;
        }

        public void setAsOf(String asOf) {
            this.asOf = asOf;
        }

        public String getDisclaimer() {
            return disclaimer;
        }

        public void setDisclaimer(String disclaimer) {
            this.disclaimer = disclaimer;
        }

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public Boolean getStale() {
            return stale;
        }

        public void setStale(Boolean stale) {
            this.stale = stale;
        }

        public Boolean getPartial() {
            return partial;
        }

        public void setPartial(Boolean partial) {
            this.partial = partial;
        }

        public String getAvailableFrom() {
            return availableFrom;
        }

        public void setAvailableFrom(String availableFrom) {
            this.availableFrom = availableFrom;
        }
    }
}
