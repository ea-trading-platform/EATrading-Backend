package com.eatrading.api.dto;

import java.math.BigDecimal;
import java.util.Map;

public class OrderAnalyticsResponse {
    private long totalOrders;
    private BigDecimal totalQuantity;
    private long buyOrders;
    private long sellOrders;
    private long filledOrders;
    private long pendingOrders;
    private long canceledOrders;
    private long rejectedOrders;
    private Map<String, Long> ordersByInstrument;
    private Map<String, Long> ordersBySide;
    private Map<String, Long> ordersByStatus;

    public OrderAnalyticsResponse() {
    }

    public OrderAnalyticsResponse(long totalOrders, BigDecimal totalQuantity, long buyOrders,
            long sellOrders, long filledOrders, long pendingOrders, long canceledOrders,
            long rejectedOrders, Map<String, Long> ordersByInstrument,
            Map<String, Long> ordersBySide, Map<String, Long> ordersByStatus) {
        this.totalOrders = totalOrders;
        this.totalQuantity = totalQuantity;
        this.buyOrders = buyOrders;
        this.sellOrders = sellOrders;
        this.filledOrders = filledOrders;
        this.pendingOrders = pendingOrders;
        this.canceledOrders = canceledOrders;
        this.rejectedOrders = rejectedOrders;
        this.ordersByInstrument = ordersByInstrument;
        this.ordersBySide = ordersBySide;
        this.ordersByStatus = ordersByStatus;
    }

    public long getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(long totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public long getBuyOrders() {
        return buyOrders;
    }

    public void setBuyOrders(long buyOrders) {
        this.buyOrders = buyOrders;
    }

    public long getSellOrders() {
        return sellOrders;
    }

    public void setSellOrders(long sellOrders) {
        this.sellOrders = sellOrders;
    }

    public long getFilledOrders() {
        return filledOrders;
    }

    public void setFilledOrders(long filledOrders) {
        this.filledOrders = filledOrders;
    }

    public long getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(long pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public long getCanceledOrders() {
        return canceledOrders;
    }

    public void setCanceledOrders(long canceledOrders) {
        this.canceledOrders = canceledOrders;
    }

    public long getRejectedOrders() {
        return rejectedOrders;
    }

    public void setRejectedOrders(long rejectedOrders) {
        this.rejectedOrders = rejectedOrders;
    }

    public Map<String, Long> getOrdersByInstrument() {
        return ordersByInstrument;
    }

    public void setOrdersByInstrument(Map<String, Long> ordersByInstrument) {
        this.ordersByInstrument = ordersByInstrument;
    }

    public Map<String, Long> getOrdersBySide() {
        return ordersBySide;
    }

    public void setOrdersBySide(Map<String, Long> ordersBySide) {
        this.ordersBySide = ordersBySide;
    }

    public Map<String, Long> getOrdersByStatus() {
        return ordersByStatus;
    }

    public void setOrdersByStatus(Map<String, Long> ordersByStatus) {
        this.ordersByStatus = ordersByStatus;
    }

}
