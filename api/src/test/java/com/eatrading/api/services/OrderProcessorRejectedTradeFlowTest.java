package com.eatrading.api.services;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

@SpringBootTest
@Transactional
@Import(OrderProcessorRejectedTradeFlowTest.MockConfig.class)
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
class OrderProcessorRejectedTradeFlowTest {

    private static final Logger logger = LoggerFactory.getLogger(OrderProcessorRejectedTradeFlowTest.class);

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
    void validateBuyTrade_RejectsWhenInsufficientCash() {
        logger.info("Starting rejected BUY trade test for insufficient cash");

        double realNVDAPrice = quoteService.getCurrentPrice("NVDA");
        BigDecimal nvdaPrice = BigDecimal.valueOf(realNVDAPrice);

        Client client = new Client("Low Cash User", "lowcash@example.com");
        client.addHolding(new Holding(new Asset("USD", "US DOLLAR", Instrument.CASH), new BigDecimal("25"), BigDecimal.ONE));
        prepareForInsert(client);
        client = clientRepository.saveAndFlush(client);

        UUID clientId = client.getId();
        Asset nvda = new Asset("NVDA", "NVIDIA", Instrument.EQUITY);
        Order buyOrder = orderRepository.saveAndFlush(new Order(UUID.randomUUID(), clientId, nvda, new BigDecimal("10"), true, nvdaPrice));

        OrderResponse validation = orderProcessor.validate(buyOrder);

        assertEquals(Status.REJECTED, validation.getStatusCode());
        assertTrue(validation.getRejectionReason().contains("Insufficient USD cash"));

        buyOrder.setStatus(validation.getStatusCode());
        orderRepository.saveAndFlush(buyOrder);

        entityManager.flush();
        entityManager.clear();

        Client persisted = clientRepository.findById(clientId).orElseThrow();
        assertBigDecimalEquals(new BigDecimal("25"), persisted.getUSDHolding().getQuantity());
        assertNull(persisted.getHolding("NVDA"));

        Order persistedOrder = orderRepository.findById(buyOrder.getOrderId()).orElseThrow();
        assertEquals(Status.REJECTED, persistedOrder.getCurrentStatus());
    }

    @Test
    @Commit
    void validateSellTrade_RejectsWhenInsufficientShares() {
        logger.info("Starting rejected SELL trade test for insufficient shares");

        double realAAPLPrice = quoteService.getCurrentPrice("AAPL");
        BigDecimal aaplPrice = BigDecimal.valueOf(realAAPLPrice);

        Client client = new Client("Low Shares User", "lowshares@example.com");
        client.addHolding(new Holding(new Asset("USD", "US DOLLAR", Instrument.CASH), new BigDecimal("2000"), BigDecimal.ONE));
        client.addHolding(new Holding(new Asset("AAPL", "Apple", Instrument.EQUITY), new BigDecimal("2"), aaplPrice));
        prepareForInsert(client);
        client = clientRepository.saveAndFlush(client);

        UUID clientId = client.getId();
        Asset aapl = new Asset("AAPL", "Apple", Instrument.EQUITY);
        Order sellOrder = orderRepository.saveAndFlush(new Order(UUID.randomUUID(), clientId, aapl, new BigDecimal("5"), false, aaplPrice));

        OrderResponse validation = orderProcessor.validate(sellOrder);

        assertEquals(Status.REJECTED, validation.getStatusCode());
        assertTrue(validation.getRejectionReason().contains("Insufficient shares"));

        sellOrder.setStatus(validation.getStatusCode());
        orderRepository.saveAndFlush(sellOrder);

        entityManager.flush();
        entityManager.clear();

        Client persisted = clientRepository.findById(clientId).orElseThrow();
        assertBigDecimalEquals(new BigDecimal("2"), persisted.getHolding("AAPL").getQuantity());
        assertBigDecimalEquals(new BigDecimal("2000"), persisted.getUSDHolding().getQuantity());

        Order persistedOrder = orderRepository.findById(sellOrder.getOrderId()).orElseThrow();
        assertEquals(Status.REJECTED, persistedOrder.getCurrentStatus());
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
        Producers producers() {
            return Mockito.mock(Producers.class);
        }
    }
}