package com.eatrading.backend.dto;

/**
 * Response DTO for transaction order
 */
public class OrderTransactionResponse {
    
    private String orderId;
    private String clientId;
    private String symbol;
    private String transactionType;
    private String quantity;
    private String status;
    private String orderDate;

    public OrderTransactionResponse() {}

    public OrderTransactionResponse(String orderId, String clientId, String symbol,
            String transactionType, String quantity, String status, String orderDate) {
        this.orderId = orderId;
        this.clientId = clientId;
        this.symbol = symbol;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.status = status;
        this.orderDate = orderDate;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
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
}
