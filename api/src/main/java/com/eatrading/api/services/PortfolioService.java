package com.eatrading.api.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eatrading.api.dto.PortfolioSummaryResponse;
import com.eatrading.api.entities.Client;
import com.eatrading.api.exception.ResourceNotFoundException;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Holding;
import com.eatrading.api.repository.ClientRepository;

@Service
public class PortfolioService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int SCALE = 8;

    private final ClientRepository clientRepository;
    private final PortfolioValuationService valuationService;

    public PortfolioService(ClientRepository clientRepository, PortfolioValuationService valuationService) {
        this.clientRepository = clientRepository;
        this.valuationService = valuationService;
    }

    @Transactional(readOnly = true)
    public PortfolioSummaryResponse getSummary(UUID clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

        BigDecimal availableCashUsd = BigDecimal.ZERO;
        BigDecimal portfolioValueUsd = BigDecimal.ZERO;
        BigDecimal investedValueUsd = BigDecimal.ZERO;
        BigDecimal dailyChangeUsd = BigDecimal.ZERO;

        for (Holding holding : client.getPortfolioHoldings()) {
            Asset asset = holding.getAsset();
            if (asset == null || holding.getQuantity() == null || holding.getAvgBuyPrice() == null) {
                continue;
            }

            PortfolioValuationService.Valuation valuation = valuationService.value(holding);
            portfolioValueUsd = portfolioValueUsd.add(valuation.getCurrentValueUsd());
            investedValueUsd = investedValueUsd.add(valuation.getPurchasedValueUsd());
            dailyChangeUsd = dailyChangeUsd.add(valuation.getDailyChangeUsd());

            if (valuationService.isUsdCash(asset)) {
                availableCashUsd = availableCashUsd.add(valuation.getCurrentValueUsd());
            }
        }

        BigDecimal overallReturnUsd = portfolioValueUsd.subtract(investedValueUsd);
        BigDecimal overallReturnPercent = percentage(overallReturnUsd, investedValueUsd);
        BigDecimal dailyChangePercent = percentage(dailyChangeUsd, portfolioValueUsd.subtract(dailyChangeUsd));

        return new PortfolioSummaryResponse(clientId, scale(availableCashUsd), scale(portfolioValueUsd),
                scale(investedValueUsd), scale(overallReturnUsd), scale(overallReturnPercent),
                scale(dailyChangeUsd), scale(dailyChangePercent), Instant.now());
    }

    private BigDecimal percentage(BigDecimal value, BigDecimal denominator) {
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.multiply(HUNDRED).divide(denominator, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}