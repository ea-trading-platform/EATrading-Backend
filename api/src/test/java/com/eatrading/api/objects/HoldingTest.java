package com.eatrading.api.objects;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class HoldingTest {

    @Test
    void testDefaultConstructor_InitializesEmptyHolding() {
        Holding newHolding = new Holding();

        assertNull(newHolding.getAsset());
        assertNull(newHolding.getQuantity());
        assertNull(newHolding.getAvgBuyPrice());
    }

    @Test
    void testConstructorWithExplicitValues_InitializesHoldingProperties() {
        Asset asset = new Asset("AAPL", "Apple Inc", Instrument.EQUITY);
        BigDecimal quantity = new BigDecimal("10");
        BigDecimal avgBuyPrice = new BigDecimal("123.45");

        Holding holding = new Holding(asset, quantity, avgBuyPrice);

        assertEquals(asset, holding.getAsset());
        assertEquals(0, quantity.compareTo(holding.getQuantity()));
        assertEquals(0, avgBuyPrice.compareTo(holding.getAvgBuyPrice()));
    }

    @Test
    void testSetAndGetAsset() {
        Holding holding = new Holding();
        Asset asset = new Asset("MSFT", "Microsoft", Instrument.EQUITY);

        holding.setAsset(asset);

        assertEquals(asset, holding.getAsset());
    }

    @Test
    void testSetAndGetQuantity() {
        Holding holding = new Holding();
        BigDecimal newQuantity = new BigDecimal("25.5");

        holding.setQuantity(newQuantity);

        assertEquals(newQuantity, holding.getQuantity());
    }

    @Test
    void testSetAndGetAvgBuyPrice() {
        Holding holding = new Holding();
        BigDecimal newAvgBuyPrice = new BigDecimal("95.50");

        holding.setAvgBuyPrice(newAvgBuyPrice);

        assertEquals(newAvgBuyPrice, holding.getAvgBuyPrice());
    }

    @Test
    void testGetPurchasedValue_CalculatesQuantityTimesAvgBuyPrice() {
        Asset asset = new Asset("AAPL", "Apple Inc", Instrument.EQUITY);
        BigDecimal quantity = new BigDecimal("10");
        BigDecimal avgBuyPrice = new BigDecimal("100.00");
        Holding holding = new Holding(asset, quantity, avgBuyPrice);

        BigDecimal purchasedValue = holding.getPurchasedValue();

        assertEquals(0, new BigDecimal("1000.00").compareTo(purchasedValue));
    }

    @Test
    void testGetPurchasedValue_WithManuallySetAvgBuyPrice() {
        Asset asset = new Asset("AAPL", "Apple Inc", Instrument.EQUITY);
        BigDecimal quantity = new BigDecimal("5");
        Holding holding = new Holding(asset, quantity, new BigDecimal("150.00"));

        holding.setAvgBuyPrice(new BigDecimal("120.00"));

        BigDecimal purchasedValue = holding.getPurchasedValue();

        assertEquals(0, new BigDecimal("600.00").compareTo(purchasedValue));
    }

    @Test
    void testGetPurchasedValue_WithVerySmallQuantity() {
        Asset asset = new Asset("TEST", "Test", Instrument.EQUITY);
        Holding h = new Holding(asset, new BigDecimal("0.00000002"), new BigDecimal("0.00000001"));

        assertEquals(0, new BigDecimal("0.0000000000000002").compareTo(h.getPurchasedValue()));
    }

    @Test
    void testGetPurchasedValue_WithNullAvgBuyPrice_ThrowsNPE() {
        Holding h = new Holding();
        h.setQuantity(new BigDecimal("1"));
        h.setAvgBuyPrice(null);

        assertThrows(NullPointerException.class, h::getPurchasedValue);
    }

    @Test
    void testNegativeQuantity_ProducesNegativePurchasedValue() {
        Asset asset = new Asset("NEG", "Negative", Instrument.EQUITY);
        BigDecimal negativeQty = new BigDecimal("-10");
        Holding h = new Holding(asset, negativeQty, new BigDecimal("100.00"));

        BigDecimal expected = new BigDecimal("-1000.00");
        assertEquals(0, expected.compareTo(h.getPurchasedValue()));
    }

    @Test
    void testNullAsset_StillSupportsPurchasedValue() {
        Holding h = new Holding(null, new BigDecimal("2"), new BigDecimal("3"));

        assertEquals(0, new BigDecimal("6").compareTo(h.getPurchasedValue()));
    }
}
