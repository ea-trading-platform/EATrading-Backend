package com.eatrading.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class QuoteResponse {
    private Quote data;
    private QuoteMetadata meta;

    // Constructors
    public QuoteResponse() {}

    public QuoteResponse(Quote data, QuoteMetadata meta) {
        this.data = data;
        this.meta = meta;
    }

    // Getters and Setters
    public Quote getData() {
        return data;
    }

    public void setData(Quote data) {
        this.data = data;
    }

    public QuoteMetadata getMeta() {
        return meta;
    }

    public void setMeta(QuoteMetadata meta) {
        this.meta = meta;
    }

    // Inner class for metadata
    public static class QuoteMetadata {
        @JsonProperty("asOf")
        private String asOf;
        
        private String disclaimer;
        private String symbol;
        private String source;
        private Boolean stale;
        
        @JsonProperty("spreadSource")
        private String spreadSource;

        // Constructors
        public QuoteMetadata() { 
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

        public String getSpreadSource() {
            return spreadSource;
        }

        public void setSpreadSource(String spreadSource) {
            this.spreadSource = spreadSource;
        }
    }
}
