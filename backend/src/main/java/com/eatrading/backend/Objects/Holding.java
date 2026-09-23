package com.eatrading.backend.Objects;

import java.math.BigDecimal;

public class Holding {
    private final Asset asset;
    private BigDecimal quantity;
    private BigDecimal avgBuyPrice;

    public Holding(Asset asset, BigDecimal quantity) {
        this.asset = asset;
        this.quantity = quantity;
        this.avgBuyPrice = this.asset.getCurrMarketPrice().divide(this.quantity);
    }

    public BigDecimal getQuantity() {
        return this.quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAvgBuyPrice() {
        return this.avgBuyPrice;
    }

    public void setAvgBuyPrice(BigDecimal avgBuyPrice) {
        this.avgBuyPrice = avgBuyPrice;
    }

    public Asset getAsset(){
        return this.asset;
    }

    public BigDecimal getCurrentValue() {
        return this.quantity.multiply(this.asset.getCurrMarketPrice());
    }

    public BigDecimal getPurchasedValue() {
        return this.quantity.multiply(this.avgBuyPrice);
    }

}
