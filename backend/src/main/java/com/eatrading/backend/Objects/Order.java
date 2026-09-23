package com.eatrading.backend.Objects;

import java.util.*;
import java.time.Instant;
import java.math.BigDecimal;

public class Order {
    // @Id
    // @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    private final UUID clientId;
    private final boolean buy;
    private final Instant orderDate;
    private final BigDecimal price;
    private final BigDecimal quantity;
    private final Asset asset;
    private Status currentStatus; // turn into method
    private final Map<Status, Instant> statusChangeLog;

    public Order(UUID clientId, Asset asset, BigDecimal quantity,
     boolean buy) {
        this.clientId = clientId;
        this.asset = asset;
        this.quantity = quantity;
        this.buy = buy;
        this.orderDate = Instant.now();
        this.price = this.asset.getCurrMarketPrice();
        this.statusChangeLog = new HashMap<Status, Instant>();
        statusChangeLog.put(Status.SUBMITTED, Instant.now());
        // add to database
    }

    public UUID getOrderID() {
        return this.orderId;
    }

    public UUID getClientId() {
        return this.clientId;
    }

    public boolean isBuy() {
        return this.buy;
    }

    public Instant getOrderDate() {
        return this.orderDate;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    public BigDecimal getQuantity() {
        return this.quantity;
    }
    
    public Asset getAsset() {
        return this.asset;
    }

    public Status getCurrentStatus() {
        return this.currentStatus;
    }
    
    public void setStatus(Status status) {
        this.currentStatus = status;
        this.statusChangeLog.put(status, Instant.now());
    }
}
