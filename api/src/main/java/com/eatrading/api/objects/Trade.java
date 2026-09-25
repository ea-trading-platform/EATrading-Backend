package com.eatrading.api.objects;

import java.math.BigDecimal;
import java.util.UUID;

public class Trade {
    // private UUID tradeId; for db
    private final UUID orderId;
    private final Holding purchasedHolding;
    private final Holding cashHolding;

    public Trade(UUID orderId, Holding purchasedHolding, 
        Holding cashHolding) {
        this.orderId = orderId;
        this.purchasedHolding = purchasedHolding;
        this.cashHolding = cashHolding;
        // this.executionPrice = executionPrice;
    }

    public UUID getOrderId() {
        return this.orderId;
    }

    public Holding getPurchasedHolding() {
        return this.purchasedHolding;
    }

    public Holding getCashHolding() {
        return this.cashHolding;
    }

    public BigDecimal getExecutionPrice() {
        return this.cashHolding.getPurchasedValue();
    }
}
