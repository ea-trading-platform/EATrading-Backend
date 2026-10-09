package com.eatrading.api.services;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eatrading.api.entities.Order;
import com.eatrading.api.exception.ResourceNotFoundException;
import com.eatrading.api.exception.UnprocessableEntityException;
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

    public Order getOrderByTrackingId(UUID trackingId) {
        return orderRepository.findByTrackingId(trackingId);
    }

    @Transactional
    public void saveIncomingOrder(Order order) {
        orderRepository.save(order);
    }

    @Transactional
    public Map<String, Object> cancelOrder(UUID trackingId) {
        Order order = getOrderByTrackingId(trackingId);

        if (order == null) {
            throw new ResourceNotFoundException("Order not found for trackingId: " + trackingId);
        }

        Status currentStatus = order.getCurrentStatus();
        if (!Status.SUBMITTED.equals(currentStatus)) {
            throw new UnprocessableEntityException(
                "Order with trackingId " + trackingId + " cannot be canceled because current status is " + currentStatus + "."
            );
        }

        order.setStatus(Status.CANCELED);
        orderRepository.save(order);

        return Map.of(
            "status", "success",
            "message", "Order canceled successfully",
            "trackingId", trackingId,
            "currentStatus", Status.CANCELED.toString()
        );
    }
}
