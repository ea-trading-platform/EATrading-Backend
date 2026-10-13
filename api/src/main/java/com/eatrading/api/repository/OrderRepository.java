package com.eatrading.api.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eatrading.api.entities.Order;
import com.eatrading.api.objects.Status;
import com.eatrading.api.objects.BaseMarket;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByClientId(UUID clientId);
    Order findByTrackingId(UUID trackingId);
    List<Order> findByCurrentStatusAndBaseMarket(Status status, BaseMarket baseMarket);
}
