package com.eatrading.api.dto;

public class HoldingResponse {
    private String clientId;
    private String symbol;
    private String quantity;
    private String avgBuyPrice;
    private String name;
    private String instrument;
    private String currentPrice;
    private String currentValue;
    private String dailyChange;
    private String dailyChangePercent;
    private String unrealizedReturn;
    private String unrealizedReturnPercent;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public String getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(String currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }

    public String getDailyChange() {
        return dailyChange;
    }

    public void setDailyChange(String dailyChange) {
        this.dailyChange = dailyChange;
    }

    public String getDailyChangePercent() {
        return dailyChangePercent;
    }

    public void setDailyChangePercent(String dailyChangePercent) {
        this.dailyChangePercent = dailyChangePercent;
    }

    public String getUnrealizedReturn() {
        return unrealizedReturn;
    }

    public void setUnrealizedReturn(String unrealizedReturn) {
        this.unrealizedReturn = unrealizedReturn;
    }

    public String getUnrealizedReturnPercent() {
        return unrealizedReturnPercent;
    }

    public void setUnrealizedReturnPercent(String unrealizedReturnPercent) {
        this.unrealizedReturnPercent = unrealizedReturnPercent;
    }
}