package com.eatrading.api.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.eatrading.api.dto.OrderTransactionRequest;
import com.eatrading.api.entities.Order;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.objects.OrderResponse;
import com.eatrading.api.objects.OrderStatusLogEntry;
import com.eatrading.api.objects.Status;
import com.eatrading.api.services.OrderProcessor;
import com.eatrading.api.services.OrderService;
import com.eatrading.api.services.QuoteService;

class ConsumersTest {

    @Test
    void consumeValidateAndExecute_restoreMissingSubmissionHistory_whenOrderCompletes() {
        OrderProcessor orderProcessor = mock(OrderProcessor.class);
        OrderService orderService = mock(OrderService.class);
        Producers producers = mock(Producers.class);
        Consumers consumers = new Consumers(orderProcessor, producers, mock(QuoteService.class), orderService);
        Order order = new Order(UUID.randomUUID(), UUID.randomUUID(),
                new Asset("AAPL", "APPLE", Instrument.EQUITY), BigDecimal.ONE, true, new BigDecimal("100"));
        Instant orderDate = order.getOrderDate();
        order.getStatusLogEntries().clear();
        OrderTransactionRequest request = new OrderTransactionRequest();
        request.setTrackingId(order.getTrackingId());
        OrderResponse accepted = new OrderResponse();
        accepted.setStatusCode(Status.ACCEPTED);
        OrderResponse filled = new OrderResponse();
        filled.setStatusCode(Status.FILLED);
        when(orderService.getOrderByTrackingId(order.getTrackingId())).thenReturn(order);
        when(orderProcessor.validate(order)).thenReturn(accepted);
        when(orderProcessor.executeOrder(order)).thenReturn(filled);

        TransactionSynchronizationManager.initSynchronization();
        try {
            consumers.consumeValidate(request);
            assertEquals(orderDate, order.getStatusChangeLog().get(Status.SUBMITTED));
            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
        consumers.consumeExecute(request);

        verify(producers).addToExecutionQueue(request);
        assertEquals(Status.FILLED, order.getCurrentStatus());
        assertEquals(orderDate, order.getOrderDate());
        assertEquals(orderDate, order.getStatusChangeLog().get(Status.SUBMITTED));
        assertEquals(3, order.getStatusChangeLog().size());
    }

    @Test
    void consumeValidate_preservesSubmittedTime_whenRevalidationStillDefersOrder() {
        OrderProcessor orderProcessor = mock(OrderProcessor.class);
        OrderService orderService = mock(OrderService.class);
        Producers producers = mock(Producers.class);
        Consumers consumers = new Consumers(orderProcessor, producers, mock(QuoteService.class), orderService);
        Order order = new Order(UUID.randomUUID(), UUID.randomUUID(),
                new Asset("AAPL", "APPLE", Instrument.EQUITY), BigDecimal.ONE, true, new BigDecimal("100"));
        Instant orderDate = order.getOrderDate();
        Instant submittedAt = orderDate.minusSeconds(3600);
        order.getStatusLogEntries().put(Status.SUBMITTED, new OrderStatusLogEntry(submittedAt, null));
        OrderTransactionRequest request = new OrderTransactionRequest();
        request.setTrackingId(order.getTrackingId());
        when(orderService.getOrderByTrackingId(order.getTrackingId())).thenReturn(order);
        when(orderProcessor.validate(order)).thenReturn(new OrderResponse());

        consumers.consumeValidate(request);
        consumers.consumeValidate(request);

        assertEquals(Status.SUBMITTED, order.getCurrentStatus());
        assertEquals(submittedAt, order.getStatusChangeLog().get(Status.SUBMITTED));
        assertEquals(orderDate, order.getOrderDate());
        verifyNoInteractions(producers);
    }
}
