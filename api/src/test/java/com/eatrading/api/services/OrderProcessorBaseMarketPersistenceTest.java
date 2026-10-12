package com.eatrading.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.eatrading.api.dto.Quote;
import com.eatrading.api.entities.Client;
import com.eatrading.api.entities.Order;
import com.eatrading.api.messaging.Producers;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.BaseMarket;
import com.eatrading.api.objects.Holding;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.objects.OrderResponse;
import com.eatrading.api.objects.Status;
import com.eatrading.api.repository.ClientRepository;
import com.eatrading.api.repository.OrderRepository;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
@Import(OrderProcessorBaseMarketPersistenceTest.TestConfig.class)
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
class OrderProcessorBaseMarketPersistenceTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderProcessor orderProcessor;

    @Autowired
    private QuoteService quoteService;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(quoteService);
    }

    @Test
    void validate_updatesBaseMarketColumnForManagedOrder() {
        Quote quote = quote("INFY", 17.10, "open", "INR", "2026-10-07T04:30:00Z");

        Client client = new Client("Buy INFY", "buy-infy@example.com");
        client.setPasswordHash("test-password-hash");
        client.addHolding(new Holding(new Asset("USD", "US DOLLAR", Instrument.CASH), new BigDecimal("100000"), BigDecimal.ONE));
        prepareForInsert(client);
        client = clientRepository.saveAndFlush(client);

        Order order = orderRepository.saveAndFlush(
                new Order(UUID.randomUUID(), client.getId(), new Asset("INFY", "INFY", Instrument.EQUITY), new BigDecimal("1"), true,
                        new BigDecimal("17.10")));

        assertEquals(BaseMarket.UNCAT, order.getBaseMarket());

        when(quoteService.getQuote("INFY")).thenReturn(quote, quote);
        OrderResponse response = orderProcessor.validate(order);

        assertEquals(Status.ACCEPTED, response.getStatusCode());
        assertEquals(BaseMarket.IN, order.getBaseMarket());

        entityManager.flush();
        entityManager.clear();

        Order persistedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals(BaseMarket.IN, persistedOrder.getBaseMarket());
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
    static class TestConfig {
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