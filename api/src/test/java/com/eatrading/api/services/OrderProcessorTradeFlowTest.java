package com.eatrading.api.services;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.eatrading.api.entities.Client;
import com.eatrading.api.entities.Order;
import com.eatrading.api.messaging.Producers;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Holding;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.objects.OrderResponse;
import com.eatrading.api.objects.Status;
import com.eatrading.api.repository.ClientRepository;
import com.eatrading.api.repository.OrderRepository;

import jakarta.persistence.EntityManager;

/**
 * Integration test for order processor trade flow.
 * 
 * Credentials are resolved from environment variables or .env file:
 * 
 * In CI/Jenkins, these are injected by the pipeline's Prepare Env stage.
 * Locally, ensure your .env file contains the correct DB credentials.
 */
@SpringBootTest
@Transactional
@Import(OrderProcessorTradeFlowTest.MockConfig.class)
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
class OrderProcessorTradeFlowTest {

    private static final Logger logger = LoggerFactory.getLogger(OrderProcessorTradeFlowTest.class);

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


    @Test
    @Commit
    void validateThenExecuteBuyTrade_FillsAndUpdatesHoldings() {
        logger.info("Starting BUY trade test - fetching REAL market price for NVDA");

        double realNVDAPrice = quoteService.getCurrentPrice("NVDA");
        BigDecimal nvdaPrice = BigDecimal.valueOf(realNVDAPrice);

        Client client = new Client("Buy User", "buy@example.com");
        client.addHolding(new Holding(new Asset("USD", "US DOLLAR", Instrument.CASH), new BigDecimal("5000"), BigDecimal.ONE));
        prepareForInsert(client);
        client = clientRepository.saveAndFlush(client);
        UUID clientId = client.getId();

        Asset nvda = new Asset("NVDA", "NVIDIA", Instrument.EQUITY);
        Order buyOrder = orderRepository.saveAndFlush(new Order(UUID.randomUUID(), clientId, nvda, new BigDecimal("10"), true, nvdaPrice));

        // Use the same fetched price as source of truth for the order.
        logger.info("REAL NVDA market price fetched: {}", realNVDAPrice);
        
        OrderResponse validation = orderProcessor.validate(buyOrder);
        logger.info("Buy order validation status: {}", validation.getStatusCode());
        if (Status.SUBMITTED.equals(validation.getStatusCode())) {
            // Outside market hours: order stays queued, no holdings update should happen.
            buyOrder.setStatus(validation.getStatusCode());
            orderRepository.saveAndFlush(buyOrder);

            entityManager.flush();
            entityManager.clear();

            Client persisted = clientRepository.findById(clientId).orElseThrow();
            assertNull(persisted.getHolding("NVDA"));
            assertBigDecimalEquals(new BigDecimal("5000"), persisted.getUSDHolding().getQuantity());

            Order persistedOrder = orderRepository.findById(buyOrder.getOrderId()).orElseThrow();
            assertEquals(Status.SUBMITTED, persistedOrder.getCurrentStatus());
            logger.info("BUY trade stayed SUBMITTED because market is closed");
            return;
        }

        assertEquals(Status.ACCEPTED, validation.getStatusCode());
        buyOrder.setStatus(validation.getStatusCode());
        buyOrder = orderRepository.saveAndFlush(buyOrder);

        OrderResponse execution = orderProcessor.executeOrder(buyOrder);
        logger.info("Buy order execution status: {}", execution.getStatusCode());
        assertEquals(Status.FILLED, execution.getStatusCode());
        buyOrder.setStatus(execution.getStatusCode());
        orderRepository.saveAndFlush(buyOrder);

        entityManager.flush();
        entityManager.clear();

        Client persisted = clientRepository.findById(clientId).orElseThrow();
        assertNotNull(persisted.getHolding("NVDA"));
        assertBigDecimalEquals(new BigDecimal("10"), persisted.getHolding("NVDA").getQuantity());

        BigDecimal usdHolding = persisted.getUSDHolding().getQuantity();
        logger.info("Final USD holding after BUY order: {} (price was {})", usdHolding, realNVDAPrice);
        
        // USD should be reduced by (10 shares * price)
        BigDecimal expectedUSD = new BigDecimal("5000").subtract(new BigDecimal("10").multiply(nvdaPrice));
        assertBigDecimalEquals(expectedUSD, usdHolding);

        Order persistedOrder = orderRepository.findById(buyOrder.getOrderId()).orElseThrow();
        assertEquals(Status.FILLED, persistedOrder.getCurrentStatus());
        logger.info("BUY trade test PASSED with real price: {}", realNVDAPrice);
    }

