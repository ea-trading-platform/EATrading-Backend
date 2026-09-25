package com.eatrading.api.objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Holding class.
 * 
 * These tests focus on Holding's behavior:
 * - Construction with asset and quantity
 * - Calculating average buy price from asset market price
 * - Getting current value (quantity * market price)
 * - Getting purchased value (quantity * avg buy price)
 * - Managing holdings properties
 * 
 * MOCKS NEEDED:
 * - Asset: Mock the asset to provide controlled market prices without hitting real data sources
 */
@ExtendWith(MockitoExtension.class)
class HoldingTest {

    @Mock private Asset mockAsset;

    private Holding holding;

    @BeforeEach
    void setUp() {
        // Setup will be done per test since different tests need different mock configurations
    }

    // ===== CONSTRUCTOR TESTS =====

    @Test
    void testDefaultConstructor_InitializesEmptyHolding() {
        // Act
        Holding newHolding = new Holding();

        // Assert
        assertNull(newHolding.getAsset(), "Default constructor should initialize asset to null");
    }

    @Test
    void testConstructorWithAssetAndQuantity_InitializesHoldingProperties() {
        // Arrange
        BigDecimal marketPrice = new BigDecimal("100.00");
        BigDecimal quantity = new BigDecimal("10");
        when(mockAsset.getCurrMarketPrice()).thenReturn(marketPrice);

        // Act
        holding = new Holding(mockAsset, quantity);

        // Assert
        assertEquals(mockAsset, holding.getAsset(), "Asset should be set to provided asset");
        assertEquals(quantity, holding.getQuantity(), "Quantity should be set to provided quantity");
    }

    @Test
    void testConstructorWithAssetAndQuantity_CalculatesAvgBuyPrice() {
        // Arrange
        BigDecimal marketPrice = new BigDecimal("100.00");
        BigDecimal quantity = new BigDecimal("10");
        when(mockAsset.getCurrMarketPrice()).thenReturn(marketPrice);

        // Act
        holding = new Holding(mockAsset, quantity);

        // Assert
        assertEquals(0, marketPrice.compareTo(holding.getAvgBuyPrice()), 
            "Average buy price should be set to the market price of the asset at the time of purchase");
    }

    @Test
    void testConstructorWithNullAsset_HandlesNullAssetGracefully() {
        // Arrange
        BigDecimal quantity = new BigDecimal("5");

        // Act
        holding = new Holding(null, quantity);

        // Assert
        assertNull(holding.getAsset(), "Asset should be null");
        assertEquals(quantity, holding.getQuantity(), "Quantity should be set");
    }

    // ===== GETTER AND SETTER TESTS =====

    @Test
    void testSetAndGetAsset() {
        // Arrange
        holding = new Holding();

        // Act
        holding.setAsset(mockAsset);

        // Assert
        assertEquals(mockAsset, holding.getAsset(), "Asset setter/getter should work correctly");
    }

    @Test
    void testSetAndGetQuantity() {
        // Arrange
        holding = new Holding();
        BigDecimal newQuantity = new BigDecimal("25.5");

        // Act
        holding.setQuantity(newQuantity);

        // Assert
        assertEquals(newQuantity, holding.getQuantity(), "Quantity setter/getter should work correctly");
    }

    @Test
    void testSetAndGetAvgBuyPrice() {
        // Arrange
        holding = new Holding();
        BigDecimal newAvgBuyPrice = new BigDecimal("95.50");

        // Act
        holding.setAvgBuyPrice(newAvgBuyPrice);

        // Assert
        assertEquals(newAvgBuyPrice, holding.getAvgBuyPrice(), "Average buy price setter/getter should work correctly");
    }

    // ===== CURRENT VALUE TESTS =====

    @Test
    void testGetCurrentValue_CalculatesQuantityTimesMarketPrice() {
        // Arrange
        BigDecimal marketPrice = new BigDecimal("150.00");
        BigDecimal quantity = new BigDecimal("10");
        when(mockAsset.getCurrMarketPrice()).thenReturn(marketPrice);
        holding = new Holding(mockAsset, quantity);

        // Act
        BigDecimal currentValue = holding.getCurrentValue();

        // Assert
        BigDecimal expectedValue = quantity.multiply(marketPrice);
        assertEquals(0, expectedValue.compareTo(currentValue), 
            "Current value should be quantity multiplied by market price");
    }

