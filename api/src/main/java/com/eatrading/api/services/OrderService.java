package com.eatrading.api.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eatrading.api.dto.OrderAnalyticsResponse;
import com.eatrading.api.entities.Order;
import com.eatrading.api.exception.ForbiddenOperationException;
import com.eatrading.api.exception.ResourceNotFoundException;
import com.eatrading.api.exception.UnprocessableEntityException;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.objects.Status;
import com.eatrading.api.repository.OrderRepository;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> getOrdersByClient(UUID clientId) {
        return orderRepository.findByClientId(clientId);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderByTrackingId(UUID trackingId) {
        return orderRepository.findByTrackingId(trackingId);
    }

    public List<Order> getOrdersByInstrument(Instrument instrument) {
        return orderRepository.findByAssetInstrument(instrument);
    }

    public List<Order> getOrdersBySide(boolean buy) {
        return orderRepository.findByBuy(buy);
    }

    public List<Order> getOrdersByStatus(Status status) {
        return orderRepository.findByCurrentStatus(status);
    }

    public List<Order> getOrdersByDateRange(Instant from, Instant to) {
        return orderRepository.findByOrderDateGreaterThanEqualAndOrderDateLessThan(from, to);
    }

    @Transactional(readOnly = true)
    public OrderAnalyticsResponse getOrderAnalytics(UUID clientId, Instant from, Instant to,
            Instrument instrument, Boolean buy, Status status) {
        Map<Instrument, Long> ordersByInstrument = zeroedInstrumentCounts();
        Map<Status, Long> ordersByStatus = zeroedStatusCounts();

        long buyOrders = 0;
        long sellOrders = 0;
        BigDecimal totalQuantity = BigDecimal.ZERO;

        for (Order order : orderRepository.findAll()) {
            if (!matchesFilters(order, clientId, from, to, instrument, buy, status)) {
                continue;
            }

            totalQuantity = totalQuantity.add(order.getQuantity());
            if (order.isBuy()) {
                buyOrders++;
            } else {
                sellOrders++;
            }

            Instrument orderInstrument = order.getAsset().getInstrument();
            Status orderStatus = order.getCurrentStatus();
            ordersByInstrument.compute(orderInstrument, (key, count) -> count + 1);
            ordersByStatus.compute(orderStatus, (key, count) -> count + 1);
        }

        long totalOrders = buyOrders + sellOrders;
        long pendingOrders = ordersByStatus.get(Status.SUBMITTED) + ordersByStatus.get(Status.ACCEPTED);
        return new OrderAnalyticsResponse(
                totalOrders,
                totalQuantity,
                buyOrders,
                sellOrders,
                ordersByStatus.get(Status.FILLED),
                pendingOrders,
                ordersByStatus.get(Status.CANCELED),
                ordersByStatus.get(Status.REJECTED),
                stringKeyedInstrumentCounts(ordersByInstrument),
                Map.of("BUY", buyOrders, "SELL", sellOrders),
                stringKeyedStatusCounts(ordersByStatus));
    }
    
    @Transactional(readOnly = true)
    public Map<Status, java.time.Instant> getStatusLogByTrackingId(UUID trackingId) {
        Order order = getOrderByTrackingId(trackingId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found for trackingId: " + trackingId);
        }

        return Map.copyOf(order.getStatusChangeLog());
    }

    @Transactional
    public void saveIncomingOrder(Order order) {
        orderRepository.save(order);
    }

    @Transactional
    public Map<String, Object> cancelOrder(UUID trackingId, UUID authenticatedClientId) {
        Order order = getOrderByTrackingId(trackingId);

        if (order == null) {
            throw new ResourceNotFoundException("Order not found for trackingId: " + trackingId);
        }

        if (!order.getClientId().equals(authenticatedClientId)) {
            throw new ForbiddenOperationException(
                    "Client " + authenticatedClientId + " is not allowed to cancel order " + trackingId);
        }

        Status currentStatus = order.getCurrentStatus();
        if (!Status.SUBMITTED.equals(currentStatus)) {
            throw new UnprocessableEntityException(
                    "Order with trackingId " + trackingId + " cannot be canceled because current status is "
                            + currentStatus + ".");
        }

        order.setStatus(Status.CANCELED);
        orderRepository.save(order);

        return Map.of(
                "status", "success",
                "message", "Order canceled successfully",
                "trackingId", trackingId,
                "currentStatus", Status.CANCELED.toString());
    }

    private boolean matchesFilters(Order order, UUID clientId, Instant from, Instant to,
            Instrument instrument, Boolean buy, Status status) {
        return (clientId == null || clientId.equals(order.getClientId()))
                && (from == null || !order.getOrderDate().isBefore(from))
                && (to == null || order.getOrderDate().isBefore(to))
                && (instrument == null || instrument.equals(order.getAsset().getInstrument()))
                && (buy == null || buy.booleanValue() == order.isBuy())
                && (status == null || status.equals(order.getCurrentStatus()));
    }

    private Map<Instrument, Long> zeroedInstrumentCounts() {
        Map<Instrument, Long> counts = new EnumMap<>(Instrument.class);
        for (Instrument instrument : Instrument.values()) {
            counts.put(instrument, 0L);
        }
        return counts;
    }

    private Map<Status, Long> zeroedStatusCounts() {
        Map<Status, Long> counts = new EnumMap<>(Status.class);
        for (Status status : Status.values()) {
            counts.put(status, 0L);
        }
        return counts;
    }

    private Map<String, Long> stringKeyedInstrumentCounts(Map<Instrument, Long> counts) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Instrument instrument : Instrument.values()) {
            result.put(instrument.name(), counts.get(instrument));
        }
        return result;
    }

    private Map<String, Long> stringKeyedStatusCounts(Map<Status, Long> counts) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Status status : Status.values()) {
            result.put(status.name(), counts.get(status));
        }
        return result;
    }
}
