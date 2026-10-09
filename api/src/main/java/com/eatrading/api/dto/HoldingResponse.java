package com.eatrading.api.dto;

public class HoldingResponse {
    private String clientId;
    private String symbol;
    private String quantity;
    private String avgBuyPrice;
    private String currentValue;

    public HoldingResponse() {
    }

    public HoldingResponse(String clientId, String symbol, String quantity, String avgBuyPrice) {
        this.clientId = clientId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.avgBuyPrice = avgBuyPrice;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getAvgBuyPrice() {
        return avgBuyPrice;
    }

    public void setAvgBuyPrice(String avgBuyPrice) {
        this.avgBuyPrice = avgBuyPrice;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }
}