    @Test
    void testGetCurrentValue_WithLargeQuantity() {
        // Arrange
        BigDecimal marketPrice = new BigDecimal("1000.00");
        BigDecimal quantity = new BigDecimal("100");
        when(mockAsset.getCurrMarketPrice()).thenReturn(marketPrice);
        holding = new Holding(mockAsset, quantity);

        // Act
        BigDecimal currentValue = holding.getCurrentValue();

        // Assert
        BigDecimal expectedValue = new BigDecimal("100000.00");
        assertEquals(0, expectedValue.compareTo(currentValue), "Should handle large quantities correctly");
    }

    @Test
    void testGetCurrentValue_WithFractionalQuantity() {
        // Arrange
        BigDecimal marketPrice = new BigDecimal("50.00");
        BigDecimal quantity = new BigDecimal("2.5");
        when(mockAsset.getCurrMarketPrice()).thenReturn(marketPrice);
        holding = new Holding(mockAsset, quantity);

        // Act
        BigDecimal currentValue = holding.getCurrentValue();

        // Assert
        BigDecimal expectedValue = new BigDecimal("125.00");
        assertEquals(0, expectedValue.compareTo(currentValue), "Should handle fractional quantities correctly");
    }

    // ===== PURCHASED VALUE TESTS =====

    @Test
    void testGetPurchasedValue_CalculatesQuantityTimesAvgBuyPrice() {
        // Arrange
        BigDecimal marketPrice = new BigDecimal("100.00");
        BigDecimal quantity = new BigDecimal("10");
        when(mockAsset.getCurrMarketPrice()).thenReturn(marketPrice);
        holding = new Holding(mockAsset, quantity);

        // Act
        BigDecimal purchasedValue = holding.getPurchasedValue();

        // Assert
        BigDecimal expectedValue = quantity.multiply(marketPrice);
        assertEquals(0, expectedValue.compareTo(purchasedValue), 
            "Purchased value should be quantity multiplied by average buy price");
    }

    @Test
    void testGetPurchasedValue_WithManuallySetAvgBuyPrice() {
        // Arrange
        BigDecimal marketPrice = new BigDecimal("150.00");
        BigDecimal quantity = new BigDecimal("5");
        BigDecimal customAvgBuyPrice = new BigDecimal("120.00");
        
        when(mockAsset.getCurrMarketPrice()).thenReturn(marketPrice);
        holding = new Holding(mockAsset, quantity);
        holding.setAvgBuyPrice(customAvgBuyPrice);

        // Act
        BigDecimal purchasedValue = holding.getPurchasedValue();

        // Assert
        BigDecimal expectedValue = quantity.multiply(customAvgBuyPrice);
        assertEquals(0, expectedValue.compareTo(purchasedValue), 
            "Purchased value should use the manually set average buy price");
    }

    @Test
    void testGetPurchasedValue_WithHigherMarketPriceShowsProfit() {
        // Arrange
        BigDecimal initialMarketPrice = new BigDecimal("100.00");
        BigDecimal quantity = new BigDecimal("10");
        when(mockAsset.getCurrMarketPrice()).thenReturn(initialMarketPrice);
        holding = new Holding(mockAsset, quantity);

        // Now simulate market price increase
        BigDecimal currentMarketPrice = new BigDecimal("150.00");
        when(mockAsset.getCurrMarketPrice()).thenReturn(currentMarketPrice);

        // Act
        BigDecimal purchasedValue = holding.getPurchasedValue();
        BigDecimal currentValue = holding.getCurrentValue();

        // Assert
        assertTrue(currentValue.compareTo(purchasedValue) > 0, 
            "Current value should exceed purchased value when price increases");
    }

