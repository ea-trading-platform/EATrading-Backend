package com.eatrading.api.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eatrading.api.config.JwtAuthenticationFilter;
import com.eatrading.api.services.AuthService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/holdings")
public class HoldingsController {

    private static final Logger logger = LoggerFactory.getLogger(HoldingsController.class);

    private final AuthService authService;

    public HoldingsController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * GET /api/holdings?clientId={clientId}
     * Get holdings for a specific client
     * 
     * BR-02: Zero Trust - Verify authenticated client owns the requested data
     */
    @GetMapping
    public ResponseEntity<List<HoldingResponse>> getClientHoldings(
            @RequestParam String clientId,
            HttpServletRequest request) {

        // Get authenticated client ID from JWT filter
        UUID authenticatedClientId = JwtAuthenticationFilter.getAuthenticatedClientId(request);
        UUID requestedClientId = UUID.fromString(clientId);

        // BR-02: Verify client can access their own holdings
        if (!authService.canAccessResource(authenticatedClientId, requestedClientId)) {
            logger.warn("Unauthorized access attempt: client {} tried to access holdings for {}",
                    authenticatedClientId, requestedClientId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<HoldingResponse> clientHoldings = new ArrayList<>();

        // TODO: Access holdings from the specific client's portfolio
        // This requires adding a getPortfolio() method to Client class
        // or a public method to retrieve holdings

        logger.info("Retrieved holdings for client: {}", requestedClientId);
        return ResponseEntity.ok(clientHoldings);
    }

    // Response DTO
    public static class HoldingResponse {
        private String clientId;
        private String symbol;
        private String quantity;
        private String avgBuyPrice;
        private String currentValue;

        public HoldingResponse() {
        }

        public HoldingResponse(String clientId, String symbol, String quantity, String avgBuyPrice,
                String currentValue) {
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
