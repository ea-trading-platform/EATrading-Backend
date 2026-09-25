package com.eatrading.api.Objects;

import java.math.BigDecimal;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;

@Embeddable
public class Asset {
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

    @Transient
    public BigDecimal getCurrMarketPrice() {
        // Mock service
        BigDecimal price = new BigDecimal(2.0);
        return price;
    }
}