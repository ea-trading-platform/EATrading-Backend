package com.eatrading.api.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import com.eatrading.api.entities.Client;
import com.eatrading.api.entities.PortfolioSnapshot;
import com.eatrading.api.objects.Holding;
import com.eatrading.api.repository.ClientRepository;
import com.eatrading.api.repository.PortfolioSnapshotRepository;

@Service
public class PortfolioSnapshotService {
    private static final Logger logger = LoggerFactory.getLogger(PortfolioSnapshotService.class);
    private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");
    private static final LocalTime MARKET_CLOSE = LocalTime.of(16, 0);
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final ClientRepository clientRepository;
    private final PortfolioSnapshotRepository snapshotRepository;
    private final PortfolioValuationService valuationService;

    public PortfolioSnapshotService(ClientRepository clientRepository,
            PortfolioSnapshotRepository snapshotRepository,
            PortfolioValuationService valuationService) {
        this.clientRepository = clientRepository;
        this.snapshotRepository = snapshotRepository;
        this.valuationService = valuationService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createTradeSnapshot(UUID clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalStateException("Client not found: " + clientId));
        createSnapshot(client, Instant.now());
    }

    @Scheduled(cron = "0 5 16 * * MON-FRI", zone = "America/New_York")
    @Transactional
    public void createEndOfDaySnapshots() {
        Instant snapshotTimestamp = marketCloseTimestamp(LocalDate.now(MARKET_ZONE));
        logger.info("Starting end-of-day portfolio snapshots at {}", snapshotTimestamp);

        for (Client client : clientRepository.findAll()) {
            try {
                createSnapshot(client, snapshotTimestamp);
            } catch (Exception exception) {
                logger.error("Failed to create portfolio snapshot for client {}: {}",
                        client.getId(), exception.getMessage(), exception);
            }
        }

        logger.info("Completed end-of-day portfolio snapshots at {}", snapshotTimestamp);
    }

    private void createSnapshot(Client client, Instant snapshotTimestamp) {
        UUID clientId = client.getId();
        if (snapshotRepository.existsByClientIdAndTimestamp(clientId, snapshotTimestamp)) {
            logger.debug("Portfolio snapshot already exists for client {} at {}", clientId, snapshotTimestamp);
            return;
        }

        BigDecimal portfolioValueUsd = ZERO;
        BigDecimal cashValueUsd = ZERO;
        BigDecimal investedValueUsd = ZERO;
        BigDecimal dailyChangeUsd = ZERO;

        for (Holding holding : client.getPortfolioHoldings()) {
            PortfolioValuationService.Valuation valuation = valuationService.value(holding);
            portfolioValueUsd = portfolioValueUsd.add(valuation.getCurrentValueUsd());
            investedValueUsd = investedValueUsd.add(valuation.getPurchasedValueUsd());
            dailyChangeUsd = dailyChangeUsd.add(valuation.getDailyChangeUsd());

            if (valuationService.isUsdCash(holding.getAsset())) {
                cashValueUsd = cashValueUsd.add(valuation.getCurrentValueUsd());
            }
        }

        PortfolioSnapshot snapshot = new PortfolioSnapshot(clientId, snapshotTimestamp,
                portfolioValueUsd, cashValueUsd, investedValueUsd, dailyChangeUsd);
        snapshotRepository.save(snapshot);
        logger.debug("Created portfolio snapshot for client {} at {}", clientId, snapshotTimestamp);
    }

    private Instant marketCloseTimestamp(LocalDate date) {
        return LocalDateTime.of(date, MARKET_CLOSE).atZone(MARKET_ZONE).toInstant();
    }
}
