package com.eatrading.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eatrading.api.dto.PortfolioSummaryResponse;
import com.eatrading.api.dto.Quote;
import com.eatrading.api.entities.Client;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Holding;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.repository.ClientRepository;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private QuoteService quoteService;

    private PortfolioService portfolioService;

    @BeforeEach
    void setUp() {
        portfolioService = new PortfolioService(clientRepository, new PortfolioValuationService(quoteService));
    }

    @Test
    void getSummaryValuesUsdEquityAndCash() {
        UUID clientId = UUID.randomUUID();
        Client client = new Client("Test Client", "test@example.com");
        client.addHolding(new Holding(new Asset("USD", "US Dollar", Instrument.CASH),
                new BigDecimal("1000"), BigDecimal.ONE));
        client.addHolding(new Holding(new Asset("AAPL", "Apple", Instrument.EQUITY),
                new BigDecimal("2"), new BigDecimal("100")));

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(quoteService.getQuote("AAPL")).thenReturn(quote("AAPL", "USD", 120, 5));

        PortfolioSummaryResponse response = portfolioService.getSummary(clientId);

        assertEquals(new BigDecimal("1000.00"), response.getAvailableCashUsd());
        assertEquals(new BigDecimal("1240.00"), response.getPortfolioValueUsd());
        assertEquals(new BigDecimal("1200.00"), response.getInvestedValueUsd());
        assertEquals(new BigDecimal("40.00"), response.getOverallReturnUsd());
        assertEquals(new BigDecimal("10.00"), response.getDailyChangeUsd());
    }

    @Test
    void getSummaryConvertsForeignCashAndEquityToUsd() {
        UUID clientId = UUID.randomUUID();
        Client client = new Client("Test Client", "test@example.com");
        client.addHolding(new Holding(new Asset("INR", "Indian Rupee", Instrument.CASH),
                new BigDecimal("1000"), BigDecimal.ONE));
        client.addHolding(new Holding(new Asset("INFY.NS", "Infosys", Instrument.EQUITY),
                new BigDecimal("10"), new BigDecimal("80")));

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(quoteService.getQuote("INFY.NS")).thenReturn(quote("INFY.NS", "INR", 100, 2));
        when(quoteService.getQuote("FX:INRUSD")).thenReturn(quote("FX:INRUSD", "USD", 0.012, 0));

        PortfolioSummaryResponse response = portfolioService.getSummary(clientId);

        assertEquals(new BigDecimal("0.00"), response.getAvailableCashUsd());
        assertEquals(new BigDecimal("24.00"), response.getPortfolioValueUsd());
        assertEquals(new BigDecimal("1800.00"), response.getInvestedValueUsd());
        assertEquals(new BigDecimal("-1776.00"), response.getOverallReturnUsd());
        assertEquals(new BigDecimal("0.24"), response.getDailyChangeUsd());
    }

    @Test
    void getSummaryDerivesDailyChangeWhenQuoteOnlyHasPreviousClose() {
        UUID clientId = UUID.randomUUID();
        Client client = new Client("Test Client", "test-previous-close@example.com");
        client.addHolding(new Holding(new Asset("AAPL", "Apple", Instrument.EQUITY),
                new BigDecimal("2"), new BigDecimal("100")));

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        Quote quote = quote("AAPL", "USD", 120, 0);
        quote.setChange(null);
        quote.setChangePercent(null);
        quote.setPreviousClose(115.0);
        when(quoteService.getQuote("AAPL")).thenReturn(quote);

        PortfolioSummaryResponse response = portfolioService.getSummary(clientId);

        assertEquals(new BigDecimal("10.00"), response.getDailyChangeUsd());
        assertEquals(new BigDecimal("4.35"), response.getDailyChangePercent());
    }

    private Quote quote(String symbol, String currency, double price, double change) {
        Quote quote = new Quote();
        quote.setSymbol(symbol);
        quote.setCurrency(currency);
        quote.setPrice(price);
        quote.setChange(change);
        return quote;
    }
}