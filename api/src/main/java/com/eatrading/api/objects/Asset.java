package com.eatrading.api.objects;

import java.math.BigDecimal;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;

@Embeddable
public class Asset {
    private static final BigDecimal DEFAULT_PRICE = BigDecimal.valueOf(2.0);
    private static final BigDecimal USD_PRICE = BigDecimal.ONE;

    @FunctionalInterface
    public interface MarketPriceResolver {
        BigDecimal resolve(String symbol);
    }

    private static volatile MarketPriceResolver marketPriceResolver;

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

    public static void setMarketPriceResolver(MarketPriceResolver resolver) {
        marketPriceResolver = resolver;
    }

    @Transient
    public BigDecimal getCurrMarketPrice() {
        if ("USD".equalsIgnoreCase(this.symbol)) {
            return USD_PRICE;
        }

        MarketPriceResolver resolver = marketPriceResolver;
        if (resolver != null && this.symbol != null && !this.symbol.isBlank()) {
            try {
                BigDecimal price = resolver.resolve(this.symbol);
                if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
                    return price;
                }
            } catch (Exception ignored) {
                // Fall back to default price for non-Spring contexts or temporary service failures.
            }
        }

        return DEFAULT_PRICE;
    }
}