package com.eatrading.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class PortfolioHistoryPointResponse {
    private Instant timestamp;
    private BigDecimal portfolioValueUsd;
    private BigDecimal cashValueUsd;
    private BigDecimal investedValueUsd;
    private BigDecimal dailyChangeUsd;

    public PortfolioHistoryPointResponse() {
    }

    public PortfolioHistoryPointResponse(Instant timestamp, BigDecimal portfolioValueUsd,
            BigDecimal cashValueUsd, BigDecimal investedValueUsd, BigDecimal dailyChangeUsd) {
        this.timestamp = timestamp;
        this.portfolioValueUsd = portfolioValueUsd;
        this.cashValueUsd = cashValueUsd;
        this.investedValueUsd = investedValueUsd;
        this.dailyChangeUsd = dailyChangeUsd;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public BigDecimal getPortfolioValueUsd() {
        return portfolioValueUsd;
    }

    public void setPortfolioValueUsd(BigDecimal portfolioValueUsd) {
        this.portfolioValueUsd = portfolioValueUsd;
    }

    public BigDecimal getCashValueUsd() {
        return cashValueUsd;
    }

    public void setCashValueUsd(BigDecimal cashValueUsd) {
        this.cashValueUsd = cashValueUsd;
    }

    public BigDecimal getInvestedValueUsd() {
        return investedValueUsd;
    }

    public void setInvestedValueUsd(BigDecimal investedValueUsd) {
        this.investedValueUsd = investedValueUsd;
    }

    public BigDecimal getDailyChangeUsd() {
        return dailyChangeUsd;
    }

    public void setDailyChangeUsd(BigDecimal dailyChangeUsd) {
        this.dailyChangeUsd = dailyChangeUsd;
    }
}
