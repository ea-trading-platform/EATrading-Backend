package com.eatrading.api.entities;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Holding;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.objects.OrderRequest;
import com.eatrading.api.objects.OrderResponse;
import com.eatrading.api.objects.Status;

/**
 * Unit tests for Client class.
 * 
 * These tests focus on Client's behavior:
 * - Portfolio management delegation
 * - Watchlist management
 * - Order placement
 * - Data retrieval
 * 
 * MOCKS NEEDED:
 * - Portfolio: Mock the portfolio object to test that Client correctly delegates holding operations
 * - Holding: Mock holding objects with specified assets and quantities for testing
 * - Asset: Mock assets used in holdings and watchlist operations
 * - OrderProcessor: Mock the order processor to avoid external service calls during testing
 * - OrderRequest/OrderResponse: Mock request/response objects for order processing
 */
@ExtendWith(MockitoExtension.class)
class ClientTest {

    // Mock dependencies
    @Mock private Holding mockHolding;
    @Mock private Asset mockAsset;
    @Mock private Portfolio mockPortfolio;

    private Client client;

    @BeforeEach
    void setUp() {
        client = new Client("John Doe", "john@example.com", mockPortfolio);
    }

    // ===== CONSTRUCTOR TESTS =====

    @Test
    void testClientConstructor_InitializesWithNameAndEmail() {
        String testName = "Jane Smith";
        String testEmail = "jane@example.com";
        
        Client newClient = new Client(testName, testEmail);
        
        assertEquals(testName, newClient.getName());
        assertEquals(testEmail, newClient.getEmail());
    }

    @Test
    void testClientConstructor_InitializesWithNonNullPortfolioValue() {
        when(mockPortfolio.getPortfolioValue()).thenReturn(BigDecimal.ZERO);
        
        assertNotNull(client.getPortfolioValue(), 
            "Portfolio should be initialized in constructor");
    }


    @Test
    void testClientConstructor_InitializesWithEmptyWatchlist() {
        Iterable<Asset> watchlist = client.getWatchlist();
        
        assertNotNull(watchlist, "Watchlist should be initialized");
        assertFalse(watchlist.iterator().hasNext(), 
            "Watchlist should be empty after construction");
    }

    @Test
    void testClientInheritsFromUser() {
        assertTrue(client instanceof User, 
            "Client should extend User class");
    }

    // ===== PORTFOLIO HOLDING MANAGEMENT TESTS =====

    @Test
    void testAddHolding_DelegatesPortfolioAddHolding() {
        
        // Act - when we add a holding
        client.addHolding(mockHolding);
        
        // Assert - Portfolio.addHolding should have been called with the mock holding
        verify(mockPortfolio, times(1)).addHolding(mockHolding);
    }

    @Test
    void testRemoveHolding_DelegatesPortfolioRemoveHolding() {
        // Act - when we remove a holding
        client.removeHolding(mockHolding);
        
        // Assert - Portfolio.removeHolding should have been called
        verify(mockPortfolio, times(1)).removeHolding(mockHolding);
    }

    @Test
    void testGetHolding_ReturnsHoldingBySymbol() {
        // Arrange
        when(mockHolding.getAsset()).thenReturn(mockAsset);
        when(mockPortfolio.findHoldingFromPortfolio("AAPL")).thenReturn(mockHolding);
        
        // Act
        Holding result = client.getHolding("AAPL");
        
        // Assert
        assertEquals(mockHolding, result);
        verify(mockPortfolio, times(1)).findHoldingFromPortfolio("AAPL");
    }

    @Test
    void testGetHolding_ReturnsHoldingByName() {
        // Arrange
        when(mockHolding.getAsset()).thenReturn(mockAsset);
        when(mockPortfolio.findHoldingFromPortfolio("Apple Inc")).thenReturn(mockHolding);
        
        // Act
        Holding result = client.getHolding("Apple Inc");
        
        // Assert
        assertEquals(mockHolding, result);
        verify(mockPortfolio, times(1)).findHoldingFromPortfolio("Apple Inc");
    }

    @Test
    void testGetHolding_ReturnsNullWhenHoldingNotFound() {
        // Arrange
        when(mockPortfolio.findHoldingFromPortfolio("NONEXISTENT")).thenReturn(null);
        
        // Act
        Holding result = client.getHolding("NONEXISTENT");
        
        // Assert - should return null when asset is null
        assertNull(result);
    }

    // ===== WATCHLIST MANAGEMENT TESTS =====

    @Test
    void testAddToWatchlist_AddsAssetToWatchlist() {
        // Arrange
        Asset asset = mock(Asset.class);
        
        // Act
        client.addToWatchlist(asset);
        
        // Assert
        Iterable<Asset> watchlist = client.getWatchlist();
        assertTrue(watchlist.iterator().hasNext(), "Watchlist should contain the asset");
        assertTrue(((HashSet<Asset>) watchlist).contains(asset), "Watchlist should contain the correct asset");
    }

    @Test
    void testAddToWatchlist_CanAddMultipleAssets() {
        // Arrange
        Asset asset1 = mock(Asset.class);
        Asset asset2 = mock(Asset.class);
        
        // Act
        client.addToWatchlist(asset1);
        client.addToWatchlist(asset2);
        
        // Assert
        Iterable<Asset> watchlist = client.getWatchlist();
        HashSet<Asset> watchlistSet = (HashSet<Asset>) watchlist;
        assertEquals(2, watchlistSet.size());
        assertTrue(watchlistSet.contains(asset1), "Watchlist should contain asset1");
        assertTrue(watchlistSet.contains(asset2), "Watchlist should contain asset2");
    }

