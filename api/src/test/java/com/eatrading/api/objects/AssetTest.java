package com.eatrading.api.objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

/**
 * Tests for `Asset`.
 */
class AssetTest {

    @Test
    void testGetters_ReturnConstructorValues() {
        Asset a = new Asset("AAPL", "Apple Inc", Instrument.EQUITY);
        assertEquals("AAPL", a.getSymbol());
        assertEquals("Apple Inc", a.getName());
        assertEquals(Instrument.EQUITY, a.getInstrument());
    }

    @Test
    void testConstructor_AllNullFields() {
        Asset a = new Asset(null, null, null);
        assertNull(a.getSymbol());
        assertNull(a.getName());
        assertNull(a.getInstrument());
    }

}
