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
@RequestMapping("/api/orders")
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
    @GetMapping
    public ResponseEntity<List<Order>> getOrdersByClient(@RequestParam String clientId) {
        UUID clientUuid = UUID.fromString(clientId);
        List<Order> orders = orderRepository.findByClientId(clientUuid);
        return ResponseEntity.ok(orders);
    }

    /**
     * POST /api/orders/transact
     * Create a transaction order (BUY or SELL)
     */
    @PostMapping("/transact")
    public ResponseEntity<OrderTransactionResponse> createTransactionOrder(
            @Valid @RequestBody OrderTransactionRequest request) {
        
        try {
            String clientIdString = request.getClientId();
            UUID clientId = UUID.fromString(clientIdString);
            String transactionType = request.getTransactionType();
            boolean isBuy = "BUY".equalsIgnoreCase(transactionType);
            Asset asset = new Asset(request.getSymbol().toUpperCase(), request.getSymbol().toUpperCase(), Instrument.EQUITY);

            Order order = new Order(clientId, asset, request.getQuantity(), isBuy);
            Order savedOrder = orderRepository.save(order);
            producers.validate(savedOrder.getOrderID());

            OrderTransactionResponse response = new OrderTransactionResponse(
                savedOrder.getOrderID().toString(),
                clientId.toString(),
                asset.getSymbol(),
                transactionType,
                request.getQuantity().toString(),
                "SUBMITTED",
                savedOrder.getOrderDate().toString()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
