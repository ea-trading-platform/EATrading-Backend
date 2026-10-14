package com.eatrading.api.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eatrading.api.dto.HoldingResponse;
import com.eatrading.api.entities.Client;
import com.eatrading.api.exception.ResourceNotFoundException;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Holding;
import com.eatrading.api.repository.ClientRepository;

@Service
public class HoldingService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int SCALE = 8;

    private final ClientRepository clientRepository;
    private final PortfolioValuationService valuationService;

    public HoldingService(ClientRepository clientRepository, PortfolioValuationService valuationService) {
        this.clientRepository = clientRepository;
        this.valuationService = valuationService;
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
        PortfolioValuationService.Valuation valuation = valuationService.value(holding);

        HoldingResponse response = new HoldingResponse(clientId.toString(), asset.getSymbol(),
                format(holding.getQuantity()), format(holding.getAvgBuyPrice()));
        response.setName(asset.getName());
        response.setInstrument(asset.getInstrument() == null ? null : asset.getInstrument().name());
        response.setCurrentPrice(format(valuation.getCurrentPriceUsd()));
        response.setCurrentValue(format(valuation.getCurrentValueUsd()));
        response.setDailyChange(format(valuation.getDailyChangeUsd()));
        response.setDailyChangePercent(format(valuation.getDailyChangePercent()));
        response.setUnrealizedReturn(format(valuation.getUnrealizedReturnUsd()));
        response.setUnrealizedReturnPercent(format(
                percentage(valuation.getUnrealizedReturnUsd(), valuation.getPurchasedValueUsd())));
        return response;
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