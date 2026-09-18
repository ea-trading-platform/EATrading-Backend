package com.eatrading.backend;

import java.math.BigDecimal;

public class Holding {
    private BigDecimal quantity;
    private final Asset asset;

    public Holding(Asset asset, BigDecimal quantity) {
        this.asset = asset;
        this.quantity = quantity;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public Asset getAsset(){
        return asset;
    }
}
