package com.eatrading.api.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.format.annotation.DateTimeFormat;

import com.eatrading.api.config.JwtAuthenticationFilter;
import com.eatrading.api.dto.OrderAnalyticsResponse;
import com.eatrading.api.dto.OrderTransactionRequest;
import com.eatrading.api.dto.OrderTransactionResponse;
import com.eatrading.api.entities.Order;
import com.eatrading.api.messaging.Producers;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.objects.Role;
import com.eatrading.api.objects.Status;
import com.eatrading.api.services.AuthService;
import com.eatrading.api.services.ClientService;
import com.eatrading.api.services.OrderService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrdersController {

    private final OrderService orderService;
    private final Producers producers;
    private final AuthService authService;
    private final ClientService clientService;

    private static final Logger logger = LoggerFactory.getLogger(OrdersController.class);


    public OrdersController(OrderService orderService, Producers producers, AuthService authService,
            ClientService clientService) {
        this.orderService = orderService;
        this.producers = producers;
        this.authService = authService;
        this.clientService = clientService;
    }

    /**
     * GET /api/orders
     * Get all orders. Administrators only.
     */
    @GetMapping
    public ResponseEntity<List<OrderTransactionResponse>> getAllOrders(HttpServletRequest request) {
        if (JwtAuthenticationFilter.getAuthenticatedRole(request) != Role.ADMIN) {
            logger.warn("Unauthorized all-orders query attempt");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(toResponses(orderService.getAllOrders()));
    }

    /**
     * GET /api/orders?clientId={clientId}
     * Get orders for a specific client
     * 
     * BR-02: Zero Trust - Verify authenticated client owns the requested data.
     * Administrators may access any client's orders.
     */
    @GetMapping(params = "clientId")
    public ResponseEntity<List<OrderTransactionResponse>> getOrdersByClient(
            @RequestParam String clientId,
            HttpServletRequest request) {
        UUID authenticatedClientId = JwtAuthenticationFilter.getAuthenticatedClientId(request);
        UUID requestedClientId = UUID.fromString(clientId);

        boolean isAdmin = JwtAuthenticationFilter.getAuthenticatedRole(request) == Role.ADMIN;
        if (!isAdmin && !authService.canAccessResource(authenticatedClientId, requestedClientId)) {
            logger.warn("Unauthorized access attempt: client {} tried to access orders for {}",
                    authenticatedClientId, requestedClientId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Order> orders = orderService.getOrdersByClient(requestedClientId);
        return ResponseEntity.ok(toResponses(orders));
    }

    /**
     * GET /api/orders?clientEmail={clientEmail}
     * Get all orders for a client identified by email. Administrators only.
     */
    @GetMapping(params = "clientEmail")
    public ResponseEntity<List<OrderTransactionResponse>> getOrdersByClientEmail(
            @RequestParam String clientEmail,
            HttpServletRequest request) {

        if (JwtAuthenticationFilter.getAuthenticatedRole(request) != Role.ADMIN) {
            logger.warn("Unauthorized client email order query attempt");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return clientService.getClientByEmail(clientEmail.trim())
                .map(client -> ResponseEntity.ok(toResponses(orderService.getOrdersByClient(client.getId()))))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * GET /api/orders?instrument={instrument}
     * Get all orders for an instrument. Administrators only.
     */
    @GetMapping(params = "instrument")
    public ResponseEntity<List<OrderTransactionResponse>> getOrdersByInstrument(
            @RequestParam Instrument instrument,
            HttpServletRequest request) {

        if (JwtAuthenticationFilter.getAuthenticatedRole(request) != Role.ADMIN) {
            logger.warn("Unauthorized instrument order query attempt");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(toResponses(orderService.getOrdersByInstrument(instrument)));
    }

    /**
     * GET /api/orders?side={side}
     * Get all BUY or SELL orders. Administrators only.
     */
    @GetMapping(params = "side")
    public ResponseEntity<List<OrderTransactionResponse>> getOrdersBySide(
            @RequestParam String side,
            HttpServletRequest request) {

        if (JwtAuthenticationFilter.getAuthenticatedRole(request) != Role.ADMIN) {
            logger.warn("Unauthorized side order query attempt");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean buy;
        if ("BUY".equalsIgnoreCase(side)) {
            buy = true;
        } else if ("SELL".equalsIgnoreCase(side)) {
            buy = false;
        } else {
            logger.warn("Invalid order side requested: {}", side);
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(toResponses(orderService.getOrdersBySide(buy)));
    }

    /**
     * GET /api/orders?status={status}
     * Get all orders with a status. Administrators only.
     */
    @GetMapping(params = "status")
    public ResponseEntity<List<OrderTransactionResponse>> getOrdersByStatus(
            @RequestParam String status,
            HttpServletRequest request) {

        if (JwtAuthenticationFilter.getAuthenticatedRole(request) != Role.ADMIN) {
            logger.warn("Unauthorized status order query attempt");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        final Status orderStatus;
        try {
            orderStatus = Status.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException exception) {
            logger.warn("Invalid order status requested: {}", status);
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(toResponses(orderService.getOrdersByStatus(orderStatus)));
    }

    /**
     * GET /api/orders?from={from}&to={to}
     * Get orders in a half-open date range. Administrators only.
     */
    @GetMapping(params = { "from", "to" })
    public ResponseEntity<List<OrderTransactionResponse>> getOrdersByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            HttpServletRequest request) {

        if (JwtAuthenticationFilter.getAuthenticatedRole(request) != Role.ADMIN) {
            logger.warn("Unauthorized order date-range query attempt");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!from.isBefore(to)) {
            logger.warn("Invalid order date range: from {} is not before to {}", from, to);
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(toResponses(orderService.getOrdersByDateRange(from, to)));
    }

    /**
     * GET /api/orders/analytics
     * Get aggregate order analytics with optional composable filters. Administrators only.
     */
    @GetMapping("/analytics")
    public ResponseEntity<OrderAnalyticsResponse> getOrderAnalytics(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Instrument instrument,
            @RequestParam(required = false) String side,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
                
        if (JwtAuthenticationFilter.getAuthenticatedRole(request) != Role.ADMIN) {
            logger.warn("Unauthorized order analytics query attempt");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (from != null && to != null && !from.isBefore(to)) {
            logger.warn("Invalid order analytics date range: from {} is not before to {}", from, to);
            return ResponseEntity.badRequest().build();
        }

        Boolean buy = null;
        if (side != null) {
            if ("BUY".equalsIgnoreCase(side)) {
                buy = true;
            } else if ("SELL".equalsIgnoreCase(side)) {
                buy = false;
            } else {
                logger.warn("Invalid order analytics side requested: {}", side);
                return ResponseEntity.badRequest().build();
            }
        }

        Status orderStatus = null;
        if (status != null) {
            try {
                orderStatus = Status.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException | NullPointerException exception) {
                logger.warn("Invalid order analytics status requested: {}", status);
                return ResponseEntity.badRequest().build();
            }
        }

        return ResponseEntity.ok(
                orderService.getOrderAnalytics(clientId, from, to, instrument, buy, orderStatus));
    }

    /**
     * GET /api/orders/{trackingId}/status-log
     * Get all stored status changes for an order.
     */
    @GetMapping("/{trackingId}/status-log")
    public ResponseEntity<Map<Status, java.time.Instant>> getStatusLog(
            @PathVariable UUID trackingId) {
        return ResponseEntity.ok(orderService.getStatusLogByTrackingId(trackingId));
    }

    /**
     * POST /api/orders/
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

    /**
     * 
     * PATCH /api/orders/{trackingId}
     * Change status of existing order to canceled
     * 
     * @return
     */
    @PatchMapping("/{trackingId}")
    public ResponseEntity<Map<String, Object>> cancelOrder(
            @PathVariable UUID trackingId,
            HttpServletRequest request) {
        UUID authenticatedClientId = JwtAuthenticationFilter.getAuthenticatedClientId(request);
        Map<String, Object> response = orderService.cancelOrder(trackingId, authenticatedClientId);
        return ResponseEntity.ok(response);
    }

    private List<OrderTransactionResponse> toResponses(List<Order> orders) {
        return orders.stream()
                .map(OrderTransactionResponse::toResponse)
                .toList();
    }

}
