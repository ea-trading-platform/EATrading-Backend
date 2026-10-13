package com.eatrading.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eatrading.api.dto.OrderTransactionRequest;
import com.eatrading.api.entities.Order;
import com.eatrading.api.messaging.Producers;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.BaseMarket;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.objects.Status;
import com.eatrading.api.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class MarketOpenBatchExecutorTest {

    @Mock
    private Producers producers;

    @Mock
    private OrderRepository orderRepository;

    private MarketOpenBatchExecutor batchExecutor;

    @BeforeEach
    void setUp() {
        batchExecutor = new MarketOpenBatchExecutor(producers, orderRepository);
    }

    @Test
    void processAfterHoursOrdersUS_queuesSubmittedUsOrdersForValidation() {
        Order order = order(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new Asset("NVDA", "NVIDIA", Instrument.EQUITY),
                new BigDecimal("10"),
                true,
                new BigDecimal("125.50"));

        when(orderRepository.findByCurrentStatusAndBaseMarket(Status.SUBMITTED, BaseMarket.US))
                .thenReturn(List.of(order));

        batchExecutor.processAfterHoursOrdersUS();

        verify(orderRepository, times(1)).findByCurrentStatusAndBaseMarket(Status.SUBMITTED, BaseMarket.US);

        ArgumentCaptor<OrderTransactionRequest> requestCaptor = ArgumentCaptor.forClass(OrderTransactionRequest.class);
        verify(producers, times(1)).addToValidationQueue(requestCaptor.capture());

        OrderTransactionRequest request = requestCaptor.getValue();
        assertEquals(order.getTrackingId(), request.getTrackingId());
        assertEquals(order.getClientId().toString(), request.getClientId());
        assertEquals("NVDA", request.getSymbol());
        assertEquals("NVIDIA", request.getName());
        assertEquals("BUY", request.getTransactionType());
        assertEquals(0, order.getQuantity().compareTo(request.getQuantity()));
    }

    @Test
    void processAfterHoursOrdersIN_queuesSubmittedIndianOrdersForValidation() {
        Order order = order(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new Asset("INFY", "INFOSYS", Instrument.EQUITY),
                new BigDecimal("5"),
                false,
                new BigDecimal("17.10"));

        when(orderRepository.findByCurrentStatusAndBaseMarket(Status.SUBMITTED, BaseMarket.IN))
                .thenReturn(List.of(order));

        batchExecutor.processAfterHoursOrdersIN();

        verify(orderRepository, times(1)).findByCurrentStatusAndBaseMarket(Status.SUBMITTED, BaseMarket.IN);

        ArgumentCaptor<OrderTransactionRequest> requestCaptor = ArgumentCaptor.forClass(OrderTransactionRequest.class);
        verify(producers, times(1)).addToValidationQueue(requestCaptor.capture());

        OrderTransactionRequest request = requestCaptor.getValue();
        assertEquals(order.getTrackingId(), request.getTrackingId());
        assertEquals(order.getClientId().toString(), request.getClientId());
        assertEquals("INFY", request.getSymbol());
        assertEquals("INFOSYS", request.getName());
        assertEquals("SELL", request.getTransactionType());
        assertEquals(0, order.getQuantity().compareTo(request.getQuantity()));
    }

    @Test
    void processAfterHoursOrdersUS_skipsQueueWhenNoOrdersFound() {
        when(orderRepository.findByCurrentStatusAndBaseMarket(Status.SUBMITTED, BaseMarket.US))
                .thenReturn(List.of());

        batchExecutor.processAfterHoursOrdersUS();

        verify(orderRepository, times(1)).findByCurrentStatusAndBaseMarket(Status.SUBMITTED, BaseMarket.US);
        verify(producers, never()).addToValidationQueue(any(OrderTransactionRequest.class));
    }

    private Order order(UUID trackingId, UUID clientId, Asset asset, BigDecimal quantity, boolean buy, BigDecimal price) {
        return new Order(trackingId, clientId, asset, quantity, buy, price);
    }
}