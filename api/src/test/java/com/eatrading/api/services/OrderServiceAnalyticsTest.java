package com.eatrading.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eatrading.api.entities.Order;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.objects.Status;
import com.eatrading.api.objects.TradeInterval;
import com.eatrading.api.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceAnalyticsTest {

    @Mock
    private OrderRepository orderRepository;

    @Test
    void getOrderAnalyticsAggregatesOrdersAndUsesAllFiltersTogether() {
        Instant firstDay = Instant.parse("2026-10-20T08:00:00Z");
        Instant secondDay = Instant.parse("2026-10-21T14:00:00Z");

        Order filledEquityBuy = order(firstDay, Instrument.EQUITY, true, Status.FILLED, "100");
        Order filledEquitySell = order(secondDay, Instrument.EQUITY, false, Status.FILLED, "7");
        Order acceptedEquityBuy = order(secondDay, Instrument.EQUITY, true, Status.ACCEPTED, "30");
        Order canceledCryptoSell = order(
                secondDay, Instrument.CRYPTO, false, Status.CANCELED, "50");
        Order anotherClientOrder = order(
                firstDay, Instrument.EQUITY, true, Status.FILLED, "999");
        when(orderRepository.findAll()).thenReturn(
                List.of(filledEquityBuy, filledEquitySell, acceptedEquityBuy,
                        canceledCryptoSell, anotherClientOrder));
        when(orderRepository.findAllCompletionTimes(
                Status.FILLED, Status.FILLED,
                Instant.parse("2026-10-20T00:00:00Z"),
                Instant.parse("2026-10-22T00:00:00Z"))).thenReturn(List.of(
                        firstDay, firstDay.plusSeconds(3600), secondDay));

        OrderService service = new OrderService(orderRepository);
        var analytics = service.getOrderAnalytics(
                Instant.parse("2026-10-20T00:00:00Z"),
                Instant.parse("2026-10-22T00:00:00Z"),
                null,
                null,
                null,
                TradeInterval.DAY);

        assertEquals(5, analytics.getTotalOrders());
        assertEquals(new BigDecimal("1186"), analytics.getTotalQuantity());
        assertEquals(3L, analytics.getOrdersByInstrument().get("EQUITY"));
        assertEquals(0L, analytics.getOrdersByInstrument().get("CRYPTO"));
        assertEquals(0L, analytics.getOrdersByInstrument().get("CASH"));
        assertEquals(2L, analytics.getOrdersBySide().get("BUY"));
        assertEquals(1L, analytics.getOrdersBySide().get("SELL"));
        assertEquals(3L, analytics.getOrdersByStatus().get("FILLED"));
        assertEquals(1L, analytics.getOrdersByStatus().get("ACCEPTED"));
        assertEquals(1L, analytics.getOrdersByStatus().get("CANCELED"));
        assertEquals(0L, analytics.getOrdersByStatus().get("REJECTED"));
        assertEquals(List.of(2L, 1L),
                analytics.getTradeCounts().stream().map(point -> point.getTradeCount()).toList());
        assertEquals(List.of(
                Instant.parse("2026-10-20T00:00:00Z"),
                Instant.parse("2026-10-21T00:00:00Z")),
                analytics.getTradeCounts().stream().map(point -> point.getTimestamp()).toList());
        var filteredAnalytics = service.getOrderAnalytics(
                Instant.parse("2026-10-20T00:00:00Z"),
                Instant.parse("2026-10-22T00:00:00Z"),
                Instrument.EQUITY,
                true,
                Status.FILLED,
                TradeInterval.DAY);

        assertEquals(2, filteredAnalytics.getTotalOrders());
        assertEquals(new BigDecimal("1099"), filteredAnalytics.getTotalQuantity());
        assertEquals(2L, filteredAnalytics.getOrdersByStatus().get("FILLED"));
        assertEquals(0L, filteredAnalytics.getOrdersByStatus().get("ACCEPTED"));
        assertEquals(Map.of("BUY", 2L, "SELL", 0L), filteredAnalytics.getOrdersBySide());
        assertEquals(Map.of("EQUITY", 2L, "CRYPTO", 0L, "CASH", 0L),
                filteredAnalytics.getOrdersByInstrument());
    }

    @ParameterizedTest
    @EnumSource(value = Status.class, names = {"SUBMITTED", "ACCEPTED", "CANCELED", "REJECTED"})
    void getOrderAnalyticsExcludesUnfilledOrdersFromSideAndInstrumentCounts(Status status) {
        Instant from = Instant.parse("2026-10-20T00:00:00Z");
        Instant to = Instant.parse("2026-10-22T00:00:00Z");
        List<Order> orders = List.of(
                order(from, Instrument.EQUITY, true, status, "10"),
                order(from.plusSeconds(3600), Instrument.EQUITY, false, status, "20"));
        when(orderRepository.findAll()).thenReturn(orders);

        var analytics = new OrderService(orderRepository).getOrderAnalytics(
                from, to, null, null, status, TradeInterval.DAY);

        assertEquals(Map.of("BUY", 0L, "SELL", 0L), analytics.getOrdersBySide());
        assertEquals(2, analytics.getTotalOrders());
        assertEquals(new BigDecimal("30"), analytics.getTotalQuantity());
        assertEquals(Map.of("EQUITY", 0L, "CRYPTO", 0L, "CASH", 0L),
                analytics.getOrdersByInstrument());
        assertEquals(2L, analytics.getOrdersByStatus().get(status.name()));
    }

    private Order order(Instant orderDate, Instrument instrument, boolean buy,
            Status status, String quantity) {
        Order order = org.mockito.Mockito.mock(Order.class);
        when(order.getOrderDate()).thenReturn(orderDate);
        when(order.getAsset()).thenReturn(new Asset("TEST", "Test asset", instrument));
        when(order.isBuy()).thenReturn(buy);
        when(order.getCurrentStatus()).thenReturn(status);
        when(order.getQuantity()).thenReturn(new BigDecimal(quantity));
        return order;
    }

}
