package com.eatrading.api.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.eatrading.api.dto.MarketMoversResponse;
import com.eatrading.api.dto.MarketMoversResponse.StockPerformance;
import com.eatrading.api.services.FauxnanceClient;
import com.eatrading.api.services.MarketMoversService;
import com.eatrading.api.services.QuoteService;

class MarketMoversControllerTest {
    private MarketMoversService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(MarketMoversService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MarketDataController(
                mock(QuoteService.class), mock(FauxnanceClient.class), service)).build();
    }

    @Test
    void returnsWatchlistAndRankingsAsJson() throws Exception {
        StockPerformance stock = new StockPerformance("AAPL", "2026-10-22", "2026-10-21",
                "USD", 110, 110, 100, 10, false, false, false);
        when(service.getMovers()).thenReturn(new MarketMoversResponse(
                List.of("AAPL", "MSFT"), List.of(stock), List.of(stock), List.of(), List.of("MSFT")));

        mockMvc.perform(get("/api/v1/market/movers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbols[0]").value("AAPL"))
                .andExpect(jsonPath("$.stocks[0].adjustedClose").value(110))
                .andExpect(jsonPath("$.gainers[0].changePercent").value(10))
                .andExpect(jsonPath("$.losers").isEmpty())
                .andExpect(jsonPath("$.unavailableSymbols[0]").value("MSFT"));
    }

    @Test
    void returnsBadGatewayWhenNoStockDataIsAvailable() throws Exception {
        when(service.getMovers()).thenThrow(new IllegalStateException("No candle data available"));

        mockMvc.perform(get("/api/v1/market/movers"))
                .andExpect(status().isBadGateway());
    }
}