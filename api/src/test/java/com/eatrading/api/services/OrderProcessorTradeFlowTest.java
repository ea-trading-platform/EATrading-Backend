package com.eatrading.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.eatrading.api.dto.Quote;
import com.eatrading.api.entities.Client;
import com.eatrading.api.entities.Order;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Holding;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.objects.OrderResponse;
import com.eatrading.api.objects.Status;
import com.eatrading.api.repository.ClientRepository;
import com.eatrading.api.repository.OrderRepository;
import com.eatrading.api.support.TradeTestFixtures;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
@Import(OrderProcessorTradeFlowTest.MockConfig.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:8088/ea-db",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.datasource.username=${DB_USER:eauser}",
    "spring.datasource.password=${DB_PASSWORD:securepassword}",
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
    "spring.kafka.listener.auto-startup=false",
    "fauxnance.api.key=test-key",
    "FAUXNANCE_API_KEY=test-key"
})
class OrderProcessorTradeFlowTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private QuoteService quoteService;

    @Autowired
    private OrderProcessor orderProcessor;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        Mockito.reset(quoteService);
    }

    @Test
    @Commit
    void validateThenExecuteBuyTrade_FillsAndUpdatesHoldings() {
        Client client = new Client("Buy User", "buy@example.com");
        client.addHolding(new Holding(new Asset("USD", "US DOLLAR", Instrument.CASH), new BigDecimal("5000")));
        prepareForInsert(client);
        client = clientRepository.saveAndFlush(client);
        UUID clientId = client.getId();

        Asset nvda = new Asset("NVDA", "NVIDIA", Instrument.EQUITY);
        Order buyOrder = orderRepository.saveAndFlush(new Order(clientId, nvda, new BigDecimal("10"), true));

        Quote nvdaQuote = TradeTestFixtures.quote("NVDA", 2.02);
        when(quoteService.getQuote("NVDA")).thenReturn(nvdaQuote);

        OrderResponse validation = orderProcessor.validate(buyOrder);
        assertEquals(Status.ACCEPTED, validation.getStatusCode());
        buyOrder.setStatus(validation.getStatusCode());
        buyOrder = orderRepository.saveAndFlush(buyOrder);

        OrderResponse execution = orderProcessor.executeOrder(buyOrder);
        assertEquals(Status.FILLED, execution.getStatusCode());
        buyOrder.setStatus(execution.getStatusCode());
        orderRepository.saveAndFlush(buyOrder);

        entityManager.flush();
        entityManager.clear();

        Client persisted = clientRepository.findById(clientId).orElseThrow();
        assertNotNull(persisted.getHolding("NVDA"));
        assertBigDecimalEquals(new BigDecimal("10"), persisted.getHolding("NVDA").getQuantity());

        // Current executeOrder logic removes USD quantity by purchased value and uses Asset mock market prices.
        assertBigDecimalEquals(new BigDecimal("4980"), persisted.getUSDHolding().getQuantity());

        Order persistedOrder = orderRepository.findById(buyOrder.getOrderID()).orElseThrow();
        assertEquals(Status.FILLED, persistedOrder.getCurrentStatus());
    }

    @Test
    @Commit
    void validateThenExecuteSellTrade_FillsAndUpdatesHoldings() {
        Client client = new Client("Sell User", "sell@example.com");
        client.addHolding(new Holding(new Asset("USD", "US DOLLAR", Instrument.CASH), new BigDecimal("2000")));
        client.addHolding(new Holding(new Asset("AAPL", "Apple", Instrument.EQUITY), new BigDecimal("20")));
        prepareForInsert(client);
        client = clientRepository.saveAndFlush(client);

        UUID clientId = client.getId();
        Asset aapl = new Asset("AAPL", "Apple", Instrument.EQUITY);
        Order sellOrder = orderRepository.saveAndFlush(new Order(clientId, aapl, new BigDecimal("5"), false));

        Quote aaplQuote = TradeTestFixtures.quote("AAPL", 1.98);
        when(quoteService.getQuote("AAPL")).thenReturn(aaplQuote);

        OrderResponse validation = orderProcessor.validate(sellOrder);
        assertEquals(Status.ACCEPTED, validation.getStatusCode());
        sellOrder.setStatus(validation.getStatusCode());
        sellOrder = orderRepository.saveAndFlush(sellOrder);

        OrderResponse execution = orderProcessor.executeOrder(sellOrder);
        assertEquals(Status.FILLED, execution.getStatusCode());
        sellOrder.setStatus(execution.getStatusCode());
        orderRepository.saveAndFlush(sellOrder);

        entityManager.flush();
        entityManager.clear();

        Client persisted = clientRepository.findById(clientId).orElseThrow();
        assertBigDecimalEquals(new BigDecimal("15"), persisted.getHolding("AAPL").getQuantity());
        assertBigDecimalEquals(new BigDecimal("2010"), persisted.getUSDHolding().getQuantity());

        Order persistedOrder = orderRepository.findById(sellOrder.getOrderID()).orElseThrow();
        assertEquals(Status.FILLED, persistedOrder.getCurrentStatus());
    }

    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual),
                () -> "Expected " + expected + " but was " + actual);
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
        QuoteService quoteService() {
            return Mockito.mock(QuoteService.class);
        }

        @Bean
        @Primary
        KafkaTemplate<String, String> kafkaTemplate() {
            return Mockito.mock(KafkaTemplate.class);
        }
    }
}