package com.eatrading.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eatrading.api.dto.PortfolioHistoryResponse;
import com.eatrading.api.entities.PortfolioSnapshot;
import com.eatrading.api.repository.PortfolioSnapshotRepository;

@ExtendWith(MockitoExtension.class)
class PortfolioHistoryServiceTest {

    @Mock
    private PortfolioSnapshotRepository snapshotRepository;

    private PortfolioHistoryService historyService;

    @BeforeEach
    void setUp() {
        historyService = new PortfolioHistoryService(snapshotRepository);
    }

    @Test
    void getHistoryReturnsSnapshotPointsAndMetadata() {
        UUID clientId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 2);
        PortfolioSnapshot earlierSnapshot = new PortfolioSnapshot(clientId,
                Instant.parse("2026-01-01T20:00:00Z"),
                new BigDecimal("10000.00"), new BigDecimal("1000.00"),
                new BigDecimal("9000.00"), new BigDecimal("100.00"));
        PortfolioSnapshot snapshot = new PortfolioSnapshot(clientId,
                Instant.parse("2026-01-02T20:00:00Z"),
                new BigDecimal("10350.25"), new BigDecimal("1250.00"),
                new BigDecimal("9100.25"), new BigDecimal("125.40"));
        when(snapshotRepository.findByClientIdAndTimestampBetweenOrderByTimestampAsc(
                clientId,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T23:59:59.999999999Z")))
                .thenReturn(List.of(earlierSnapshot, snapshot));

        PortfolioHistoryResponse response = historyService.getHistory(clientId, from, to);

        assertEquals(clientId, response.getClientId());
        assertEquals("1d", response.getInterval());
        assertEquals(from, response.getFrom());
        assertEquals(to, response.getTo());
        assertEquals(2, response.getPoints().size());
        assertEquals(new BigDecimal("10000.00"), response.getPoints().get(0).getPortfolioValueUsd());
        assertEquals(new BigDecimal("10350.25"), response.getPoints().get(1).getPortfolioValueUsd());
        verify(snapshotRepository).findByClientIdAndTimestampBetweenOrderByTimestampAsc(
                clientId,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T23:59:59.999999999Z"));
    }

    @Test
    void getHistoryReturnsEmptyPointsWhenNoSnapshotsExist() {
        UUID clientId = UUID.randomUUID();
        when(snapshotRepository.findByClientIdAndTimestampBetweenOrderByTimestampAsc(
                clientId,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T23:59:59.999999999Z")))
                .thenReturn(List.of());

        PortfolioHistoryResponse response = historyService.getHistory(clientId,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));

        assertEquals(List.of(), response.getPoints());
    }

    @Test
    void getHistoryRejectsReversedDateRange() {
        assertThrows(IllegalArgumentException.class, () -> historyService.getHistory(
                UUID.randomUUID(), LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 1)));
    }
}