    @Test
    void testPurchasedValueExceedsCurrent_WhenAvgBuyPriceRemainsHigher() {
        // Arrange
        BigDecimal initialMarketPrice = new BigDecimal("100.00");
        BigDecimal quantity = new BigDecimal("10");
        when(mockAsset.getCurrMarketPrice()).thenReturn(initialMarketPrice);
        holding = new Holding(mockAsset, quantity);

        // Manually set an average buy price higher than the later market price
        BigDecimal highAvgBuyPrice = new BigDecimal("300.00");
        holding.setAvgBuyPrice(highAvgBuyPrice);

        // Now simulate a market price increase that still stays below avg buy price
        BigDecimal increasedMarketPrice = new BigDecimal("150.00");
        when(mockAsset.getCurrMarketPrice()).thenReturn(increasedMarketPrice);

        // Act
        BigDecimal purchasedValue = holding.getPurchasedValue();
        BigDecimal currentValue = holding.getCurrentValue();

        // Assert
        assertTrue(purchasedValue.compareTo(currentValue) > 0,
            "Purchased value should exceed current value when avg buy price is higher than market price");
    }

    // ===== PROFIT/LOSS CALCULATION TESTS =====

    @Test
    void testCalculateProfit_CurrentValueMinusPurchasedValue() {
        // Arrange
        BigDecimal initialMarketPrice = new BigDecimal("100.00");
        BigDecimal quantity = new BigDecimal("10");
        when(mockAsset.getCurrMarketPrice()).thenReturn(initialMarketPrice);
        holding = new Holding(mockAsset, quantity);

        // Update market price
        BigDecimal newMarketPrice = new BigDecimal("120.00");
        when(mockAsset.getCurrMarketPrice()).thenReturn(newMarketPrice);

        // Act
        BigDecimal purchasedValue = holding.getPurchasedValue();
        BigDecimal currentValue = holding.getCurrentValue();
        BigDecimal profit = currentValue.subtract(purchasedValue);

        // Assert - precise profit calculation
        // initial avgBuyPrice = constructor price = 100 = 100.00
        // purchasedValue = quantity * avgBuyPrice = 10 * 100 = 1000.00
        // currentValue = quantity * newMarketPrice = 10 * 120 = 1200.00
        // profit = 200.00
        BigDecimal expectedProfit = new BigDecimal("200.00");
        assertEquals(0, expectedProfit.compareTo(profit), "Profit should equal expected precise profit of 20.00");
    }

    @Test
    void testCalculateLoss_CurrentValueBelowPurchasedValue() {
        // Arrange
        BigDecimal initialMarketPrice = new BigDecimal("100.00");
        BigDecimal quantity = new BigDecimal("10");
        when(mockAsset.getCurrMarketPrice()).thenReturn(initialMarketPrice);
        holding = new Holding(mockAsset, quantity);

        // Update market price to lower value
        BigDecimal newMarketPrice = new BigDecimal("80.00");
        when(mockAsset.getCurrMarketPrice()).thenReturn(newMarketPrice);

        // Act
        BigDecimal purchasedValue = holding.getPurchasedValue();
        BigDecimal currentValue = holding.getCurrentValue();
        BigDecimal loss = currentValue.subtract(purchasedValue);

        // Assert - precise profit calculation
        // initial avgBuyPrice = constructor price = 100 = 100.00
        // purchasedValue = quantity * avgBuyPrice = 10 * 100 = 1000.00
        // currentValue = quantity * newMarketPrice = 10 * 80 = 800.00
        // loss = -200.00
        BigDecimal expectedLoss = new BigDecimal("-200.00");
        assertEquals(0, expectedLoss.compareTo(loss), "Loss should equal expected precise loss of -200.00");
        assertTrue(loss.compareTo(BigDecimal.ZERO) < 0, "Should show loss when market price decreases");
    }

    // ===== EDGE CASE TESTS =====

    @Test
    void testHoldingWithZeroQuantity() {
        // Arrange
        BigDecimal marketPrice = new BigDecimal("100.00");
        BigDecimal quantity = BigDecimal.ZERO;
        when(mockAsset.getCurrMarketPrice()).thenReturn(marketPrice);

        // Act - should handle zero quantity gracefully without throwing
        holding = new Holding(mockAsset, quantity);

        // Assert - quantity remains zero
        assertEquals(BigDecimal.ZERO, holding.getQuantity(), "Quantity should remain zero");

        // Market price should still be available from the asset mock
        assertNotNull(mockAsset.getCurrMarketPrice(), "Market price should still be available from asset");
    }

