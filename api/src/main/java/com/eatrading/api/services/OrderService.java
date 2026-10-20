package com.eatrading.api.services;

import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
