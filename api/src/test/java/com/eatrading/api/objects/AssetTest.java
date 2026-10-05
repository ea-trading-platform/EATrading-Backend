package com.eatrading.api.objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for `Asset`.
 */
@ExtendWith(MockitoExtension.class)
class AssetTest {

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        Asset.setMarketPriceResolver(null);
    }

    @Test
    void testGetters_ReturnConstructorValues() {
        Asset a = new Asset("AAPL", "Apple Inc", Instrument.EQUITY);
        assertEquals("AAPL", a.getSymbol());
        assertEquals("Apple Inc", a.getName());
        assertEquals(Instrument.EQUITY, a.getInstrument());
    }

    @Test
    void testGetCurrMarketPrice_DefaultFallbackPresent() {
        Asset a = new Asset("FAKE", "Fake Co", Instrument.EQUITY);
        BigDecimal price = a.getCurrMarketPrice();
        assertNotNull(price);
        assertEquals(0, price.compareTo(BigDecimal.valueOf(2.0)));
    }

    @Test
    void testGetCurrMarketPrice_UsesResolverPrice() {
        Asset.setMarketPriceResolver(symbol -> new BigDecimal("123.45"));
        Asset a = new Asset("FAKE", "Fake Co", Instrument.EQUITY);

        BigDecimal price = a.getCurrMarketPrice();

        assertEquals(0, price.compareTo(new BigDecimal("123.45")));
    }

    @Test
    void testGetCurrMarketPrice_UsdUsesOne() {
        Asset.setMarketPriceResolver(symbol -> new BigDecimal("999"));
        Asset usd = new Asset("USD", "US DOLLAR", Instrument.CASH);

        BigDecimal price = usd.getCurrMarketPrice();

        assertEquals(0, price.compareTo(BigDecimal.ONE));
    }

    // ===== EDGE CASES =====

    @Test
    void testConstructor_AllNullFields() {
        Asset a = new Asset(null, null, null);
        assertNull(a.getSymbol());
        assertNull(a.getName());
        assertNull(a.getInstrument());
    }

}
