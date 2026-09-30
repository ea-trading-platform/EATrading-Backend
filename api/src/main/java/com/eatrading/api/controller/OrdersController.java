package com.eatrading.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.eatrading.api.messaging.Producers;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Instrument;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class OrdersController {

    private final OrderRepository orderRepository;
    private final Producers producers;

    public OrdersController(OrderRepository orderRepository, Producers producers) {
        this.orderRepository = orderRepository;
        this.producers = producers;
    }

    /**
     * GET /api/orders?clientId={clientId}
     * Get orders for a specific client
     */
    @GetMapping("/orders")
    public ResponseEntity<List<Order>> getOrdersByClient(@RequestParam String clientId) {
        UUID clientUuid = UUID.fromString(clientId);
        List<Order> orders = orderRepository.findByClientId(clientUuid);
        return ResponseEntity.ok(orders);
    }

    /**
     * POST /api/orders/transact
     * Create a transaction order (BUY or SELL)
     */
    @PostMapping("/orders")
    public ResponseEntity<UUID> createTransactionOrder(
            @Valid @RequestBody OrderTransactionRequest request) {
        
        producers.addToIncomingQueue(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(request.getTrackingId());
    }
}
