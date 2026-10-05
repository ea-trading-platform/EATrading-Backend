package com.eatrading.api.support;

import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.mockito.Mockito;

import com.eatrading.api.dto.Quote;
import com.eatrading.api.entities.Client;
import com.eatrading.api.entities.User;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Holding;
import com.eatrading.api.objects.Instrument;

public final class TradeTestFixtures {

    private TradeTestFixtures() {
    }

    public static User mockUser(String name, String email) {
        return new MockUser(name, email);
    }

    public static Client mockClientWithUsdCash(String name, String email, BigDecimal usdCashValue) {
        Client client = new Client(name, email);
        client.addHolding(new Holding(mockAsset("USD", "US DOLLAR", Instrument.CASH, BigDecimal.ONE), usdCashValue));
        return client;
    }

    public static void addHolding(Client client, String symbol, String assetName, Instrument instrument,
            BigDecimal marketPrice, BigDecimal quantity) {
        client.addHolding(new Holding(mockAsset(symbol, assetName, instrument, marketPrice), quantity));
    }

    public static Asset mockAsset(String symbol, String name, Instrument instrument, BigDecimal marketPrice) {
        Asset asset = Mockito.mock(Asset.class);
        when(asset.getSymbol()).thenReturn(symbol);
        Mockito.lenient().when(asset.getName()).thenReturn(name);
        Mockito.lenient().when(asset.getInstrument()).thenReturn(instrument);
        when(asset.getCurrMarketPrice()).thenReturn(marketPrice);
        return asset;
    }

    public static Quote quote(String symbol, double price) {
        Quote quote = new Quote();
        quote.setSymbol(symbol);
        quote.setPrice(price);
        quote.setBid(price - 0.1);
        quote.setAsk(price + 0.1);
        quote.setSpreadBps(20.0);
        return quote;
    }

    private static final class MockUser extends User {
        private MockUser(String name, String email) {
            super(name, email);
        }
    }
}