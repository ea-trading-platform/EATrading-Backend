package com.eatrading.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PortfolioSummaryResponse {
    private UUID clientId;
    private BigDecimal availableCashUsd;
    private BigDecimal portfolioValueUsd;
    private BigDecimal investedValueUsd;
    private BigDecimal overallReturnUsd;
    private BigDecimal overallReturnPercent;
    private BigDecimal dailyChangeUsd;
    private BigDecimal dailyChangePercent;
    private Instant asOf;

    public PortfolioSummaryResponse(UUID clientId, BigDecimal availableCashUsd,
            BigDecimal portfolioValueUsd, BigDecimal investedValueUsd,
            BigDecimal overallReturnUsd, BigDecimal overallReturnPercent,
            BigDecimal dailyChangeUsd, BigDecimal dailyChangePercent, Instant asOf) {
        this.clientId = clientId;
        this.availableCashUsd = availableCashUsd;
        this.portfolioValueUsd = portfolioValueUsd;
        this.investedValueUsd = investedValueUsd;
        this.overallReturnUsd = overallReturnUsd;
        this.overallReturnPercent = overallReturnPercent;
        this.dailyChangeUsd = dailyChangeUsd;
        this.dailyChangePercent = dailyChangePercent;
        this.asOf = asOf;
    }

    public UUID getClientId() {
        return clientId;
    }

    public BigDecimal getAvailableCashUsd() {
        return availableCashUsd;
    }

    public BigDecimal getPortfolioValueUsd() {
        return portfolioValueUsd;
    }

    public BigDecimal getInvestedValueUsd() {
        return investedValueUsd;
    }

    public BigDecimal getOverallReturnUsd() {
        return overallReturnUsd;
    }

    public BigDecimal getOverallReturnPercent() {
        return overallReturnPercent;
    }

    public BigDecimal getDailyChangeUsd() {
        return dailyChangeUsd;
    }

    public BigDecimal getDailyChangePercent() {
        return dailyChangePercent;
    }

    public Instant getAsOf() {
        return asOf;
    }
}