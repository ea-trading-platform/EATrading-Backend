package com.eatrading.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eatrading.api.dto.Candle;
import com.eatrading.api.dto.CandlesResponse;
import com.eatrading.api.dto.MarketMoversResponse;
import com.eatrading.api.dto.MarketMoversResponse.StockPerformance;

@ExtendWith(MockitoExtension.class)
class MarketMoversServiceTest {
    @Mock
    private FauxnanceClient fauxnanceClient;

    private MarketMoversService service;

    @BeforeEach
    void setUp() {
        service = new MarketMoversService(fauxnanceClient);
    }

    @Test
    void ranksFiveGainersAndLosersAcross25UniqueStocksUsingAdjustedClose() {
        AtomicInteger index = new AtomicInteger();
        when(fauxnanceClient.getCandles(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> candles(invocation.getArgument(0), 100, 88 + index.getAndIncrement()));

        MarketMoversResponse result = service.getMovers();

        assertEquals(25, result.symbols().stream().distinct().count());
        assertEquals(25, result.stocks().size());
        assertEquals(0, result.unavailableSymbols().size());
        assertEquals(List.of("INTC", "DIS", "XOM", "JNJ", "UNH"),
                result.gainers().stream().map(StockPerformance::symbol).toList());
        assertEquals(List.of("AAPL", "MSFT", "NVDA", "AMZN", "GOOGL"),
                result.losers().stream().map(StockPerformance::symbol).toList());
        assertEquals(12, result.gainers().get(0).changePercent(), 0.000001);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (String symbol : result.symbols()) {
            verify(fauxnanceClient).getCandles(symbol, today.minusDays(30).toString(), today.toString());
        }
    }

    @Test
    void reportsFailuresAndInsufficientOrInvalidDataWithoutDiscardingOtherStocks() {
        when(fauxnanceClient.getCandles(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            String symbol = invocation.getArgument(0);
            return switch (symbol) {
                case "AAPL" -> throw new IllegalStateException("API unavailable");
                case "MSFT" -> null;
                case "NVDA" -> new CandlesResponse();
                case "AMZN" -> new CandlesResponse(new CandlesResponse.CandleData(
                        symbol, "1d", "USD", List.of()), null);
                case "GOOGL" -> candles(symbol, 0, 100);
                case "META" -> candles(symbol, 100, Double.NaN);
                default -> candles(symbol, 100, 110);
            };
        });

        MarketMoversResponse result = service.getMovers();

        assertEquals(List.of("AAPL", "MSFT", "NVDA", "AMZN", "GOOGL", "META"), result.unavailableSymbols());
        assertEquals(19, result.stocks().size());
        assertEquals(5, result.gainers().size());
        assertTrue(result.losers().isEmpty());
        assertEquals("AMD", result.gainers().get(0).symbol());
    }

    @Test
    void exposesSyntheticAndFreshnessFlagsAndExcludesUnchangedStocksFromRankings() {
        when(fauxnanceClient.getCandles(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            CandlesResponse response = candles(invocation.getArgument(0), 100, 100);
            response.getData().getCandles().get(0).setSynthetic(true);
            CandlesResponse.CandleMetadata meta = new CandlesResponse.CandleMetadata();
            meta.setStale(true);
            meta.setPartial(true);
            response.setMeta(meta);
            return response;
        });

        MarketMoversResponse result = service.getMovers();

        assertTrue(result.gainers().isEmpty());
        assertTrue(result.losers().isEmpty());
        assertTrue(result.stocks().stream().allMatch(stock -> stock.synthetic() && stock.stale() && stock.partial()));
        assertEquals("2026-10-22", result.stocks().get(0).date());
        assertEquals("2026-10-21", result.stocks().get(0).previousDate());
    }

    @Test
    void failsWhenNoStockHasUsableData() {
        when(fauxnanceClient.getCandles(anyString(), anyString(), anyString())).thenReturn(null);

        assertThrows(IllegalStateException.class, service::getMovers);
    }

    private CandlesResponse candles(String symbol, double previous, double latest) {
        return new CandlesResponse(new CandlesResponse.CandleData(symbol, "1d", "USD", List.of(
                new Candle("2026-10-21", 200, 200, 200, 200, previous, 1000L, false),
                new Candle("2026-10-22", 50, 50, 50, 50, latest, 1000L, false))), null);
    }
}