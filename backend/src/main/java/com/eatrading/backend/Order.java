package com.eatrading.backend;

import java.util.*;
import java.time.Instant;
import java.math.BigDecimal;

public class Order {
    // @Id
    // @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    private final Client client;
    private final boolean isBuy;
    private final Instant orderDate;
    private final BigDecimal price;
    private final BigDecimal quantity;
    private final Asset asset;
    private Status currentStatus;
    private final Map<Status, Instant> statusChangeLog;

    public Order(Client client, Asset asset, BigDecimal quantity,
     boolean isBuy) {
        this.client = client;
        this.asset = asset;
        this.quantity = quantity;
        this.isBuy = isBuy;
        this.orderDate = Instant.now();
        this.price = this.asset.getCurrMarketPrice();
        this.statusChangeLog = new HashMap<Status, Instant>();
        statusChangeLog.put(Status.SUBMITTED, Instant.now());
        // add to database
    }

    public UUID getOrderID() {
        return this.orderId;
    }

    public Client getClient() {
        return this.client;
    }

    public boolean isBuy() {
        return this.isBuy;
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
