package com.eatrading.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class PortfolioHistoryResponse {
    private UUID clientId;
    private String currency;
    private LocalDate from;
    private LocalDate to;
    private String interval;
    private List<PortfolioHistoryPointResponse> points;

    public PortfolioHistoryResponse() {
    }

    public PortfolioHistoryResponse(UUID clientId, String currency, LocalDate from, LocalDate to,
            String interval, List<PortfolioHistoryPointResponse> points) {
        this.clientId = clientId;
        this.currency = currency;
        this.from = from;
        this.to = to;
        this.interval = interval;
        this.points = points;
    }

    public UUID getClientId() {
        return clientId;
    }

    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(LocalDate to) {
        this.to = to;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public List<PortfolioHistoryPointResponse> getPoints() {
        return points;
    }

    public void setPoints(List<PortfolioHistoryPointResponse> points) {
        this.points = points;
    }
}
