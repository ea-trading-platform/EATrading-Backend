package com.eatrading.api.objects;

import java.math.BigDecimal;

import jakarta.persistence.Embeddable;

@Embeddable
public class Asset {
    private static final BigDecimal USD_PRICE = BigDecimal.ONE;

    private String symbol;
    private String name;
    private Instrument instrument;

    public Asset() {
        this.symbol = null;
        this.name = null;
        this.instrument = null;
    }

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
}