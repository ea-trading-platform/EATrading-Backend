package com.eatrading.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import com.eatrading.api.dto.Quote;
import com.eatrading.api.entities.Client;
import com.eatrading.api.entities.Order;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.BaseMarket;
import com.eatrading.api.objects.Holding;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.objects.OrderResponse;
import com.eatrading.api.objects.Status;
import com.eatrading.api.repository.ClientRepository;

@ExtendWith(MockitoExtension.class)
class OrderProcessorMarketHoursValidationTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private QuoteService quoteService;

    @Mock
    private PortfolioSnapshotService portfolioSnapshotService;

    private OrderProcessor orderProcessor;

    @BeforeEach
    void setUp() {
        setCurrentTime("2026-10-07T14:00:00Z");
    }

    @Test
    void validateBuy_returnsSubmitted_whenMarketStateClosed() {
        Quote quote = quote("NVDA", 100.0, "closed", "USD", "2026-10-07T14:00:00Z");
        Order order = buyOrder("NVDA", new BigDecimal("1"), new BigDecimal("100"));

        when(quoteService.getQuote("NVDA")).thenReturn(quote);

        OrderResponse response = orderProcessor.validate(order);

        assertEquals(Status.SUBMITTED, response.getStatusCode());
        verify(clientRepository, never()).findById(order.getClientId());
        verify(quoteService, times(2)).getQuote("NVDA");
    }

    @Test
    void validateBuy_returnsSubmitted_whenUnknownStateOutsideUsHours() {
        setCurrentTime("2026-10-07T13:00:00Z");
        // 13:00Z = 09:00 New York (before US market open).
        Quote quote = quote("NVDA", 100.0, "unknown", "USD", "2026-10-06T14:00:00Z");
        Order order = buyOrder("NVDA", new BigDecimal("1"), new BigDecimal("100"));

        when(quoteService.getQuote("NVDA")).thenReturn(quote);

        OrderResponse response = orderProcessor.validate(order);

        assertEquals(Status.SUBMITTED, response.getStatusCode());
        verify(clientRepository, never()).findById(order.getClientId());
    }

    @Test
    void validateBuy_returnsSubmitted_whenUnknownStateOnWeekend() {
        setCurrentTime("2026-10-10T14:00:00Z");
        // Saturday during nominal US hours still should be closed.
        Quote quote = quote("NVDA", 100.0, "unknown", "USD", "2026-10-10T14:00:00Z");
        Order order = buyOrder("NVDA", new BigDecimal("1"), new BigDecimal("100"));

        when(quoteService.getQuote("NVDA")).thenReturn(quote);

        OrderResponse response = orderProcessor.validate(order);

        assertEquals(Status.SUBMITTED, response.getStatusCode());
        verify(clientRepository, never()).findById(order.getClientId());
    }

    @Test
    void validateBuy_returnsAccepted_whenUnknownStateWithinUsHours() {
        // 14:00Z = 10:00 New York (inside US market hours).
        Quote quote = quote("NVDA", 100.0, "unknown", "USD", "2026-10-07T14:00:00Z");
        Order order = buyOrder("NVDA", new BigDecimal("2"), new BigDecimal("100"));

        when(quoteService.getQuote("NVDA")).thenReturn(quote, quote);
        when(clientRepository.findById(order.getClientId())).thenReturn(Optional.of(clientWithCash("5000")));

        OrderResponse response = orderProcessor.validate(order);

        assertEquals(Status.ACCEPTED, response.getStatusCode());
        verify(clientRepository, times(1)).findById(order.getClientId());
        verify(quoteService, times(3)).getQuote("NVDA");
    }

    @Test
    void validateSell_returnsAccepted_whenUnknownStateWithinIndianHours() {
        setCurrentTime("2026-10-07T04:30:00Z");
        // 04:30Z = 10:00 India (inside Indian market hours).
        Quote quote = quote("AAPL", 150.0, "unknown", "INR", "2026-10-07T04:30:00Z");
        Order order = sellOrder("AAPL", new BigDecimal("5"), new BigDecimal("150"));

        when(quoteService.getQuote("AAPL")).thenReturn(quote, quote);
        when(clientRepository.findById(order.getClientId())).thenReturn(Optional.of(clientWithCashAndShares("2000", "AAPL", "10", "150")));

        OrderResponse response = orderProcessor.validate(order);

        assertEquals(Status.ACCEPTED, response.getStatusCode());
    }

    @Test
    void validateBuy_setsBaseMarketFromQuoteCurrency() {
        Quote quote = quote("NVDA", 100.0, "open", "USD", "2026-10-07T14:00:00Z");
        Order order = buyOrder("NVDA", new BigDecimal("2"), new BigDecimal("100"));

        when(quoteService.getQuote("NVDA")).thenReturn(quote, quote);
        when(clientRepository.findById(order.getClientId())).thenReturn(Optional.of(clientWithCash("5000")));

        OrderResponse response = orderProcessor.validate(order);

        assertEquals(Status.ACCEPTED, response.getStatusCode());
        assertEquals(BaseMarket.US, order.getBaseMarket());
    }

    @Test
    void validateSell_returnsSubmitted_whenUnknownStateOutsideIndianHours() {
        setCurrentTime("2026-10-07T11:00:00Z");
        // 11:00Z = 16:30 India (after Indian market close).
        Quote quote = quote("AAPL", 150.0, "unknown", "INR", "2026-10-07T11:00:00Z");
        Order order = sellOrder("AAPL", new BigDecimal("5"), new BigDecimal("150"));

        when(quoteService.getQuote("AAPL")).thenReturn(quote);

        OrderResponse response = orderProcessor.validate(order);

        assertEquals(Status.SUBMITTED, response.getStatusCode());
        verify(clientRepository, never()).findById(order.getClientId());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"not-an-instant", "2026-10-22T20:00:01Z"})
    void validateBuy_usesCurrentTime_whenUnknownStateRegardlessOfQuoteTimestamp(String asOf) {
        setCurrentTime("2026-10-23T13:30:01Z");
        Quote quote = quote("NVDA", 100.0, "unknown", "USD", asOf);
        Order order = buyOrder("NVDA", new BigDecimal("1"), new BigDecimal("100"));

        when(quoteService.getQuote("NVDA")).thenReturn(quote);
        when(clientRepository.findById(order.getClientId())).thenReturn(Optional.of(clientWithCash("5000")));

        OrderResponse response = orderProcessor.validate(order);

        assertEquals(Status.ACCEPTED, response.getStatusCode());
        assertEquals(asOf, quote.getAsOf());
    }

    @ParameterizedTest
    @CsvSource({
        "USD, 2026-10-23T13:29:59Z, SUBMITTED",
        "USD, 2026-10-23T13:30:00Z, ACCEPTED",
        "USD, 2026-10-23T20:00:00Z, ACCEPTED",
        "USD, 2026-10-23T20:00:01Z, SUBMITTED",
        "USD, 2026-10-24T14:00:00Z, SUBMITTED",
        "USD, 2026-10-25T14:00:00Z, SUBMITTED",
        "USD, 2026-11-02T14:29:59Z, SUBMITTED",
        "USD, 2026-11-02T14:30:00Z, ACCEPTED",
        "INR, 2026-10-23T03:44:59Z, SUBMITTED",
        "INR, 2026-10-23T03:45:00Z, ACCEPTED",
        "INR, 2026-10-23T10:00:00Z, ACCEPTED",
        "INR, 2026-10-23T10:00:01Z, SUBMITTED",
        "INR, 2026-10-24T04:30:00Z, SUBMITTED"
    })
    void validateBuy_checksCurrentExchangeHours_whenMarketStateUnknown(
            String currency, String now, Status expectedStatus) {
        setCurrentTime(now);
        Quote quote = quote("NVDA", 100.0, "unknown", currency, "2026-10-22T14:00:00Z");
        Order order = buyOrder("NVDA", BigDecimal.ONE, new BigDecimal("100"));
        when(quoteService.getQuote("NVDA")).thenReturn(quote);
        if (expectedStatus == Status.ACCEPTED) {
            when(clientRepository.findById(order.getClientId())).thenReturn(Optional.of(clientWithCash("5000")));
        }

        assertEquals(expectedStatus, orderProcessor.validate(order).getStatusCode());
        if (expectedStatus == Status.SUBMITTED) {
            verify(clientRepository, never()).findById(order.getClientId());
        }
    }

    @Test
    void validateBuy_honorsExplicitOpenState_outsideFallbackHours() {
        setCurrentTime("2026-10-24T03:00:00Z");
        Quote quote = quote("NVDA", 100.0, "open", "USD", null);
        Order order = buyOrder("NVDA", BigDecimal.ONE, new BigDecimal("100"));
        when(quoteService.getQuote("NVDA")).thenReturn(quote);
        when(clientRepository.findById(order.getClientId())).thenReturn(Optional.of(clientWithCash("5000")));

        assertEquals(Status.ACCEPTED, orderProcessor.validate(order).getStatusCode());
    }

    @Test
    void validateBuy_returnsSubmitted_whenMarketMetadataMissing() {
        Quote quote = quote("NVDA", 100.0, null, "USD", "2026-10-07T14:00:00Z");
        Order order = buyOrder("NVDA", new BigDecimal("1"), new BigDecimal("100"));

        when(quoteService.getQuote("NVDA")).thenReturn(quote);

        OrderResponse response = orderProcessor.validate(order);

        assertEquals(Status.SUBMITTED, response.getStatusCode());
        verify(clientRepository, never()).findById(order.getClientId());
    }

    @Test
    void validateBuy_returnsSubmitted_whenQuoteCurrencyUnsupported() {
        Quote quote = quote("NVDA", 100.0, "unknown", "EUR", "2026-10-07T14:00:00Z");
        Order order = buyOrder("NVDA", new BigDecimal("1"), new BigDecimal("100"));

        when(quoteService.getQuote("NVDA")).thenReturn(quote);

        OrderResponse response = orderProcessor.validate(order);

        assertEquals(Status.SUBMITTED, response.getStatusCode());
        verify(clientRepository, never()).findById(order.getClientId());
    }

    @Test
    void validateBuy_skipsMarketHoursGate_whenSymbolContainsColon() {
        Quote quote = quote("NVDA:EXT", 100.0, "closed", "USD", "2026-10-07T03:00:00Z");
        Order order = buyOrder("NVDA:EXT", new BigDecimal("2"), new BigDecimal("100"));

        when(quoteService.getQuote("NVDA:EXT")).thenReturn(quote, quote);
        when(clientRepository.findById(order.getClientId())).thenReturn(Optional.of(clientWithCash("5000")));

        OrderResponse response = orderProcessor.validate(order);

        assertEquals(Status.ACCEPTED, response.getStatusCode());
        verify(clientRepository, times(1)).findById(order.getClientId());
        verify(quoteService, times(3)).getQuote("NVDA:EXT");
    }

    private void setCurrentTime(String instant) {
        orderProcessor = new OrderProcessor(clientRepository, quoteService, portfolioSnapshotService,
                Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    private Quote quote(String symbol, double price, String marketState, String currency, String asOf) {
        Quote quote = new Quote();
        quote.setSymbol(symbol);
        quote.setPrice(price);
        quote.setMarketState(marketState);
        quote.setCurrency(currency);
        quote.setAsOf(asOf);
        return quote;
    }

    private Order buyOrder(String symbol, BigDecimal quantity, BigDecimal price) {
        return new Order(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new Asset(symbol, symbol, Instrument.EQUITY),
                quantity,
                true,
                price);
    }

    private Order sellOrder(String symbol, BigDecimal quantity, BigDecimal price) {
        return new Order(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new Asset(symbol, symbol, Instrument.EQUITY),
                quantity,
                false,
                price);
    }

    private Client clientWithCash(String usd) {
        Client client = new Client("Test User", "test@example.com");
        client.addHolding(new Holding(new Asset("USD", "US DOLLAR", Instrument.CASH), new BigDecimal(usd), BigDecimal.ONE));
        return client;
    }

    private Client clientWithCashAndShares(String usd, String symbol, String shares, String avgPrice) {
        Client client = clientWithCash(usd);
        client.addHolding(new Holding(new Asset(symbol, symbol, Instrument.EQUITY), new BigDecimal(shares), new BigDecimal(avgPrice)));
        return client;
    }
}
