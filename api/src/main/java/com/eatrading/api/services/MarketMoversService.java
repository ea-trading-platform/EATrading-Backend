package com.eatrading.api.services;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.eatrading.api.dto.Candle;
import com.eatrading.api.dto.CandlesResponse;
import com.eatrading.api.dto.MarketMoversResponse;
import com.eatrading.api.dto.MarketMoversResponse.StockPerformance;

@Service
public class MarketMoversService {
    private static final Logger logger = LoggerFactory.getLogger(MarketMoversService.class);
    private static final List<String> POPULAR_STOCKS = List.of(
            "AAPL", "MSFT", "NVDA", "AMZN", "GOOGL", "META", "TSLA", "AVGO", "AMD", "NFLX",
            "JPM", "V", "MA", "BRK-B", "WMT", "COST", "HD", "PG", "KO", "PEP",
            "UNH", "JNJ", "XOM", "DIS", "INTC");

    private final FauxnanceClient fauxnanceClient;

    public MarketMoversService(FauxnanceClient fauxnanceClient) {
        this.fauxnanceClient = fauxnanceClient;
    }

    public MarketMoversResponse getMovers() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String from = today.minusDays(30).toString();
        String to = today.toString();
        List<StockPerformance> stocks = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();
        for (String symbol : POPULAR_STOCKS) {
            try {
                stocks.add(performance(symbol, fauxnanceClient.getCandles(symbol, from, to)));
            } catch (RuntimeException exception) {
                logger.warn("Cannot calculate daily return for {}: {}", symbol, exception.getMessage());
                unavailable.add(symbol);
            }
        }
        if (stocks.isEmpty()) {
            throw new IllegalStateException("No candle data available for the stock watchlist");
        }
        Comparator<StockPerformance> descending = Comparator
                .comparingDouble(StockPerformance::changePercent).reversed()
                .thenComparing(StockPerformance::symbol);
        Comparator<StockPerformance> ascending = Comparator
                .comparingDouble(StockPerformance::changePercent)
                .thenComparing(StockPerformance::symbol);
        List<StockPerformance> gainers = stocks.stream()
                .filter(stock -> stock.changePercent() > 0)
                .sorted(descending).limit(5).toList();
        List<StockPerformance> losers = stocks.stream()
                .filter(stock -> stock.changePercent() < 0)
                .sorted(ascending).limit(5).toList();
        return new MarketMoversResponse(POPULAR_STOCKS, List.copyOf(stocks), gainers, losers,
                List.copyOf(unavailable));
    }

    private StockPerformance performance(String symbol, CandlesResponse response) {
        if (response == null || response.getData() == null || response.getData().getCandles() == null) {
            throw new IllegalStateException("Missing candles");
        }
        List<Candle> candles = response.getData().getCandles();
        if (candles.size() < 2) {
            throw new IllegalStateException("At least two daily candles are required");
        }
        Candle previous = candles.get(candles.size() - 2);
        Candle latest = candles.get(candles.size() - 1);
        if (previous == null || latest == null
                || previous.getDate() == null || latest.getDate() == null
                || !LocalDate.parse(previous.getDate()).isBefore(LocalDate.parse(latest.getDate()))
                || !Double.isFinite(previous.getAdjclose()) || previous.getAdjclose() <= 0
                || !Double.isFinite(latest.getAdjclose()) || latest.getAdjclose() <= 0
                || !Double.isFinite(latest.getClose()) || latest.getClose() <= 0) {
            throw new IllegalStateException("Invalid daily candles");
        }
        double changePercent = (latest.getAdjclose() / previous.getAdjclose() - 1) * 100;
        if (!Double.isFinite(changePercent)) {
            throw new IllegalStateException("Invalid daily return");
        }
        CandlesResponse.CandleMetadata meta = response.getMeta();
        return new StockPerformance(symbol, latest.getDate(), previous.getDate(),
                response.getData().getCurrency(), latest.getClose(), latest.getAdjclose(),
                previous.getAdjclose(), changePercent, previous.isSynthetic() || latest.isSynthetic(),
                meta == null ? null : meta.getStale(), meta == null ? null : meta.getPartial());
    }
}