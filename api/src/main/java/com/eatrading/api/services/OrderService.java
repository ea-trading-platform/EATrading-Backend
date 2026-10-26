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
import com.eatrading.api.dto.OrderStatusLogResponse;
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
    public OrderAnalyticsResponse getOrderAnalytics(Instant from, Instant to,
            Instrument instrument, Boolean buy, Status status) {
        Map<Instrument, Long> ordersByInstrument = zeroedInstrumentCounts();
        Map<Status, Long> ordersByStatus = zeroedStatusCounts();
        Map<String, Long> ordersBySide = zeroedSideCounts();

        long totalOrders = 0;
        BigDecimal totalQuantity = BigDecimal.ZERO;

        for (Order order : orderRepository.findAll()) {
            if (!matchesFilters(order, from, to, instrument, buy, status)) {
                continue;
            }

            totalQuantity = totalQuantity.add(order.getQuantity());
            totalOrders++;
            String side = order.isBuy() ? "BUY" : "SELL";
            ordersBySide.compute(side, (key, count) -> count + 1);

            Instrument orderInstrument = order.getAsset().getInstrument();
            Status orderStatus = order.getCurrentStatus();
            ordersByInstrument.compute(orderInstrument, (key, count) -> count + 1);
            ordersByStatus.compute(orderStatus, (key, count) -> count + 1);
        }

        return new OrderAnalyticsResponse(
                totalOrders,
                totalQuantity,
                stringKeyedInstrumentCounts(ordersByInstrument),
                ordersBySide,
                stringKeyedStatusCounts(ordersByStatus));
    }

    @Transactional(readOnly = true)
    public Map<Status, OrderStatusLogResponse> getStatusLogByTrackingId(UUID trackingId) {
        Order order = getOrderByTrackingId(trackingId);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found for trackingId: " + trackingId);
        }

        Map<Status, OrderStatusLogResponse> statusLog = new EnumMap<>(Status.class);
        order.getStatusLogEntries().forEach((status, entry) -> statusLog.put(status,
                new OrderStatusLogResponse(entry.getStatusChangeTime(), entry.getRejectionReason())));
        return Map.copyOf(statusLog);
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

    private boolean matchesFilters(Order order, Instant from, Instant to,
            Instrument instrument, Boolean buy, Status status) {
        return (from == null || !order.getOrderDate().isBefore(from))
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

    private Map<String, Long> zeroedSideCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("BUY", 0L);
        counts.put("SELL", 0L);
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
