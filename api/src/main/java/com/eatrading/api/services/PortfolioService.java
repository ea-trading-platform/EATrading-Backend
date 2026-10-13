package com.eatrading.api.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eatrading.api.dto.PortfolioSummaryResponse;
import com.eatrading.api.dto.Quote;
import com.eatrading.api.entities.Client;
import com.eatrading.api.exception.ResourceNotFoundException;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Holding;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.repository.ClientRepository;

@Service
public class PortfolioService {
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int SCALE = 8;

    private final ClientRepository clientRepository;
    private final QuoteService quoteService;

    public PortfolioService(ClientRepository clientRepository, QuoteService quoteService) {
        this.clientRepository = clientRepository;
        this.quoteService = quoteService;
    }

    @Transactional(readOnly = true)
    public PortfolioSummaryResponse getSummary(UUID clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

        BigDecimal availableCashUsd = BigDecimal.ZERO;
        BigDecimal portfolioValueUsd = BigDecimal.ZERO;
        BigDecimal investedValueUsd = BigDecimal.ZERO;
        BigDecimal dailyChangeUsd = BigDecimal.ZERO;

        for (Holding holding : client.getPortfolioHoldings()) {
            Asset asset = holding.getAsset();
            if (asset == null || holding.getQuantity() == null || holding.getAvgBuyPrice() == null) {
                continue;
            }

            Quote quote = null;
            BigDecimal currentPriceUsd = ONE;
            BigDecimal dailyChangeUsdPerUnit = BigDecimal.ZERO;

            if (!isUsdCash(asset)) {
                String quoteSymbol = normalizeQuoteSymbol(asset);
                quote = quoteService.getQuote(quoteSymbol);
                if (quote == null) {
                    throw new IllegalStateException("No quote returned for symbol: " + quoteSymbol);
                }
                BigDecimal conversionRate = getUsdConversionRate(quote.getCurrency());
                currentPriceUsd = BigDecimal.valueOf(quote.getPrice()).multiply(conversionRate);
                if (quote.getChange() != null) {
                    dailyChangeUsdPerUnit = BigDecimal.valueOf(quote.getChange()).multiply(conversionRate);
                }
            }

            BigDecimal quantity = holding.getQuantity();
            BigDecimal currentValueUsd = quantity.multiply(currentPriceUsd);
            BigDecimal costValueUsd = quantity.multiply(holding.getAvgBuyPrice());

            portfolioValueUsd = portfolioValueUsd.add(currentValueUsd);
            investedValueUsd = investedValueUsd.add(costValueUsd);
            dailyChangeUsd = dailyChangeUsd.add(quantity.multiply(dailyChangeUsdPerUnit));

            if (isUsdCash(asset)) {
                availableCashUsd = availableCashUsd.add(currentValueUsd);
            }
        }

        BigDecimal overallReturnUsd = portfolioValueUsd.subtract(investedValueUsd);
        BigDecimal overallReturnPercent = percentage(overallReturnUsd, investedValueUsd);
        BigDecimal dailyChangePercent = percentage(dailyChangeUsd, portfolioValueUsd.subtract(dailyChangeUsd));

        return new PortfolioSummaryResponse(clientId, scale(availableCashUsd), scale(portfolioValueUsd),
                scale(investedValueUsd), scale(overallReturnUsd), scale(overallReturnPercent),
                scale(dailyChangeUsd), scale(dailyChangePercent), Instant.now());
    }

    private BigDecimal getUsdConversionRate(String currency) {
        String normalizedCurrency = currency;
        if (normalizedCurrency == null || normalizedCurrency.isBlank()
                || "USD".equalsIgnoreCase(normalizedCurrency)) {
            return ONE;
        }

        String fxSymbol = normalizedCurrency.toUpperCase().startsWith("FX:")
                ? normalizedCurrency.toUpperCase()
                : "FX:" + normalizedCurrency.toUpperCase() + "USD";
        Quote fxQuote = quoteService.getQuote(fxSymbol);
        if (fxQuote == null) {
            throw new IllegalStateException("No USD conversion quote returned for: " + normalizedCurrency);
        }
        return BigDecimal.valueOf(fxQuote.getPrice());
    }

    private boolean isUsdCash(Asset asset) {
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

    private BigDecimal percentage(BigDecimal value, BigDecimal denominator) {
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.multiply(HUNDRED).divide(denominator, SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}