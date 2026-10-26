package com.eatrading.api.repository;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            select value(log).statusChangeTime from Order o join o.statusChangeLog log
            where o.clientId = :clientId and o.currentStatus = :currentStatus
              and key(log) = :logStatus
              and value(log).statusChangeTime >= :startTime
              and value(log).statusChangeTime < :endTime
            """)
    List<Instant> findCompletionTimes(
            @Param("clientId") UUID clientId,
            @Param("currentStatus") Status currentStatus, @Param("logStatus") Status logStatus,
            @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Query("""
            select value(log).statusChangeTime from Order o join o.statusChangeLog log
            where o.currentStatus = :currentStatus
              and key(log) = :logStatus
              and value(log).statusChangeTime >= :startTime
              and value(log).statusChangeTime < :endTime
            """)
    List<Instant> findAllCompletionTimes(
            @Param("currentStatus") Status currentStatus, @Param("logStatus") Status logStatus,
            @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
}
