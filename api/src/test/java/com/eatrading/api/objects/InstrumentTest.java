package com.eatrading.api.objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InstrumentTest {

    @Test
    void testEnumContainsExpectedValues() {
        Instrument[] values = Instrument.values();
        assertNotNull(values);
        assertTrue(values.length >= 3);
        assertEquals(Instrument.CASH, Instrument.valueOf("CASH"));
        assertEquals(Instrument.EQUITY, Instrument.valueOf("EQUITY"));
        assertEquals(Instrument.CRYPTO, Instrument.valueOf("CRYPTO"));
    }
}
