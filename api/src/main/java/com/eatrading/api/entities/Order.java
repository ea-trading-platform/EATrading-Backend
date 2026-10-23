package com.eatrading.api.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Status;
import com.eatrading.api.objects.OrderStatusLogEntry;
import com.eatrading.api.objects.BaseMarket;

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
    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "tracking_id", unique = true)
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

    @Column(name = "current_status", nullable = false)
    private Status currentStatus;

    private Asset asset;

    @Column(name = "base_market", nullable = false)
    private BaseMarket baseMarket;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "order_status_log", joinColumns = {
            @JoinColumn(name = "order_id", referencedColumnName = "order_id"),
            @JoinColumn(name = "tracking_id", referencedColumnName = "tracking_id")
    })
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "status")
    private Map<Status, OrderStatusLogEntry> statusChangeLog;

    public Order() {
        // Default constructor for JPA
        this.baseMarket = BaseMarket.UNCAT;
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
        this.baseMarket = BaseMarket.UNCAT;
        this.currentStatus = Status.SUBMITTED;
        this.statusChangeLog = new HashMap<>();
        statusChangeLog.put(this.currentStatus, new OrderStatusLogEntry(this.orderDate, null));
    }

    public UUID getOrderId() {
        return this.orderId;
    }

    public UUID getTrackingId() {
        return this.trackingId;
    }

    @JsonIgnore
    public Map<Status, Instant> getStatusChangeLog() {
        Map<Status, Instant> timestamps = new HashMap<>();
        this.statusChangeLog.forEach((status, entry) -> timestamps.put(status, entry.getStatusChangeTime()));
        return timestamps;
    }

    @JsonIgnore
    public Map<Status, OrderStatusLogEntry> getStatusLogEntries() {
        return this.statusChangeLog;
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

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getQuantity() {
        return this.quantity;
    }

    public Asset getAsset() {
        return this.asset;
    }

    public BaseMarket getBaseMarket() {
        return this.baseMarket;
    }

    public void setBaseMarket(BaseMarket baseMarket) {
        this.baseMarket = baseMarket;
    }

    public Status getCurrentStatus() {
        return this.currentStatus;
    }

    public void setStatus(Status status) {
        setStatus(status, null);
    }

    public void setStatus(Status status, String rejectionReason) {
        this.currentStatus = status;
        if (this.statusChangeLog == null) {
            this.statusChangeLog = new HashMap<>();
        }
        if (this.orderDate != null) {
            this.statusChangeLog.putIfAbsent(Status.SUBMITTED, new OrderStatusLogEntry(this.orderDate, null));
        }
        if (status == Status.SUBMITTED) {
            this.statusChangeLog.putIfAbsent(status,
                    new OrderStatusLogEntry(this.orderDate != null ? this.orderDate : Instant.now(), null));
        } else {
            this.statusChangeLog.put(status,
                    new OrderStatusLogEntry(Instant.now(), status == Status.REJECTED ? rejectionReason : null));
        }
    }
}
