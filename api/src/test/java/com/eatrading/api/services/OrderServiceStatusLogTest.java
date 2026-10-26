package com.eatrading.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eatrading.api.dto.OrderStatusLogResponse;
import com.eatrading.api.entities.Order;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.objects.Status;
import com.eatrading.api.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceStatusLogTest {

    @Mock
    private OrderRepository orderRepository;

    @Test
    void returnsTimestampsAndRejectionReasonsFromStoredEntries() {
        UUID trackingId = UUID.randomUUID();
        Instant submittedAt = Instant.parse("2026-01-02T03:04:05Z");
        Instant rejectedAt = submittedAt.plusSeconds(30);
        Order order = new Order(trackingId, UUID.randomUUID(),
                new Asset("TSLA", "Tesla", Instrument.EQUITY), BigDecimal.ONE, false, BigDecimal.TEN);
        order.setStatus(Status.REJECTED, "Client does not own TSLA");
        when(orderRepository.findByTrackingId(trackingId)).thenReturn(order);

        var response = new OrderService(orderRepository).getStatusLogByTrackingId(trackingId);

        OrderStatusLogResponse submitted = response.get(Status.SUBMITTED);
        OrderStatusLogResponse rejected = response.get(Status.REJECTED);
        assertEquals(submittedAt, submitted.getStatusChangeTime());
        assertEquals(null, submitted.getRejectionReason());
        assertEquals(rejectedAt, rejected.getStatusChangeTime());
        assertEquals("Client does not own TSLA", rejected.getRejectionReason());
    }
}