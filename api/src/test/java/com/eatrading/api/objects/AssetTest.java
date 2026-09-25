package com.eatrading.api.objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for `Asset`.
 *
 * Note: `getCurrMarketPrice()` currently returns a hard-coded value and
 * will be replaced by an API call in the future. Tests that depend on
 * the external API are included but commented out until the API/service
 * integration is implemented.
 */
@ExtendWith(MockitoExtension.class)
class AssetTest {

    @Test
    void testGetters_ReturnConstructorValues() {
        Asset a = new Asset("AAPL", "Apple Inc", Instrument.EQUITY);
        assertEquals("AAPL", a.getSymbol());
        assertEquals("Apple Inc", a.getName());
        assertEquals(Instrument.EQUITY, a.getInstrument());
    }

    @Test
    void testGetCurrMarketPrice_HardcodedValuePresent() {
        Asset a = new Asset("FAKE", "Fake Co", Instrument.CASH);
        BigDecimal price = a.getCurrMarketPrice();
        assertNotNull(price);
    }

    // Future tests for API-backed price fetching. Keep commented until API exists.
    /*
    @Test
    void testGetCurrMarketPrice_UsesPriceService() {
        // Arrange
        // - Create a mock PriceService
        // - Inject into Asset (requires constructor or setter accepting service)

        // Act
        // BigDecimal price = asset.getCurrMarketPrice();

        // Assert
        // assertEquals(new BigDecimal("123.45"), price);
    }

    @Test
    void testGetCurrMarketPrice_HandlesServiceFailuresGracefully() {
        // Arrange: mock service throws exception or returns null
        // Act & Assert: ensure getCurrMarketPrice() returns fallback or throws documented exception
    }
    */
}
