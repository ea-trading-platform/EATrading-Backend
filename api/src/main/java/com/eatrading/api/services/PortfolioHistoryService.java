package com.eatrading.api.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eatrading.api.dto.PortfolioHistoryPointResponse;
import com.eatrading.api.dto.PortfolioHistoryResponse;
import com.eatrading.api.entities.PortfolioSnapshot;
import com.eatrading.api.repository.PortfolioSnapshotRepository;

@Service
public class PortfolioHistoryService {
    private static final String DAILY_INTERVAL = "1d";
    private static final ZoneOffset UTC = ZoneOffset.UTC;

    private final PortfolioSnapshotRepository snapshotRepository;

    public PortfolioHistoryService(PortfolioSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional(readOnly = true)
    public PortfolioHistoryResponse getHistory(UUID clientId, LocalDate from, LocalDate to) {
        validateDateRange(from, to);

        Instant fromInstant = from.atStartOfDay(UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(UTC).toInstant().minusNanos(1);

        List<PortfolioHistoryPointResponse> points = snapshotRepository
                .findByClientIdAndTimestampBetweenOrderByTimestampAsc(clientId, fromInstant, toInstant)
                .stream()
                .map(this::toPoint)
                .toList();

        return new PortfolioHistoryResponse(clientId, from, to, DAILY_INTERVAL, points);
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from and to dates are required");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from date must not be after to date");
        }
    }

    private PortfolioHistoryPointResponse toPoint(PortfolioSnapshot snapshot) {
        return new PortfolioHistoryPointResponse(
                snapshot.getTimestamp(),
                snapshot.getPortfolioValueUsd(),
                snapshot.getCashValueUsd(),
                snapshot.getInvestedValueUsd(),
                snapshot.getDailyChangeUsd());
    }
}
