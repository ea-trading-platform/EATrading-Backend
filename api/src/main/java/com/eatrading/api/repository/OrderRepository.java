package com.eatrading.api.repository;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eatrading.api.entities.Order;
import com.eatrading.api.objects.BaseMarket;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.objects.Status;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByClientId(UUID clientId);
    Order findByTrackingId(UUID trackingId);
    List<Order> findByCurrentStatusAndBaseMarket(Status status, BaseMarket baseMarket);
    List<Order> findByAssetInstrument(Instrument instrument);
    List<Order> findByBuy(boolean buy);
    List<Order> findByCurrentStatus(Status status);
    List<Order> findByOrderDateGreaterThanEqualAndOrderDateLessThan(Instant from, Instant to);
}
