package com.eatrading.api.config;

import java.math.BigDecimal;

import org.springframework.context.annotation.Configuration;

import com.eatrading.api.objects.Asset;
import com.eatrading.api.services.QuoteService;

@Configuration
public class AssetMarketPriceConfig {

    public AssetMarketPriceConfig(QuoteService quoteService) {
        Asset.setMarketPriceResolver(symbol -> BigDecimal.valueOf(quoteService.getCurrentPrice(symbol)));
    }
}
