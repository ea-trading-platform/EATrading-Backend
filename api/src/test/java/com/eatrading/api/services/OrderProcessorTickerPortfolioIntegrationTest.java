package com.eatrading.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.eatrading.api.dto.Quote;
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
@Import(OrderProcessorTickerPortfolioIntegrationTest.MockConfig.class)
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
class OrderProcessorTickerPortfolioIntegrationTest {

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

    @ParameterizedTest
    @MethodSource("tickerFixtures")
    @Commit
    void buyOrder_updatesPortfolioForTicker(String symbol, Instrument instrument, BigDecimal price) {
        Quote quote = quote(symbol, price.doubleValue());
        when(quoteService.getQuote(symbol)).thenReturn(quote, quote);

        Client client = new Client("Buy " + symbol, "buy-" + symbol.replace(":", "-") + "@example.com");
        client.addHolding(new Holding(new Asset("USD", "US DOLLAR", Instrument.CASH), new BigDecimal("100000"), BigDecimal.ONE));
        prepareForInsert(client);
        client = clientRepository.saveAndFlush(client);

        UUID clientId = client.getId();
        BigDecimal quantity = new BigDecimal("1");
        Asset asset = new Asset(symbol, symbol, instrument);

        Order order = orderRepository.saveAndFlush(
                new Order(UUID.randomUUID(), clientId, asset, quantity, true, price));

        OrderResponse validation = orderProcessor.validate(order);
        assertEquals(Status.ACCEPTED, validation.getStatusCode(), "Validation should be ACCEPTED for " + symbol);

        order.setStatus(validation.getStatusCode());
        order = orderRepository.saveAndFlush(order);

        OrderResponse execution = orderProcessor.executeOrder(order);
        assertEquals(Status.FILLED, execution.getStatusCode(), "Execution should be FILLED for " + symbol);

        order.setStatus(execution.getStatusCode());
        orderRepository.saveAndFlush(order);

        entityManager.flush();
        entityManager.clear();

        Client persisted = clientRepository.findById(clientId).orElseThrow();
        Holding purchasedHolding = persisted.getHolding(symbol);
        assertNotNull(purchasedHolding, "Holding should exist for " + symbol);
        assertBigDecimalEquals(quantity, purchasedHolding.getQuantity());

        BigDecimal expectedUsd = new BigDecimal("100000").subtract(price.multiply(quantity));
        assertBigDecimalEquals(expectedUsd, persisted.getUSDHolding().getQuantity());

        Order persistedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals(Status.FILLED, persistedOrder.getCurrentStatus());
    }

    @ParameterizedTest
    @MethodSource("tickerFixtures")
    @Commit
    void sellOrder_updatesPortfolioForTicker(String symbol, Instrument instrument, BigDecimal price) {
        Quote quote = quote(symbol, price.doubleValue());
        when(quoteService.getQuote(symbol)).thenReturn(quote, quote);

        Client client = new Client("Sell " + symbol, "sell-" + symbol.replace(":", "-") + "@example.com");
        client.addHolding(new Holding(new Asset("USD", "US DOLLAR", Instrument.CASH), new BigDecimal("1000"), BigDecimal.ONE));
        client.addHolding(new Holding(new Asset(symbol, symbol, instrument), new BigDecimal("3"), price));
        prepareForInsert(client);
        client = clientRepository.saveAndFlush(client);

        UUID clientId = client.getId();
        BigDecimal quantity = new BigDecimal("1");
        Asset asset = new Asset(symbol, symbol, instrument);

        Order order = orderRepository.saveAndFlush(
                new Order(UUID.randomUUID(), clientId, asset, quantity, false, price));

        OrderResponse validation = orderProcessor.validate(order);
        assertEquals(Status.ACCEPTED, validation.getStatusCode(), "Validation should be ACCEPTED for " + symbol);

        order.setStatus(validation.getStatusCode());
        order = orderRepository.saveAndFlush(order);

        OrderResponse execution = orderProcessor.executeOrder(order);
        assertEquals(Status.FILLED, execution.getStatusCode(), "Execution should be FILLED for " + symbol);

        order.setStatus(execution.getStatusCode());
        orderRepository.saveAndFlush(order);

        entityManager.flush();
        entityManager.clear();

        Client persisted = clientRepository.findById(clientId).orElseThrow();
        Holding remainingHolding = persisted.getHolding(symbol);
        assertNotNull(remainingHolding, "Holding should remain for " + symbol);
        assertBigDecimalEquals(new BigDecimal("2"), remainingHolding.getQuantity());

        BigDecimal expectedUsd = new BigDecimal("1000").add(price.multiply(quantity));
        assertBigDecimalEquals(expectedUsd, persisted.getUSDHolding().getQuantity());

        Order persistedOrder = orderRepository.findById(order.getOrderId()).orElseThrow();
        assertEquals(Status.FILLED, persistedOrder.getCurrentStatus());
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> tickerFixtures() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("INFY", Instrument.EQUITY, new BigDecimal("17.10")),
                org.junit.jupiter.params.provider.Arguments.of("LT", Instrument.EQUITY, new BigDecimal("39.25")),
                org.junit.jupiter.params.provider.Arguments.of("ITC", Instrument.EQUITY, new BigDecimal("5.80")),
                org.junit.jupiter.params.provider.Arguments.of("X:BTC-USD", Instrument.CRYPTO, new BigDecimal("68123.45")),
                org.junit.jupiter.params.provider.Arguments.of("X:ETH-USD", Instrument.CRYPTO, new BigDecimal("3521.77"))
        );
    }

    private Quote quote(String symbol, double price) {
        Quote quote = new Quote();
        quote.setSymbol(symbol);
        quote.setPrice(price);
        quote.setBid(price - 0.01);
        quote.setAsk(price + 0.01);
        quote.setMarketState("open");
        quote.setCurrency("USD");
        quote.setAsOf("2026-10-08T18:00:00Z");
        return quote;
    }

    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual), () -> "Expected " + expected + " but was " + actual);
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
