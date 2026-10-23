package com.eatrading.api.dto;

import java.util.List;

public record MarketMoversResponse(
        List<String> symbols,
        List<StockPerformance> stocks,
        List<StockPerformance> gainers,
        List<StockPerformance> losers,
        List<String> unavailableSymbols) {

    public record StockPerformance(
            String symbol,
            String date,
            String previousDate,
            String currency,
            double close,
            double adjustedClose,
            double previousAdjustedClose,
            double changePercent,
            boolean synthetic,
            Boolean stale,
            Boolean partial) {
    }
}