package com.eatrading.api.controller;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eatrading.api.config.JwtAuthenticationFilter;
import com.eatrading.api.dto.OrderTransactionRequest;
import com.eatrading.api.entities.Order;
import com.eatrading.api.messaging.Producers;
import com.eatrading.api.repository.OrderRepository;
import com.eatrading.api.services.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrdersController {

    private static final Logger logger = LoggerFactory.getLogger(OrdersController.class);

    private final OrderRepository orderRepository;
    private final Producers producers;
    private final AuthService authService;

    public OrdersController(OrderRepository orderRepository, Producers producers, AuthService authService) {
        this.orderRepository = orderRepository;
        this.producers = producers;
        this.authService = authService;
    }

    /**
     * GET /api/orders?clientId={clientId}
     * Get orders for a specific client
     * 
     * BR-02: Zero Trust - Verify authenticated client owns the requested data
     */
    @GetMapping
    public ResponseEntity<List<Order>> getOrdersByClient(
            @RequestParam String clientId,
            HttpServletRequest request) {

        // Get authenticated client ID from JWT filter
        UUID authenticatedClientId = JwtAuthenticationFilter.getAuthenticatedClientId(request);
        UUID requestedClientId = UUID.fromString(clientId);

        // BR-02: Verify client can access their own orders
        if (!authService.canAccessResource(authenticatedClientId, requestedClientId)) {
            logger.warn("Unauthorized access attempt: client {} tried to access orders for client {}",
                    authenticatedClientId, requestedClientId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Order> orders = orderRepository.findByClientId(requestedClientId);
        logger.info("Retrieved {} orders for client: {}", orders.size(), requestedClientId);
        return ResponseEntity.ok(orders);
    }

    /**
     * POST /api/orders/transact
     * Create a transaction order (BUY or SELL)
     * 
     * BR-02: Zero Trust - Use authenticated client ID from token
     */
    @PostMapping
    public ResponseEntity<UUID> createTransactionOrder(
            @Valid @RequestBody OrderTransactionRequest request,
            HttpServletRequest httpRequest) {

        // Get authenticated client ID from JWT filter
        UUID authenticatedClientId = JwtAuthenticationFilter.getAuthenticatedClientId(httpRequest);

        // Ensure order belongs to authenticated client
        request.setClientId(authenticatedClientId.toString());

        logger.info("Creating order for client: {}", authenticatedClientId);
        producers.addToIncomingQueue(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(request.getTrackingId());
    }
}
