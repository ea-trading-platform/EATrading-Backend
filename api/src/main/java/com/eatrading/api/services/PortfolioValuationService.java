package com.eatrading.api.services;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.eatrading.api.dto.Quote;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Holding;
import com.eatrading.api.objects.Instrument;

@Service
public class PortfolioValuationService {
    private static final BigDecimal ONE = BigDecimal.ONE;

    private final QuoteService quoteService;

    public PortfolioValuationService(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    public Valuation value(Holding holding) {
        Asset asset = holding.getAsset();
        if (asset == null || holding.getQuantity() == null || holding.getAvgBuyPrice() == null) {
            throw new IllegalStateException("Holding is missing required valuation data");
        }

        BigDecimal currentPriceUsd = ONE;
        BigDecimal dailyChangeUsdPerUnit = BigDecimal.ZERO;
        BigDecimal dailyChangePercent = BigDecimal.ZERO;

        if (!isUsdCash(asset)) {
            String quoteSymbol = normalizeQuoteSymbol(asset);
            Quote quote = quoteService.getQuote(quoteSymbol);
            if (quote == null) {
                throw new IllegalStateException("No quote returned for symbol: " + quoteSymbol);
            }

            BigDecimal conversionRate = getUsdConversionRate(quote.getCurrency());
            currentPriceUsd = BigDecimal.valueOf(quote.getPrice()).multiply(conversionRate);
            if (quote.getChange() != null) {
                dailyChangeUsdPerUnit = BigDecimal.valueOf(quote.getChange()).multiply(conversionRate);
            }
            if (quote.getChangePercent() != null) {
                dailyChangePercent = BigDecimal.valueOf(quote.getChangePercent());
            }
        }

        BigDecimal quantity = holding.getQuantity();
        BigDecimal purchasedValueUsd = quantity.multiply(holding.getAvgBuyPrice());
        BigDecimal currentValueUsd = quantity.multiply(currentPriceUsd);
        BigDecimal unrealizedReturnUsd = currentValueUsd.subtract(purchasedValueUsd);

        return new Valuation(currentPriceUsd, currentValueUsd, purchasedValueUsd,
                quantity.multiply(dailyChangeUsdPerUnit), dailyChangePercent, unrealizedReturnUsd);
    }

    public boolean isUsdCash(Asset asset) {
        return asset.getInstrument() == Instrument.CASH && "USD".equalsIgnoreCase(asset.getSymbol());
    }

    private String normalizeQuoteSymbol(Asset asset) {
        String symbol = asset.getSymbol().toUpperCase();
        if (asset.getInstrument() == Instrument.CASH && !symbol.startsWith("FX:")) {
            return "FX:" + symbol + "USD";
        }
        if (asset.getInstrument() == Instrument.CRYPTO && !symbol.startsWith("X:")) {
            return "X:" + symbol + "-USD";
        }
        return symbol;
    }

    private BigDecimal getUsdConversionRate(String currency) {
        if (currency == null || currency.isBlank() || "USD".equalsIgnoreCase(currency)) {
            return ONE;
        }

        String normalizedCurrency = currency.toUpperCase();
        String fxSymbol = normalizedCurrency.startsWith("FX:")
                ? normalizedCurrency
                : "FX:" + normalizedCurrency + "USD";
        Quote fxQuote = quoteService.getQuote(fxSymbol);
        if (fxQuote == null) {
            throw new IllegalStateException("No USD conversion quote returned for: " + currency);
        }
        return BigDecimal.valueOf(fxQuote.getPrice());
    }

    public static final class Valuation {
        private final BigDecimal currentPriceUsd;
        private final BigDecimal currentValueUsd;
        private final BigDecimal purchasedValueUsd;
        private final BigDecimal dailyChangeUsd;
        private final BigDecimal dailyChangePercent;
        private final BigDecimal unrealizedReturnUsd;

        private Valuation(BigDecimal currentPriceUsd, BigDecimal currentValueUsd,
                BigDecimal purchasedValueUsd, BigDecimal dailyChangeUsd,
                BigDecimal dailyChangePercent, BigDecimal unrealizedReturnUsd) {
            this.currentPriceUsd = currentPriceUsd;
            this.currentValueUsd = currentValueUsd;
            this.purchasedValueUsd = purchasedValueUsd;
            this.dailyChangeUsd = dailyChangeUsd;
            this.dailyChangePercent = dailyChangePercent;
            this.unrealizedReturnUsd = unrealizedReturnUsd;
        }

        public BigDecimal getCurrentPriceUsd() {
            return currentPriceUsd;
        }

        public BigDecimal getCurrentValueUsd() {
            return currentValueUsd;
        }

        public BigDecimal getPurchasedValueUsd() {
            return purchasedValueUsd;
        }

        public BigDecimal getDailyChangeUsd() {
            return dailyChangeUsd;
        }

        public BigDecimal getDailyChangePercent() {
            return dailyChangePercent;
        }

        public BigDecimal getUnrealizedReturnUsd() {
            return unrealizedReturnUsd;
        }
    }
}
