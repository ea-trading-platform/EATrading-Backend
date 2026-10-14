package com.eatrading.api.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "portfolio_snapshot", indexes = {
    @Index(name = "idx_portfolio_snapshot_client_timestamp", columnList = "client_id,snapshot_timestamp")
})
public class PortfolioSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "snapshot_timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "portfolio_value_usd", precision = 19, scale = 8)
    private BigDecimal portfolioValueUsd;

    @Column(name = "cash_value_usd", precision = 19, scale = 8)
    private BigDecimal cashValueUsd;

    @Column(name = "invested_value_usd", precision = 19, scale = 8)
    private BigDecimal investedValueUsd;

    @Column(name = "daily_change_usd", precision = 19, scale = 8)
    private BigDecimal dailyChangeUsd;

    protected PortfolioSnapshot() {
        // Default constructor for JPA
    }

    public PortfolioSnapshot(UUID clientId, Instant timestamp, BigDecimal portfolioValueUsd,
            BigDecimal cashValueUsd, BigDecimal investedValueUsd, BigDecimal dailyChangeUsd) {
        this.clientId = clientId;
        this.timestamp = timestamp;
        this.portfolioValueUsd = portfolioValueUsd;
        this.cashValueUsd = cashValueUsd;
        this.investedValueUsd = investedValueUsd;
        this.dailyChangeUsd = dailyChangeUsd;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
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
