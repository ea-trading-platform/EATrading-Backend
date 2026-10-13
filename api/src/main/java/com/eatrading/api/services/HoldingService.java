package com.eatrading.api.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eatrading.api.dto.HoldingResponse;
import com.eatrading.api.dto.Quote;
import com.eatrading.api.entities.Client;
import com.eatrading.api.exception.ResourceNotFoundException;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Holding;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.repository.ClientRepository;

@Service
public class HoldingService {
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int SCALE = 8;

    private final ClientRepository clientRepository;
    private final QuoteService quoteService;

    public HoldingService(ClientRepository clientRepository, QuoteService quoteService) {
        this.clientRepository = clientRepository;
        this.quoteService = quoteService;
    }

    @Transactional(readOnly = true)
    public List<HoldingResponse> getClientHoldings(UUID clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + clientId));

        return client.getPortfolioHoldings().stream()
                .map(holding -> toResponse(clientId, holding))
                .collect(Collectors.toList());
    }

    private HoldingResponse toResponse(UUID clientId, Holding holding) {
        Asset asset = holding.getAsset();
        if (asset == null || holding.getQuantity() == null || holding.getAvgBuyPrice() == null) {
            throw new IllegalStateException("Holding is missing required valuation data");
        }

        Quote quote = null;
        BigDecimal currentPriceUsd = ONE;
        BigDecimal dailyChangeUsdPerUnit = BigDecimal.ZERO;
        BigDecimal dailyChangePercent = BigDecimal.ZERO;

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
            if (quote.getChangePercent() != null) {
                dailyChangePercent = BigDecimal.valueOf(quote.getChangePercent());
            }
        }

        BigDecimal quantity = holding.getQuantity();
        BigDecimal avgBuyPrice = holding.getAvgBuyPrice();
        BigDecimal currentValueUsd = quantity.multiply(currentPriceUsd);
        BigDecimal purchasedValueUsd = quantity.multiply(avgBuyPrice);
        BigDecimal unrealizedReturn = currentValueUsd.subtract(purchasedValueUsd);

        HoldingResponse response = new HoldingResponse(clientId.toString(), asset.getSymbol(),
                format(quantity), format(avgBuyPrice));
        response.setName(asset.getName());
        response.setInstrument(asset.getInstrument() == null ? null : asset.getInstrument().name());
        response.setCurrentPrice(format(currentPriceUsd));
        response.setCurrentValue(format(currentValueUsd));
        response.setDailyChange(format(quantity.multiply(dailyChangeUsdPerUnit)));
        response.setDailyChangePercent(format(dailyChangePercent));
        response.setUnrealizedReturn(format(unrealizedReturn));
        response.setUnrealizedReturnPercent(format(percentage(unrealizedReturn, purchasedValueUsd)));
        return response;
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

    private BigDecimal getUsdConversionRate(String currency) {
        if (currency == null || currency.isBlank() || "USD".equalsIgnoreCase(currency)) {
            return ONE;
        }
        String fxSymbol = currency.toUpperCase().startsWith("FX:")
                ? currency.toUpperCase()
                : "FX:" + currency.toUpperCase() + "USD";
        Quote fxQuote = quoteService.getQuote(fxSymbol);
        if (fxQuote == null) {
            throw new IllegalStateException("No USD conversion quote returned for: " + currency);
        }
        return BigDecimal.valueOf(fxQuote.getPrice());
    }

    private BigDecimal percentage(BigDecimal value, BigDecimal denominator) {
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return value.multiply(HUNDRED).divide(denominator, SCALE, RoundingMode.HALF_UP);
    }

    private String format(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}