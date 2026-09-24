package com.eatrading.backend.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/holdings")
public class HoldingsController {

    /**
     * GET /api/holdings?clientId={clientId}
     * Get holdings for a specific client - Only accessible for authenticated user's own holdings
     */
    @GetMapping
    public ResponseEntity<List<HoldingResponse>> getClientHoldings(@RequestParam String clientId) {
        // Verify authorization - user can only view their own holdings
        if (!isAuthorized(clientId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        List<HoldingResponse> clientHoldings = new ArrayList<>();

        // TODO: Access holdings from the specific client's portfolio
        // This requires adding a getPortfolio() method to Client class
        // or a public method to retrieve holdings
        
        return ResponseEntity.ok(clientHoldings);
    }
    
    /**
     * Helper method to verify user authorization
     * Returns true if the authenticated user's ID matches the requested clientId
     */
    private boolean isAuthorized(String clientId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        // Get the authenticated user's principal (assuming it's the user ID)
        String authenticatedUserId = authentication.getName();
        return authenticatedUserId.equals(clientId);
    }

    // Response DTO
    public static class HoldingResponse {
        private String clientId;
        private String symbol;
        private String quantity;
        private String avgBuyPrice;
        private String currentValue;

        public HoldingResponse() {}

        public HoldingResponse(String clientId, String symbol, String quantity, String avgBuyPrice, String currentValue) {
            this.clientId = clientId;
            this.symbol = symbol;
            this.quantity = quantity;
            this.avgBuyPrice = avgBuyPrice;
            this.currentValue = currentValue;
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

        public String getQuantity() {
            return quantity;
        }

        public void setQuantity(String quantity) {
            this.quantity = quantity;
        }

        public String getAvgBuyPrice() {
            return avgBuyPrice;
        }

        public void setAvgBuyPrice(String avgBuyPrice) {
            this.avgBuyPrice = avgBuyPrice;
        }

        public String getCurrentValue() {
            return currentValue;
        }

        public void setCurrentValue(String currentValue) {
            this.currentValue = currentValue;
        }
    }
}
