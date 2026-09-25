package com.eatrading.api.Objects;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;
    
    @Column(nullable = false)
    private boolean buy;
    
    @Column(nullable = false)
    private Instant orderDate;
    
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal price;
    
    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;
    
    private Asset asset;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "order_status_log", joinColumns = @JoinColumn(name = "order_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "status")
    @Column(name = "status_change_time")
    private Map<Status, Instant> statusChangeLog;

    public Order() {
        // Default constructor for JPA
        this.statusChangeLog = new HashMap<>();
    }
    
    public Order(UUID clientId, Asset asset, BigDecimal quantity,
     boolean buy) {
        this.clientId = clientId;
        this.asset = asset;
        this.quantity = quantity;
        this.buy = buy;
        this.orderDate = Instant.now();
        this.price = this.asset.getCurrMarketPrice();
        this.statusChangeLog = new HashMap<>();
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
        if (statusChangeLog == null || statusChangeLog.isEmpty()) {
            return Status.SUBMITTED;
        }
        return statusChangeLog.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(Status.SUBMITTED);
    }
    
    public void setStatus(Status status) {
        this.statusChangeLog.put(status, Instant.now());
    }
}
