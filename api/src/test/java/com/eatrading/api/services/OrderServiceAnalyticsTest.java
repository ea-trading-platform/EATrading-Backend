package com.eatrading.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
        UUID clientId = UUID.randomUUID();
        Instant firstDay = Instant.parse("2026-10-20T08:00:00Z");
        Instant secondDay = Instant.parse("2026-10-21T14:00:00Z");

        Order filledEquityBuy = order(clientId, firstDay, Instrument.EQUITY, true, Status.FILLED, "100");
        Order acceptedEquityBuy = order(clientId, secondDay, Instrument.EQUITY, true, Status.ACCEPTED, "30");
        Order canceledCryptoSell = order(
                clientId, secondDay, Instrument.CRYPTO, false, Status.CANCELED, "50");
        Order anotherClientOrder = orderForAnotherClient(UUID.randomUUID());
        when(orderRepository.findAll()).thenReturn(
                List.of(filledEquityBuy, acceptedEquityBuy, canceledCryptoSell, anotherClientOrder));

        OrderService service = new OrderService(orderRepository);
        var analytics = service.getOrderAnalytics(
                clientId,
                Instant.parse("2026-10-20T00:00:00Z"),
                Instant.parse("2026-10-22T00:00:00Z"),
                null,
                null,
                null);

        assertEquals(3, analytics.getTotalOrders());
        assertEquals(new BigDecimal("180"), analytics.getTotalQuantity());
        assertEquals(2, analytics.getBuyOrders());
        assertEquals(1, analytics.getSellOrders());
        assertEquals(1, analytics.getFilledOrders());
        assertEquals(1, analytics.getPendingOrders());
        assertEquals(1, analytics.getCanceledOrders());
        assertEquals(0, analytics.getRejectedOrders());
        assertEquals(2L, analytics.getOrdersByInstrument().get("EQUITY"));
        assertEquals(1L, analytics.getOrdersByInstrument().get("CRYPTO"));
        assertEquals(0L, analytics.getOrdersByInstrument().get("CASH"));
        assertEquals(2L, analytics.getOrdersBySide().get("BUY"));
        assertEquals(1L, analytics.getOrdersBySide().get("SELL"));
        assertEquals(1L, analytics.getOrdersByStatus().get("FILLED"));
        assertEquals(1L, analytics.getOrdersByStatus().get("ACCEPTED"));
        var filteredAnalytics = service.getOrderAnalytics(
                clientId,
                Instant.parse("2026-10-20T00:00:00Z"),
                Instant.parse("2026-10-22T00:00:00Z"),
                Instrument.EQUITY,
                true,
                Status.FILLED);

        assertEquals(1, filteredAnalytics.getTotalOrders());
        assertEquals(new BigDecimal("100"), filteredAnalytics.getTotalQuantity());
        assertEquals(1, filteredAnalytics.getFilledOrders());
        assertEquals(0, filteredAnalytics.getPendingOrders());
    }

    private Order order(UUID clientId, Instant orderDate, Instrument instrument, boolean buy,
            Status status, String quantity) {
        Order order = org.mockito.Mockito.mock(Order.class);
        when(order.getClientId()).thenReturn(clientId);
        when(order.getOrderDate()).thenReturn(orderDate);
        when(order.getAsset()).thenReturn(new Asset("TEST", "Test asset", instrument));
        when(order.isBuy()).thenReturn(buy);
        when(order.getCurrentStatus()).thenReturn(status);
        when(order.getQuantity()).thenReturn(new BigDecimal(quantity));
        return order;
    }

    private Order orderForAnotherClient(UUID clientId) {
        Order order = org.mockito.Mockito.mock(Order.class);
        when(order.getClientId()).thenReturn(clientId);
        return order;
    }
}
