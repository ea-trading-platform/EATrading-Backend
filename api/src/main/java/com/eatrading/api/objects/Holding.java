package com.eatrading.api.objects;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Embeddable
public class Holding {
    @Transient
    private Asset asset;
    
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;
    
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal avgBuyPrice;

    public Holding() {
        // Default constructor for JPA
    }
    
    public Holding(Asset asset, BigDecimal quantity) {
        this.asset = asset;
        this.quantity = quantity;
        if (asset != null) {
            this.avgBuyPrice = asset.getCurrMarketPrice().divide(quantity);
        }
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
    
    public void setAsset(Asset asset) {
        this.asset = asset;
    }

    public BigDecimal getCurrentValue() {
        return this.quantity.multiply(this.asset.getCurrMarketPrice());
    }

    public BigDecimal getPurchasedValue() {
        return this.quantity.multiply(this.avgBuyPrice);
    }

}
