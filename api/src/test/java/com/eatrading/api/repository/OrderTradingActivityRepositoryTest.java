package com.eatrading.api.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import com.eatrading.api.entities.Order;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.objects.Status;
import com.eatrading.api.objects.TradeInterval;
import com.eatrading.api.services.OrderService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

@SpringJUnitConfig(OrderTradingActivityRepositoryTest.Config.class)
@Transactional
class OrderTradingActivityRepositoryTest {
    @Autowired
    private OrderRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void countsOnlySelectedClientsFilledOrdersByCompletionTimeAndHalfOpenBounds() {
        UUID clientId = UUID.randomUUID();
        Instant from = Instant.parse("2026-10-16T00:00:00Z");
        Instant to = Instant.parse("2026-10-19T00:00:00Z");
        Instant secondDay = Instant.parse("2026-10-17T12:00:00Z");

        save(clientId, true, Status.FILLED, from);
        save(clientId, false, Status.FILLED, from);
        save(clientId, true, Status.FILLED, secondDay);
        save(clientId, true, Status.FILLED, from.minusNanos(1000));
        save(clientId, true, Status.FILLED, to);
        save(UUID.randomUUID(), true, Status.FILLED, from);
        for (Status status : List.of(Status.SUBMITTED, Status.ACCEPTED, Status.CANCELED, Status.REJECTED)) {
            save(clientId, true, status, from);
        }
        save(clientId, true, Status.FILLED, null);
        entityManager.flush();
        entityManager.clear();

        var completions = repository.findCompletionTimes(clientId, Status.FILLED, Status.FILLED, from, to);
        assertEquals(List.of(from, from, secondDay), completions.stream().sorted().toList());

        var response = new OrderService(repository).getTradingActivity(clientId, from, to, TradeInterval.DAY);
        assertEquals(List.of(2L, 1L, 0L),
                response.getPoints().stream().map(point -> point.getTradeCount()).toList());
    }

    private void save(UUID clientId, boolean buy, Status status, Instant completedAt) {
        Order order = new Order(UUID.randomUUID(), clientId,
                new Asset("TEST", "Test asset", Instrument.EQUITY),
                buy ? new BigDecimal("100") : new BigDecimal("5"), buy, BigDecimal.TEN);
        order.setStatus(status);
        order.getStatusChangeLog().remove(Status.FILLED);
        if (completedAt != null) {
            order.getStatusChangeLog().put(Status.FILLED, completedAt);
        }
        repository.save(order);
    }

    @Configuration
    @EnableJpaRepositories(basePackageClasses = OrderRepository.class)
    @EnableTransactionManagement
    static class Config {
        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().generateUniqueName(true)
                    .setType(EmbeddedDatabaseType.H2).build();
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.eatrading.api.entities");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }

        @Bean
        JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory);
        }
    }
}
