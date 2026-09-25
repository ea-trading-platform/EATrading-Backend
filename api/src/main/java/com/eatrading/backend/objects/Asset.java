package com.eatrading.api.Objects;

import java.math.BigDecimal;

public class Asset {
    private final String symbol;
    private final String name;
    private final Instrument instrument;

    public Asset(String symbol, String name, Instrument instrument) {
        this.symbol = symbol;
        this.name = name;
        this.instrument = instrument;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public BigDecimal getCurrMarketPrice() {
        // Mock service
        BigDecimal price = new BigDecimal(2.0);
        return price;
    }
}