    @Test
    @Commit
    void validateThenExecuteSellTrade_FillsAndUpdatesHoldings() {
        logger.info("Starting SELL trade test - fetching REAL market price for AAPL");

        double realAAPLPrice = quoteService.getCurrentPrice("AAPL");
        BigDecimal aaplPrice = BigDecimal.valueOf(realAAPLPrice);

        Client client = new Client("Sell User", "sell@example.com");
        client.addHolding(new Holding(new Asset("USD", "US DOLLAR", Instrument.CASH), new BigDecimal("2000"), BigDecimal.ONE));
        client.addHolding(new Holding(new Asset("AAPL", "Apple", Instrument.EQUITY), new BigDecimal("20"), aaplPrice));
        prepareForInsert(client);
        client = clientRepository.saveAndFlush(client);

        UUID clientId = client.getId();
        Asset aapl = new Asset("AAPL", "Apple", Instrument.EQUITY);
        Order sellOrder = orderRepository.saveAndFlush(new Order(UUID.randomUUID(), clientId, aapl, new BigDecimal("5"), false, aaplPrice));

        // Use the same fetched price as source of truth for the order.
        logger.info("REAL AAPL market price fetched: {}", realAAPLPrice);
        
        OrderResponse validation = orderProcessor.validate(sellOrder);
        logger.info("Sell order validation status: {}", validation.getStatusCode());
        if (Status.SUBMITTED.equals(validation.getStatusCode())) {
            // Outside market hours: order stays queued, no holdings update should happen.
            sellOrder.setStatus(validation.getStatusCode());
            orderRepository.saveAndFlush(sellOrder);

            entityManager.flush();
            entityManager.clear();

            Client persisted = clientRepository.findById(clientId).orElseThrow();
            assertBigDecimalEquals(new BigDecimal("20"), persisted.getHolding("AAPL").getQuantity());
            assertBigDecimalEquals(new BigDecimal("2000"), persisted.getUSDHolding().getQuantity());

            Order persistedOrder = orderRepository.findById(sellOrder.getOrderId()).orElseThrow();
            assertEquals(Status.SUBMITTED, persistedOrder.getCurrentStatus());
            logger.info("SELL trade stayed SUBMITTED because market is closed");
            return;
        }

        assertEquals(Status.ACCEPTED, validation.getStatusCode());
        sellOrder.setStatus(validation.getStatusCode());
        sellOrder = orderRepository.saveAndFlush(sellOrder);

        OrderResponse execution = orderProcessor.executeOrder(sellOrder);
        logger.info("Sell order execution status: {}", execution.getStatusCode());
        assertEquals(Status.FILLED, execution.getStatusCode());
        sellOrder.setStatus(execution.getStatusCode());
        orderRepository.saveAndFlush(sellOrder);

        entityManager.flush();
        entityManager.clear();

        Client persisted = clientRepository.findById(clientId).orElseThrow();
        assertBigDecimalEquals(new BigDecimal("15"), persisted.getHolding("AAPL").getQuantity());
        
        BigDecimal usdHolding = persisted.getUSDHolding().getQuantity();
        logger.info("Final USD holding after SELL order: {} (price was {})", usdHolding, realAAPLPrice);
        
        // USD should be increased by (5 shares * price)
        BigDecimal expectedUSD = new BigDecimal("2000").add(new BigDecimal("5").multiply(aaplPrice));
        assertBigDecimalEquals(expectedUSD, usdHolding);

        Order persistedOrder = orderRepository.findById(sellOrder.getOrderId()).orElseThrow();
        assertEquals(Status.FILLED, persistedOrder.getCurrentStatus());
        logger.info("SELL trade test PASSED with real price: {}", realAAPLPrice);
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
        /**
         * Only mock Producers (Kafka not running in tests).
         * QuoteService is NOT mocked - uses real Fauxnance API for live market prices.
         */
        @Bean
        @Primary
        Producers producers() {
            return Mockito.mock(Producers.class);
        }
    }
}