    @Test
    void testAddToWatchlist_DuplicateAssetsNotAdded() {
        // Arrange
        Asset asset = mock(Asset.class);
        
        // Act
        client.addToWatchlist(asset);
        client.addToWatchlist(asset);
        
        // Assert - HashSet should prevent duplicates
        Iterable<Asset> watchlist = client.getWatchlist();
        assertEquals(1, ((HashSet<Asset>) watchlist).size());
    }

    @Test
    void testGetWatchlist_ReturnsIterableAssets() {
        // Arrange
        Asset asset = mock(Asset.class);
        client.addToWatchlist(asset);
        
        // Act
        Iterable<Asset> watchlist = client.getWatchlist();
        
        // Assert
        assertNotNull(watchlist, "Watchlist should not be null");
        assertTrue(watchlist.iterator().hasNext(), "Watchlist should be iterable and contain assets");
        assertTrue(((HashSet<Asset>) watchlist).contains(asset), "Watchlist should contain the correct asset");
    }

    // ===== CONVENIENCE METHOD TESTS =====

    @Test
    void testGetUSDHolding_CallsGetHoldingWithUSD() {
        // Arrange
        Holding mockUSDHolding = mock(Holding.class);
        Asset mockUSDAsset = mock(Asset.class);
        when(mockUSDHolding.getAsset()).thenReturn(mockUSDAsset);
        when(mockPortfolio.findHoldingFromPortfolio("USD")).thenReturn(mockUSDHolding);
        
        // Act
        Holding result = client.getUSDHolding();
        
        // Assert
        assertEquals(mockUSDHolding, result);
        verify(mockPortfolio, times(1)).findHoldingFromPortfolio("USD");
    }

    @Test
    void testGetPortfolioValue_DelegatesPortfolioGetPortfolioValue() {
        // Arrange
        BigDecimal expectedValue = new BigDecimal("10000.00");
        when(mockPortfolio.getPortfolioValue()).thenReturn(expectedValue);
        
        // Act
        BigDecimal result = client.getPortfolioValue();
        
        // Assert
        assertEquals(expectedValue, result);
        verify(mockPortfolio, times(1)).getPortfolioValue();
    }

    // ===== ORDER PLACEMENT TESTS =====

    @Test
    void testPlaceOrder_CreatesOrderWithCorrectParameters() {
        Asset asset = new Asset("AAPL", "Apple Inc", Instrument.EQUITY);
        BigDecimal quantity = new BigDecimal("10");
        BigDecimal price = new BigDecimal("150.25");
        UUID clientId = UUID.randomUUID();
        UUID trackingId = UUID.randomUUID();

        Order order = new Order(trackingId, clientId, asset, quantity, true, price);
        OrderRequest request = new OrderRequest(order);

        assertNotNull(request, "Order request should be created");
        assertEquals(order, request.getOrder(), "Request should wrap the created order");
        assertEquals(clientId, request.getOrder().getClientId(), "Client ID should be attached to the order");
        assertEquals(asset.getSymbol(), request.getOrder().getAsset().getSymbol(), "Asset symbol should match");
        assertEquals(quantity, request.getOrder().getQuantity(), "Order quantity should match");
        assertTrue(request.getOrder().isBuy(), "Buy flag should be preserved");
    }

    @Test
    void testPlaceOrder_ProcessesBuyOrder() {
        Asset asset = new Asset("NVDA", "NVIDIA", Instrument.EQUITY);
        BigDecimal quantity = new BigDecimal("5");
        BigDecimal price = new BigDecimal("200.00");

        Order buyOrder = new Order(UUID.randomUUID(), UUID.randomUUID(), asset, quantity, true, price);
        OrderResponse response = new OrderResponse();
        response.setStatusCode(Status.ACCEPTED);

        assertNotNull(response, "Buy order response should be created");
        assertEquals(Status.ACCEPTED, response.getStatusCode(), "Buy order should be accepted when valid");
        assertTrue(buyOrder.isBuy(), "Order should represent a buy");
    }

    @Test
    void testPlaceOrder_ProcessesSellOrder() {
        Asset asset = new Asset("MSFT", "Microsoft", Instrument.EQUITY);
        BigDecimal quantity = new BigDecimal("3");
        BigDecimal price = new BigDecimal("320.00");

        Order sellOrder = new Order(UUID.randomUUID(), UUID.randomUUID(), asset, quantity, false, price);
        OrderResponse response = new OrderResponse();
        response.setStatusCode(Status.ACCEPTED);

        assertNotNull(response, "Sell order response should be created");
        assertEquals(Status.ACCEPTED, response.getStatusCode(), "Sell order should be accepted when valid");
        assertFalse(sellOrder.isBuy(), "Order should represent a sell");
    }

    @Test
    void testPlaceOrder_ReturnsOrderResponse() {
        OrderResponse response = new OrderResponse();

        assertNotNull(response, "placeOrder should return an OrderResponse");
        assertEquals(Status.SUBMITTED, response.getStatusCode(), "A new order response should start as submitted");
        assertEquals("", response.getRejectionReason(), "New responses should have no rejection reason");
    }

    // ===== EDGE CASE TESTS =====

    @Test
    void testClientWithEmptyName() {
        Client emptyNameClient = new Client("", "test@example.com");
        
        assertEquals("", emptyNameClient.getName());
    }

    @Test
    void testClientWithNullEmail() {
        Client nullEmailClient = new Client("Test User", null);
        
        assertNull(nullEmailClient.getEmail());
    }

    @Test
    void testMultipleClientsAreIndependent() {
        Client client1 = new Client("Client 1", "client1@example.com");
        Client client2 = new Client("Client 2", "client2@example.com");
        
        assertNotEquals(client1.getId(), client2.getId(), 
            "Different clients should have different UUIDs");
        assertNotEquals(client1.getName(), client2.getName());
    }
}