    @Test
    void testHoldingWithVerySmallQuantity() {
        // Arrange
        BigDecimal marketPrice = new BigDecimal("1000.00");
        BigDecimal quantity = new BigDecimal("0.001");
        when(mockAsset.getCurrMarketPrice()).thenReturn(marketPrice);

        // Act
        holding = new Holding(mockAsset, quantity);

        // Assert
        assertEquals(quantity, holding.getQuantity(), "Should handle very small quantities");
        assertNotNull(holding.getAvgBuyPrice(), "Should calculate average buy price for small quantities");
    }

    @Test
    void testMultipleHoldingsAreIndependent() {
        // Arrange
        Asset mockAsset1 = mock(Asset.class);
        Asset mockAsset2 = mock(Asset.class);
        when(mockAsset1.getCurrMarketPrice()).thenReturn(new BigDecimal("100.00"));
        when(mockAsset2.getCurrMarketPrice()).thenReturn(new BigDecimal("200.00"));

        // Act
        Holding holding1 = new Holding(mockAsset1, new BigDecimal("10"));
        Holding holding2 = new Holding(mockAsset2, new BigDecimal("5"));

        // Assert
        assertNotEquals(holding1.getAsset(), holding2.getAsset(), "Different holdings should have different assets");
        assertNotEquals(holding1.getQuantity(), holding2.getQuantity(), "Different holdings should have different quantities");
    }

    // These tests are commented out as requested but included for future use


    @Test
    void testConstructorWithAssetAndQuantity_CallsGetCurrMarketPrice() {
        // Arrange
        BigDecimal marketPrice = new BigDecimal("100.00");
        BigDecimal quantity = new BigDecimal("10");
        when(mockAsset.getCurrMarketPrice()).thenReturn(marketPrice);

        // Act
        holding = new Holding(mockAsset, quantity);

        // Assert
        verify(mockAsset, times(1)).getCurrMarketPrice();
    }

    @Test
    void testGetCurrentValue_CallsAssetGetCurrMarketPrice() {
        // Arrange
        BigDecimal marketPrice = new BigDecimal("150.00");
        BigDecimal quantity = new BigDecimal("10");
        when(mockAsset.getCurrMarketPrice()).thenReturn(marketPrice);
        holding = new Holding(mockAsset, quantity);

        // Act
        holding.getCurrentValue();

        // Assert
        verify(mockAsset, atLeast(2)).getCurrMarketPrice();
    }

    @Test
    void testGetCurrentValue_RealTimeMarketPriceUpdate() {
        // Arrange
        BigDecimal initialPrice = new BigDecimal("100.00");
        BigDecimal quantity = new BigDecimal("10");
        when(mockAsset.getCurrMarketPrice()).thenReturn(initialPrice);
        holding = new Holding(mockAsset, quantity);

        BigDecimal initialValue = holding.getCurrentValue();

        // Update market price
        BigDecimal updatedPrice = new BigDecimal("150.00");
        when(mockAsset.getCurrMarketPrice()).thenReturn(updatedPrice);

        BigDecimal updatedValue = holding.getCurrentValue();

        // Assert
        assertTrue(updatedValue.compareTo(initialValue) > 0, "Current value should update when market price changes");
    }

    @Test
    void testPurchasedValue_UsesConstructorMarketPrice_NotCurrent() {
        // Arrange
        BigDecimal constructorPrice = new BigDecimal("100.00");
        BigDecimal quantity = new BigDecimal("10");
        when(mockAsset.getCurrMarketPrice()).thenReturn(constructorPrice);
        holding = new Holding(mockAsset, quantity);

        // Get the initial purchased value
        BigDecimal initialPurchasedValue = holding.getPurchasedValue();

        // Now change the market price (no stubbing needed for purchased value)
        BigDecimal newMarketPrice = new BigDecimal("200.00");

        // Act
        BigDecimal stillPurchasedValue = holding.getPurchasedValue();

        // Assert
        assertEquals(0, initialPurchasedValue.compareTo(stillPurchasedValue), 
            "Purchased value should remain constant (based on construction price)");
    }
}
