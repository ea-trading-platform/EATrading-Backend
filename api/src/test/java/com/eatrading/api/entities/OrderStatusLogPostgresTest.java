package com.eatrading.api.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.eatrading.api.dto.OrderTransactionRequest;
import com.eatrading.api.messaging.Consumers;
import com.eatrading.api.messaging.Producers;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.BaseMarket;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.objects.OrderResponse;
import com.eatrading.api.objects.Status;
import com.eatrading.api.services.OrderProcessor;
import com.eatrading.api.services.OrderService;
import com.eatrading.api.services.QuoteService;

@EnabledIfEnvironmentVariable(named = "ORDER_TEST_POSTGRES_URL", matches = ".+")
class OrderStatusLogPostgresTest {

    @ParameterizedTest(name = "executionRejection={0}")
    @ValueSource(booleans = {false, true})
    void persistsRejectedOrderWithMessageForInspection(boolean executionRejection) throws Exception {
        String url = requiredEnvironment("ORDER_TEST_POSTGRES_URL");
        String username = requiredEnvironment("ORDER_TEST_POSTGRES_USER");
        String password = requiredEnvironment("ORDER_TEST_POSTGRES_PASSWORD");
        String reason = executionRejection
                ? "Invalid execution price - Docker PostgreSQL persistence test"
                : "Insufficient USD cash - Docker PostgreSQL persistence test";
        Order order = new Order(UUID.randomUUID(), UUID.randomUUID(),
                new Asset("AAPL", "REJECTION PERSISTENCE TEST", Instrument.EQUITY),
                BigDecimal.ONE, true, BigDecimal.TEN);
        order.setBaseMarket(BaseMarket.US);

        try (var factory = new Configuration()
                .addAnnotatedClass(Order.class)
                .setProperty("hibernate.connection.driver_class", "org.postgresql.Driver")
                .setProperty("hibernate.connection.url", url)
                .setProperty("hibernate.connection.username", username)
                .setProperty("hibernate.connection.password", password)
                .setProperty("hibernate.hbm2ddl.auto", "validate")
                .buildSessionFactory()) {
            factory.inTransaction(session -> {
                session.persist(order);
                if (executionRejection) {
                    order.setStatus(Status.ACCEPTED);
                }
                OrderProcessor processor = mock(OrderProcessor.class);
                OrderService service = mock(OrderService.class);
                Producers producers = mock(Producers.class);
                OrderResponse rejected = new OrderResponse();
                rejected.setStatusCode(Status.REJECTED);
                rejected.setRejectionReason(reason);
                when(service.getOrderByTrackingId(order.getTrackingId())).thenReturn(order);
                OrderTransactionRequest request = new OrderTransactionRequest();
                request.setTrackingId(order.getTrackingId());
                Consumers consumers = new Consumers(processor, producers, mock(QuoteService.class), service);
                if (executionRejection) {
                    when(processor.executeOrder(order)).thenReturn(rejected);
                    consumers.consumeExecute(request);
                } else {
                    when(processor.validate(order)).thenReturn(rejected);
                    consumers.consumeValidate(request);
                }
                verifyNoInteractions(producers);
            });

            factory.inTransaction(session -> {
                Order reloaded = session.find(Order.class, order.getOrderId());
                assertEquals(Status.REJECTED, reloaded.getCurrentStatus());
                assertEquals(BaseMarket.US, reloaded.getBaseMarket());
                assertEquals(reason, reloaded.getStatusLogEntries().get(Status.REJECTED).getRejectionReason());
                assertEquals(reloaded.getOrderDate(), reloaded.getStatusChangeLog().get(Status.SUBMITTED));
                assertNull(reloaded.getStatusLogEntries().get(Status.SUBMITTED).getRejectionReason());
                assertEquals(executionRejection ? 3 : 2, reloaded.getStatusChangeLog().size());
            });
        }

        try (var connection = DriverManager.getConnection(url, username, password);
                var statement = connection.prepareStatement("""
                        SELECT log.rejection_reason, orders.base_market
                        FROM order_status_log log
                        JOIN orders ON orders.order_id = log.order_id AND orders.tracking_id = log.tracking_id
                        WHERE log.order_id = ? AND log.tracking_id = ? AND log.status = 'REJECTED'
                        """)) {
            statement.setObject(1, order.getOrderId());
            statement.setObject(2, order.getTrackingId());
            try (var rows = statement.executeQuery()) {
                assertTrue(rows.next());
                assertEquals(reason, rows.getString("rejection_reason"));
                assertEquals(1, rows.getInt("base_market"));
            }
        }

        System.out.printf("Verified rejected test order: orderId=%s, trackingId=%s, reason=%s%n",
                order.getOrderId(), order.getTrackingId(), reason);
    }

    private String requiredEnvironment(String name) {
        return Objects.requireNonNull(System.getenv(name), "Missing required environment variable: " + name);
    }
}
