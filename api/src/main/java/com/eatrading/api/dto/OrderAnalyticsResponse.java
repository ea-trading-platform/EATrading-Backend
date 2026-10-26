package com.eatrading.api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OrderAnalyticsResponse {
    private long totalOrders;
    private BigDecimal totalQuantity;
    private Map<String, Long> ordersByInstrument;
    private Map<String, Long> ordersBySide;
    private Map<String, Long> ordersByStatus;
    private List<TradingActivityResponse.Point> tradeCounts = List.of();

    public OrderAnalyticsResponse() {
    }

    public OrderAnalyticsResponse(long totalOrders, BigDecimal totalQuantity,
            Map<String, Long> ordersByInstrument,
            Map<String, Long> ordersBySide, Map<String, Long> ordersByStatus) {
        this.totalOrders = totalOrders;
        this.totalQuantity = totalQuantity;
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

    public List<TradingActivityResponse.Point> getTradeCounts() {
        return tradeCounts;
    }

    public void setTradeCounts(List<TradingActivityResponse.Point> tradeCounts) {
        this.tradeCounts = List.copyOf(tradeCounts);
    }

}
