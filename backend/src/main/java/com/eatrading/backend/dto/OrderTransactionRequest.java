package com.eatrading.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for creating a transaction order (BUY or SELL)
 */
public class OrderTransactionRequest {
    
    @NotBlank(message = "Client ID is required")
    @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", 
             message = "Client ID must be a valid UUID")
    private String clientId;
    
    @NotBlank(message = "Symbol is required")
    private String symbol;
    
    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "^(BUY|SELL)$", message = "Transaction type must be either BUY or SELL")
    private String transactionType;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    public OrderTransactionRequest() {}

    public OrderTransactionRequest(String clientId, String symbol, String transactionType, BigDecimal quantity) {
        this.clientId = clientId;
        this.symbol = symbol;
        this.transactionType = transactionType;
        this.quantity = quantity;
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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
