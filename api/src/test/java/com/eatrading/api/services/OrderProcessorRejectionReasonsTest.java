package com.eatrading.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eatrading.api.dto.Quote;
import com.eatrading.api.entities.Client;
import com.eatrading.api.entities.Order;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Holding;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.objects.OrderResponse;
import com.eatrading.api.objects.Status;
import com.eatrading.api.repository.ClientRepository;

@ExtendWith(MockitoExtension.class)
class OrderProcessorRejectionReasonsTest {

    private static final UUID CLIENT_ID = UUID.fromString("d7274a95-fd81-4a0b-be3e-682b3722111b");
    private static final String SYMBOL = "AAPL";

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private QuoteService quoteService;

    @Mock
    private PortfolioSnapshotService portfolioSnapshotService;

    private OrderProcessor orderProcessor;

    @BeforeEach
    void setUp() {
        orderProcessor = new OrderProcessor(clientRepository, quoteService, portfolioSnapshotService);
    }

    @Test
    void buyRejectsPriceOutsideVarianceWithReason() {
        stubQuote(100);

        OrderResponse response = orderProcessor.validate(order(true, "1", "110"));

        assertRejectedWithReason(response, "Stock price variance exceeds 5.0% of order price");
    }

    @Test
    void buyRejectsMissingClientWithReason() {
        stubQuote(100);
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.empty());

        OrderResponse response = orderProcessor.validate(order(true, "1", "100"));

        assertRejectedWithReason(response, "Client not found");
    }

    @Test
    void buyRejectsInsufficientCashWithAmountsInReason() {
        stubQuote(10);
        Client client = mock(Client.class);
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(client.getUSDHolding()).thenReturn(
                new Holding(new Asset("USD", "US DOLLAR", Instrument.CASH), new BigDecimal("25"), BigDecimal.ONE));

        OrderResponse response = orderProcessor.validate(order(true, "10", "10"));

        assertRejectedWithReason(response, "Insufficient USD cash. Required: $100, Available: $25");
    }

    @Test
    void sellRejectsPriceOutsideVarianceWithReason() {
        stubQuote(100);

        OrderResponse response = orderProcessor.validate(order(false, "1", "110"));

        assertRejectedWithReason(response, "Market price has fallen more than 5.0% below order price");
    }

    @Test
    void sellRejectsMissingClientWithReason() {
        stubQuote(100);
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.empty());

        OrderResponse response = orderProcessor.validate(order(false, "1", "100"));

        assertRejectedWithReason(response, "Client not found");
    }

    @Test
    void sellRejectsWhenClientDoesNotOwnSymbolWithReason() {
        stubQuote(100);
        Client client = mock(Client.class);
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(client.getHolding(SYMBOL)).thenReturn(null);

        OrderResponse response = orderProcessor.validate(order(false, "1", "100"));

        assertRejectedWithReason(response, "Client does not own AAPL");
    }

    @Test
    void sellRejectsInsufficientSharesWithAmountsInReason() {
        stubQuote(100);
        Client client = mock(Client.class);
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(client.getHolding(SYMBOL)).thenReturn(
                new Holding(new Asset(SYMBOL, "Apple", Instrument.EQUITY), new BigDecimal("2"), new BigDecimal("90")));

        OrderResponse response = orderProcessor.validate(order(false, "5", "100"));

        assertRejectedWithReason(response, "Insufficient shares. Required: 5, Available: 2");
    }

    @Test
    void executionRejectsInvalidPriceWithReason() {
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(mock(Client.class)));

        OrderResponse response = orderProcessor.executeOrder(order(true, "1", "0"));

        assertRejectedWithReason(response, "Invalid execution price on order");
    }

    private void stubQuote(double price) {
        Quote quote = new Quote(SYMBOL, price, price, price);
        quote.setCurrency("USD");
        quote.setMarketState("open");
        when(quoteService.getQuote(SYMBOL)).thenReturn(quote);
    }

    private Order order(boolean buy, String quantity, String price) {
        return new Order(UUID.randomUUID(), CLIENT_ID,
                new Asset(SYMBOL, "Apple", Instrument.EQUITY),
                new BigDecimal(quantity), buy, new BigDecimal(price));
    }

    private void assertRejectedWithReason(OrderResponse response, String rejectionReason) {
        assertEquals(Status.REJECTED, response.getStatusCode());
        assertEquals(rejectionReason, response.getRejectionReason());
    }
}