package com.eatrading.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.eatrading.api.dto.OrderTransactionRequest;
import com.eatrading.api.dto.Quote;
import com.eatrading.api.entities.Client;
import com.eatrading.api.entities.Order;
import com.eatrading.api.messaging.Consumers;
import com.eatrading.api.messaging.Producers;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.BaseMarket;
import com.eatrading.api.objects.Holding;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.objects.Status;
import com.eatrading.api.repository.ClientRepository;
import com.eatrading.api.repository.OrderRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
@Import(MarketOpenBatchExecutorIntegrationTest.MockConfig.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:8088/ea-db",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.datasource.username=${POSTGRES_USER}",
    "spring.datasource.password=${POSTGRES_PASSWORD}",
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    "fauxnance.api.url=https://y4t9nq2bqf.execute-api.eu-west-2.amazonaws.com/v1",
    "fauxnance.api.key=${FAUXNANCE_API_KEY:fnx_dev_TpCmhRrgodNApR4wLCG8VlikeOZqix8u}",
    "DB_URL=jdbc:postgresql://localhost:8088/ea-db",
    "DB_USER=${POSTGRES_USER}",
    "DB_PASSWORD=${POSTGRES_PASSWORD}",
    "spring.kafka.bootstrap-servers=localhost:9092",
    "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer",
    "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer"
})
class MarketOpenBatchExecutorIntegrationTest {

    @Autowired
    private MarketOpenBatchExecutor batchExecutor;

    @Autowired
    private Consumers consumers;

    @Autowired
    private Producers producers;

    @Autowired
    private QuoteService quoteService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void resetMocks() {
        reset(producers, quoteService);
    }

    @Test
    void processAfterHoursOrdersUS_picksSubmittedOrderAndValidatesIt() {
        Quote quote = quote("NVDA", 130.25, "open", "USD", "2026-10-07T14:00:00Z");
        when(quoteService.getCurrentPrice("NVDA")).thenReturn(130.25);
        when(quoteService.getQuote("NVDA")).thenReturn(quote, quote, quote);

        Client client = persistedClient("us-batch@example.com", new BigDecimal("5000"), null, null, null);
        Order order = persistedOrder(client.getId(), new Asset("NVDA", "NVIDIA", Instrument.EQUITY), new BigDecimal("10"), true,
                new BigDecimal("125.50"), BaseMarket.US);

        batchExecutor.processAfterHoursOrdersUS();

        ArgumentCaptor<OrderTransactionRequest> validationCaptor = ArgumentCaptor.forClass(OrderTransactionRequest.class);
        verify(producers, atLeastOnce()).addToValidationQueue(validationCaptor.capture());

        OrderTransactionRequest queuedRequest = validationCaptor.getAllValues().stream()
            .filter(request -> order.getTrackingId().equals(request.getTrackingId()))
            .findFirst()
            .orElse(null);

        assertTrue(queuedRequest != null, "Expected the US submitted order to be queued for validation");

        reset(producers);

        consumers.consumeValidate(queuedRequest);

        verify(producers, times(1)).addToExecutionQueue(queuedRequest);

        entityManager.flush();
        entityManager.clear();

        Order persistedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals(Status.ACCEPTED, persistedOrder.getCurrentStatus());
        assertEquals(BaseMarket.US, persistedOrder.getBaseMarket());
        assertEquals(0, persistedOrder.getPrice().compareTo(new BigDecimal("130.25")));
    }

    @Test
    void processAfterHoursOrdersIN_picksSubmittedOrderAndValidatesIt() {
        Quote quote = quote("INFY", 18.20, "open", "INR", "2026-10-07T04:30:00Z");
        when(quoteService.getCurrentPrice("INFY")).thenReturn(18.20);
        when(quoteService.getQuote("INFY")).thenReturn(quote, quote, quote);

        Client client = persistedClient("in-batch@example.com", new BigDecimal("5000"), null, null, null);
        Order order = persistedOrder(client.getId(), new Asset("INFY", "INFOSYS", Instrument.EQUITY), new BigDecimal("5"), true,
                new BigDecimal("17.10"), BaseMarket.IN);

        batchExecutor.processAfterHoursOrdersIN();

        ArgumentCaptor<OrderTransactionRequest> validationCaptor = ArgumentCaptor.forClass(OrderTransactionRequest.class);
        verify(producers, atLeastOnce()).addToValidationQueue(validationCaptor.capture());

        OrderTransactionRequest queuedRequest = validationCaptor.getAllValues().stream()
            .filter(request -> order.getTrackingId().equals(request.getTrackingId()))
            .findFirst()
            .orElse(null);

        assertTrue(queuedRequest != null, "Expected the India submitted order to be queued for validation");

        reset(producers);

        consumers.consumeValidate(queuedRequest);

        verify(producers, times(1)).addToExecutionQueue(queuedRequest);

        entityManager.flush();
        entityManager.clear();

        Order persistedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals(Status.ACCEPTED, persistedOrder.getCurrentStatus());
        assertEquals(BaseMarket.IN, persistedOrder.getBaseMarket());
        assertEquals(0, persistedOrder.getPrice().compareTo(new BigDecimal("18.20")));
    }

    private Client persistedClient(String email, BigDecimal usdCash, String symbol, BigDecimal shares, BigDecimal averagePrice) {
        Client client = new Client("Batch User", email);
        client.setPasswordHash("test-password-hash");
        client.addHolding(new Holding(new Asset("USD", "US DOLLAR", Instrument.CASH), usdCash, BigDecimal.ONE));
        if (symbol != null && shares != null && averagePrice != null) {
            client.addHolding(new Holding(new Asset(symbol, symbol, Instrument.EQUITY), shares, averagePrice));
        }
        prepareForInsert(client);
        return clientRepository.saveAndFlush(client);
    }

    private Order persistedOrder(UUID clientId, Asset asset, BigDecimal quantity, boolean buy, BigDecimal price, BaseMarket baseMarket) {
        Order order = new Order(UUID.randomUUID(), clientId, asset, quantity, buy, price);
        order.setBaseMarket(baseMarket);
        return orderRepository.saveAndFlush(order);
    }

    private Quote quote(String symbol, double price, String marketState, String currency, String asOf) {
        Quote quote = new Quote();
        quote.setSymbol(symbol);
        quote.setPrice(price);
        quote.setMarketState(marketState);
        quote.setCurrency(currency);
        quote.setAsOf(asOf);
        return quote;
    }

    private void prepareForInsert(Client client) {
        try {
            Field idField = client.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(client, null);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to prepare client entity for insert", e);
        }
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        Producers producers() {
            return Mockito.mock(Producers.class);
        }

        @Bean
        @Primary
        QuoteService quoteService() {
            return Mockito.mock(QuoteService.class);
        }
    }
}