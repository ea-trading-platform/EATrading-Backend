package com.eatrading.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eatrading.api.repository.OrderRepository;
import com.eatrading.api.dto.OrderTransactionRequest;
import com.eatrading.api.dto.OrderTransactionResponse;
import com.eatrading.api.entities.Order;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrdersController {

    private final OrderRepository orderRepository;

    public OrdersController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * GET /api/orders?clientId={clientId}
     * Get orders for a specific client - Only accessible for authenticated user's own orders
     */
    @GetMapping
    public ResponseEntity<List<Order>> getOrdersByClient(@RequestParam String clientId) {
        // Verify authorization - user can only view their own orders
        if (!isAuthorized(clientId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        UUID clientUuid = UUID.fromString(clientId);
        List<Order> orders = orderRepository.findByClientId(clientUuid);
        return ResponseEntity.ok(orders);
    }

    /**
     * POST /api/orders/transact
     * Create a transaction order (BUY or SELL) - Authenticated user can only create orders for themselves
     */
    @PostMapping("/transact")
    public ResponseEntity<OrderTransactionResponse> createTransactionOrder(
            @Valid @RequestBody OrderTransactionRequest request) {
        
        try {
            String clientIdString = request.getClientId();
            
            // Verify authorization - user can only create orders for themselves
            if (!isAuthorized(clientIdString)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            UUID clientId = UUID.fromString(clientIdString);
            String transactionType = request.getTransactionType();

            // Create Order object
            // Note: OrderProcessor.process() expects OrderRequest with an Order object
            // Adjust Order creation based on how your Order class is constructed
            // Order order = new Order(clientId, asset, quantity, isBuy);
            // OrderRequest orderRequest = new OrderRequest(order);
            // OrderResponse orderResponse = orderProcessor.process(orderRequest);

            // For now, creating a placeholder response
            // TODO: Integrate with actual OrderProcessor when Order/Asset retrieval is implemented
            OrderTransactionResponse response = new OrderTransactionResponse(
                UUID.randomUUID().toString(),
                clientId.toString(),
                request.getSymbol(),
                transactionType,
                request.getQuantity().toString(),
                "SUBMITTED",
                java.time.Instant.now().toString()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
}
