package com.eatrading.api.support;

import java.math.BigDecimal;

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
        client.addHolding(new Holding(asset("USD", "US DOLLAR", Instrument.CASH), usdCashValue, BigDecimal.ONE));
        return client;
    }

    public static void addHolding(Client client, String symbol, String assetName, Instrument instrument,
            BigDecimal marketPrice, BigDecimal quantity) {
        client.addHolding(new Holding(asset(symbol, assetName, instrument), quantity, marketPrice));
    }

    public static Asset asset(String symbol, String name, Instrument instrument) {
        return new Asset(symbol, name, instrument);
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