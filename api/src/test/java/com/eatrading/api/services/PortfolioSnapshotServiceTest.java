package com.eatrading.api.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eatrading.api.dto.Quote;
import com.eatrading.api.entities.Client;
import com.eatrading.api.entities.PortfolioSnapshot;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Holding;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.repository.ClientRepository;
import com.eatrading.api.repository.PortfolioSnapshotRepository;

@ExtendWith(MockitoExtension.class)
class PortfolioSnapshotServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private PortfolioSnapshotRepository snapshotRepository;

    @Mock
    private QuoteService quoteService;

    private PortfolioSnapshotService snapshotService;

    @BeforeEach
    void setUp() {
        snapshotService = new PortfolioSnapshotService(
                clientRepository,
                snapshotRepository,
                new PortfolioValuationService(quoteService));
    }

    @Test
    void createTradeSnapshotPersistsUsdValuesForCashForexAndCrypto() throws Exception {
        UUID clientId = UUID.randomUUID();
        Client client = client(clientId);
        client.addHolding(new Holding(new Asset("USD", "US Dollar", Instrument.CASH),
                new BigDecimal("500"), BigDecimal.ONE));
        client.addHolding(new Holding(new Asset("INR", "Indian Rupee", Instrument.CASH),
                new BigDecimal("1000"), BigDecimal.ONE));
        client.addHolding(new Holding(new Asset("BTC", "Bitcoin", Instrument.CRYPTO),
                new BigDecimal("2"), new BigDecimal("900")));

        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(snapshotRepository.existsByClientIdAndTimestamp(any(), any())).thenReturn(false);
        when(quoteService.getQuote("FX:INRUSD")).thenReturn(quote("FX:INRUSD", "USD", 0.012, 0));
        when(quoteService.getQuote("X:BTC-USD")).thenReturn(quote("X:BTC-USD", "USD", 1000, 10));

        snapshotService.createTradeSnapshot(clientId);

        ArgumentCaptor<PortfolioSnapshot> captor = ArgumentCaptor.forClass(PortfolioSnapshot.class);
        verify(snapshotRepository).save(captor.capture());
        PortfolioSnapshot snapshot = captor.getValue();

        assertEquals(clientId, snapshot.getClientId());
        assertEquals(new BigDecimal("2512.00"), snapshot.getPortfolioValueUsd().setScale(2));
        assertEquals(new BigDecimal("500.00"), snapshot.getCashValueUsd().setScale(2));
        assertEquals(new BigDecimal("3300.00"), snapshot.getInvestedValueUsd().setScale(2));
        assertEquals(new BigDecimal("20.00"), snapshot.getDailyChangeUsd().setScale(2));
    }

    @Test
    void createTradeSnapshotDoesNotPersistDuplicateTimestamp() throws Exception {
        UUID clientId = UUID.randomUUID();
        Client client = client(clientId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(snapshotRepository.existsByClientIdAndTimestamp(any(), any())).thenReturn(true);

        snapshotService.createTradeSnapshot(clientId);

        verify(snapshotRepository, never()).save(any(PortfolioSnapshot.class));
    }

    @Test
    void endOfDaySnapshotsCreatesOneSnapshotPerClient() throws Exception {
        UUID clientId = UUID.randomUUID();
        Client client = client(clientId);
        when(clientRepository.findAll()).thenReturn(List.of(client));
        when(snapshotRepository.existsByClientIdAndTimestamp(any(), any())).thenReturn(false);

        snapshotService.createEndOfDaySnapshots();

        verify(snapshotRepository).save(any(PortfolioSnapshot.class));
    }

    private Client client(UUID clientId) throws Exception {
        Client client = new Client("Test Client", "test-" + clientId + "@example.com");
        Field id = client.getClass().getSuperclass().getDeclaredField("id");
        id.setAccessible(true);
        id.set(client, clientId);
        return client;
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
