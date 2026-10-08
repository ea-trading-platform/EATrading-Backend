package com.eatrading.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.eatrading.api.dto.Quote;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTickerTest {

    @Mock
    private FauxnanceClient fauxnanceClient;

    private QuoteService quoteService;

    private static final Map<String, Double> PRICE_BY_TICKER = Map.of(
            "INFY", 17.10,
            "LT", 39.25,
            "ITC", 5.80,
            "X:BTC-USD", 68123.45,
            "X:ETH-USD", 3521.77
    );

    @BeforeEach
    void setUp() {
        quoteService = new QuoteService(fauxnanceClient);
    }

    @ParameterizedTest
    @ValueSource(strings = { "INFY", "LT", "ITC" })
    void getCurrentPrice_returnsPrice_forFxAndEquityTickerSet(String ticker) {
        Quote quote = quote(ticker, PRICE_BY_TICKER.get(ticker));
        when(fauxnanceClient.getQuote(ticker)).thenReturn(quote);

        double price = quoteService.getCurrentPrice(ticker);

        assertTrue(price > 0.0);
        assertEquals(PRICE_BY_TICKER.get(ticker), price, 0.000001);
        verify(fauxnanceClient, times(1)).getQuote(ticker);
    }

    @ParameterizedTest
    @ValueSource(strings = { "X:BTC-USD", "X:ETH-USD" })
    void getCurrentPrice_returnsPrice_forCryptoTickerSet(String ticker) {
        Quote quote = quote(ticker, PRICE_BY_TICKER.get(ticker));
        when(fauxnanceClient.getQuote(ticker)).thenReturn(quote);

        double price = quoteService.getCurrentPrice(ticker);

        assertTrue(price > 0.0);
        assertEquals(PRICE_BY_TICKER.get(ticker), price, 0.000001);
        verify(fauxnanceClient, times(1)).getQuote(ticker);
    }

    private Quote quote(String symbol, double price) {
        Quote quote = new Quote();
        quote.setSymbol(symbol);
        quote.setPrice(price);
        quote.setBid(price - 0.01);
        quote.setAsk(price + 0.01);
        quote.setAsOf("2026-10-08T18:00:00Z");
        return quote;
    }
}
