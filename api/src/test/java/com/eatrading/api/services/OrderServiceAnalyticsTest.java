package com.eatrading.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eatrading.api.entities.Order;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.objects.Status;
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
        Order acceptedEquityBuy = order(secondDay, Instrument.EQUITY, true, Status.ACCEPTED, "30");
        Order canceledCryptoSell = order(
                secondDay, Instrument.CRYPTO, false, Status.CANCELED, "50");
        Order anotherClientOrder = order(
                firstDay, Instrument.EQUITY, true, Status.FILLED, "999");
        when(orderRepository.findAll()).thenReturn(
                List.of(filledEquityBuy, acceptedEquityBuy, canceledCryptoSell, anotherClientOrder));

        OrderService service = new OrderService(orderRepository);
        var analytics = service.getOrderAnalytics(
                Instant.parse("2026-10-20T00:00:00Z"),
                Instant.parse("2026-10-22T00:00:00Z"),
                null,
                null,
                null);

        assertEquals(4, analytics.getTotalOrders());
        assertEquals(new BigDecimal("1179"), analytics.getTotalQuantity());
        assertEquals(3L, analytics.getOrdersByInstrument().get("EQUITY"));
        assertEquals(1L, analytics.getOrdersByInstrument().get("CRYPTO"));
        assertEquals(0L, analytics.getOrdersByInstrument().get("CASH"));
        assertEquals(3L, analytics.getOrdersBySide().get("BUY"));
        assertEquals(1L, analytics.getOrdersBySide().get("SELL"));
        assertEquals(2L, analytics.getOrdersByStatus().get("FILLED"));
        assertEquals(1L, analytics.getOrdersByStatus().get("ACCEPTED"));
        assertEquals(1L, analytics.getOrdersByStatus().get("CANCELED"));
        assertEquals(0L, analytics.getOrdersByStatus().get("REJECTED"));
        var filteredAnalytics = service.getOrderAnalytics(
                Instant.parse("2026-10-20T00:00:00Z"),
                Instant.parse("2026-10-22T00:00:00Z"),
                Instrument.EQUITY,
                true,
                Status.FILLED);

        assertEquals(2, filteredAnalytics.getTotalOrders());
        assertEquals(new BigDecimal("1099"), filteredAnalytics.getTotalQuantity());
        assertEquals(2L, filteredAnalytics.getOrdersByStatus().get("FILLED"));
        assertEquals(0L, filteredAnalytics.getOrdersByStatus().get("ACCEPTED"));
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
