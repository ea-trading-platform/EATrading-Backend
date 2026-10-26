package com.eatrading.api.dto;

import com.eatrading.api.entities.Order;
import com.eatrading.api.objects.OrderStatusLogEntry;
import com.eatrading.api.objects.Status;

/**
 * Response DTO for transaction order
 */
public class OrderTransactionResponse {

    private String orderId;
    private String trackingId;
    private String clientId;
    private String symbol;
    private String instrument;
    private String transactionType;
    private String quantity;
    private String price;
    private String status;
    private String orderDate;
    private String rejectionReason;

    public OrderTransactionResponse() {
    }

    public static OrderTransactionResponse toResponse(Order order) {
        OrderTransactionResponse response = new OrderTransactionResponse(
                order.getOrderId().toString(),
                order.getTrackingId().toString(),
                order.getClientId().toString(),
                order.getAsset().getSymbol(),
                order.getAsset().getInstrument().toString(),
                order.isBuy() ? "BUY" : "SELL",
                order.getQuantity().toString(),
                order.getPrice().toString(),
                order.getCurrentStatus().toString(),
                order.getOrderDate().toString());
        if (order.getCurrentStatus() == Status.REJECTED) {
            OrderStatusLogEntry rejectedEntry = order.getStatusLogEntries().get(Status.REJECTED);
            response.setRejectionReason(rejectedEntry == null ? null : rejectedEntry.getRejectionReason());
        }
        return response;
    }

    public OrderTransactionResponse(String orderId, String trackingId, String clientId, String symbol,
            String instrument,
            String transactionType, String quantity, String price, String status, String orderDate) {
        this.orderId = orderId;
        this.trackingId = trackingId;
        this.clientId = clientId;
        this.symbol = symbol;
        this.instrument = instrument;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
        this.orderDate = orderDate;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(String trackingId) {
        this.trackingId = trackingId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(String orderDate) {
        this.orderDate = orderDate;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
