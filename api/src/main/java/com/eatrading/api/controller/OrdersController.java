package com.eatrading.api.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eatrading.api.dto.OrderTransactionRequest;
import com.eatrading.api.entities.Order;
import com.eatrading.api.messaging.Producers;
import com.eatrading.api.services.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrdersController {

    private final OrderService orderService;
    private final Producers producers;

    public OrdersController(OrderService orderService, Producers producers) {
        this.orderService = orderService;
        this.producers = producers;
    }

    /**
     * GET /api/orders?clientId={clientId}
     * Get orders for a specific client
     */
    @GetMapping
    public ResponseEntity<List<Order>> getOrdersByClient(@RequestParam String clientId) {
        UUID clientUuid = UUID.fromString(clientId);
        List<Order> orders = orderService.getOrdersByClient(clientUuid);
        return ResponseEntity.ok(orders);
    }

    /**
     * POST /api/orders/
     * Create a transaction order (BUY or SELL)
     */
    @PostMapping
    public ResponseEntity<UUID> createTransactionOrder(
            @Valid @RequestBody OrderTransactionRequest request) {
        
        producers.addToIncomingQueue(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(request.getTrackingId());
    }

    /**
     * 
     * PATCH /api/orders/{trackingId}
     * Change status of existing order to canceled
     * 
     * @return
     */
    @PatchMapping("/{trackingId}")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable UUID trackingId) {
        Map<String, Object> response = orderService.cancelOrder(trackingId);
        return ResponseEntity.ok(response);
    }

}
