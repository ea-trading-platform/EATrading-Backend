package com.eatrading.api.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Status;

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

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    @Column(name="tracking_id", unique=true)
    private UUID trackingId;

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

    @Column(name="current_status", nullable = false)
    private Status currentStatus;
    
    private Asset asset;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "order_status_log", joinColumns = @JoinColumn(name = "order_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "status")
    @Column(name = "status_change_time")
    private Map<Status, Instant> statusChangeLog;

    public Order() {
        // Default constructor for JPA
        this.currentStatus = Status.SUBMITTED;
        this.statusChangeLog = new HashMap<>();
    }

    public Order(UUID trackingId, UUID clientId, Asset asset, BigDecimal quantity,
     boolean buy, BigDecimal price) {
        this.trackingId = trackingId;
        this.clientId = clientId;
        this.asset = asset;
        this.quantity = quantity;
        this.buy = buy;
        this.orderDate = Instant.now();
        this.price = price;
        this.currentStatus = Status.SUBMITTED;
        this.statusChangeLog = new HashMap<>();
        statusChangeLog.put(this.currentStatus, Instant.now());
    }

    public UUID getOrderId() {
        return this.orderId;
    }

    public UUID getTrackingId() {
        return this.trackingId;
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
        if (this.statusChangeLog == null) {
            this.statusChangeLog = new HashMap<>();
        }
        this.statusChangeLog.put(status, Instant.now());
    }
}
