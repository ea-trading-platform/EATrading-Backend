package com.eatrading.api.entities;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Holding;

@ExtendWith(MockitoExtension.class)
class PortfolioTest {

    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        portfolio = new Portfolio();
    }

    @Test
    void constructor_initialState() {
        assertNotNull(portfolio.getHoldings());
        assertEquals(0, portfolio.getPortfolioValue().compareTo(BigDecimal.ZERO));
        assertEquals(0, portfolio.getHoldings().size());
    }

    @Test
    void findHolding_emptyPortfolio_returnsNull() {
        assertNull(portfolio.findHoldingFromPortfolio("ANY"));
    }

    @Test
    void addHolding_newHolding_addsAndUpdatesTotalValue() {
        Asset a = mock(Asset.class);
        when(a.getSymbol()).thenReturn("TST");
        when(a.getCurrMarketPrice()).thenReturn(new BigDecimal("2.00"));

        Holding h = new Holding(a, new BigDecimal("10"));

        Holding returned = portfolio.addHolding(h);

        // When adding a new holding the portfolio should contain it
        Set<Holding> holdings = portfolio.getHoldings();
        assertEquals(1, holdings.size());
        // portfolio totalValue should increase by purchasedValue
        assertEquals(0, portfolio.getPortfolioValue().compareTo(h.getPurchasedValue()));
        // the API's contract: addHolding should return the current/updated holding
        assertNotNull(returned);
    }

    @Test
    void addHolding_existingHolding_mergesQuantities_andUpdatesAvgPrice() {
        Asset a = mock(Asset.class);
        when(a.getSymbol()).thenReturn("MER");;
        when(a.getCurrMarketPrice()).thenReturn(new BigDecimal("10.00"));

        Holding first = new Holding(a, new BigDecimal("2"));
        portfolio.addHolding(first);

        Holding second = new Holding(a, new BigDecimal("3"));
        Holding updated = portfolio.addHolding(second);


        assertEquals(1, portfolio.getHoldings().size());
        assertNotNull(updated);
        assertEquals(0, updated.getQuantity().compareTo(new BigDecimal("5")));
        // totalValue should be sum of purchased values 2*10 + 3*10
        assertEquals(0, updated.getAvgBuyPrice().compareTo(new BigDecimal("10.00")));
        assertEquals(0, portfolio.getPortfolioValue().compareTo(new BigDecimal("50.00")));
    }

    @Test
    void removeHolding_notFound_returnsNull() {
        Asset a = mock(Asset.class);
        when(a.getSymbol()).thenReturn("NOPE");;
        when(a.getCurrMarketPrice()).thenReturn(new BigDecimal("1.00"));

        Holding toRemove = new Holding(a, new BigDecimal("1"));

        Holding result = portfolio.removeHolding(toRemove);
        assertNull(result);
    }

    @Test
    void removeHolding_partialQuantity_decreasesQuantity_andTotalValue() {
        Asset a = mock(Asset.class);
        when(a.getSymbol()).thenReturn("PAR");;
        when(a.getCurrMarketPrice()).thenReturn(new BigDecimal("5.00"));

        Holding initial = new Holding(a, new BigDecimal("10"));
        portfolio.addHolding(initial);

        Holding remove = new Holding(a, new BigDecimal("4"));
        Holding after = portfolio.removeHolding(remove);

        assertNotNull(after);
        assertEquals(0, after.getQuantity().compareTo(new BigDecimal("6")));
        BigDecimal expectedTotal = (new BigDecimal("10").subtract(new BigDecimal("4"))).multiply(new BigDecimal("5.00"));
        assertEquals(0, portfolio.getPortfolioValue().compareTo(expectedTotal));
        assertTrue(portfolio.getHoldings().contains(after));
    }

    @Test
    void removeHolding_withDifferentRemovalPrice_keepsCostBasisConsistent() {
        Asset buyAsset = mock(Asset.class);
        when(buyAsset.getSymbol()).thenReturn("AAPL");
        when(buyAsset.getCurrMarketPrice()).thenReturn(new BigDecimal("100.00"));

        Holding initial = new Holding(buyAsset, new BigDecimal("10"));
        portfolio.addHolding(initial);

        Asset sellAsset = mock(Asset.class);
        when(sellAsset.getSymbol()).thenReturn("AAPL");
        when(sellAsset.getCurrMarketPrice()).thenReturn(new BigDecimal("150.00"));

        Holding removeAtDifferentPrice = new Holding(sellAsset, new BigDecimal("2"));
        Holding remaining = portfolio.removeHolding(removeAtDifferentPrice);

        assertNotNull(remaining);
        assertEquals(0, remaining.getQuantity().compareTo(new BigDecimal("8")));
        assertEquals(0, remaining.getAvgBuyPrice().compareTo(new BigDecimal("100.00")));
        assertEquals(0, portfolio.getPortfolioValue().compareTo(new BigDecimal("800.00")));
    }

    @Test
    void removeHolding_removeAll_removesHolding_andAdjustsTotalValue() {
        Asset a = mock(Asset.class);
        when(a.getSymbol()).thenReturn("ALL");
        when(a.getCurrMarketPrice()).thenReturn(new BigDecimal("3.00"));

        Holding initial = new Holding(a, new BigDecimal("5"));
        portfolio.addHolding(initial);

        Holding remove = new Holding(a, new BigDecimal("5"));
        Holding after = portfolio.removeHolding(remove);

        // after removing all, the holding should be gone
        assertNotNull(after);
        assertFalse(portfolio.getHoldings().contains(after));
        assertEquals(0, portfolio.getPortfolioValue().compareTo(BigDecimal.ZERO));
    }

    @Test
    void findHolding_matchBySymbol_andByName_and_loopCoverage() {
        Asset a1 = mock(Asset.class);
        when(a1.getSymbol()).thenReturn("AAA");
        when(a1.getName()).thenReturn("Alpha");
        when(a1.getCurrMarketPrice()).thenReturn(new BigDecimal("1.00"));

        Asset a2 = mock(Asset.class);
        when(a2.getSymbol()).thenReturn("BBB");
        when(a2.getName()).thenReturn("Beta");
        when(a2.getCurrMarketPrice()).thenReturn(new BigDecimal("1.00"));

        Asset a3 = mock(Asset.class);
        when(a3.getSymbol()).thenReturn("CCC");
        when(a3.getName()).thenReturn("Charlie");
        when(a3.getCurrMarketPrice()).thenReturn(new BigDecimal("1.00"));

        portfolio.addHolding(new Holding(a1, new BigDecimal("1")));
        portfolio.addHolding(new Holding(a2, new BigDecimal("1")));
        portfolio.addHolding(new Holding(a3, new BigDecimal("1")));

        Holding foundBySym = portfolio.findHoldingFromPortfolio("BBB");
        assertNotNull(foundBySym);
        assertEquals("BBB", foundBySym.getAsset().getSymbol());

        Holding foundByName = portfolio.findHoldingFromPortfolio("Charlie");
        assertNotNull(foundByName);
        assertEquals("CCC", foundByName.getAsset().getSymbol());
    }

    @Test
    void business_addThenRemove_roundtrip() {
        Asset a = mock(Asset.class);
        when(a.getSymbol()).thenReturn("RND");
        when(a.getCurrMarketPrice()).thenReturn(new BigDecimal("6.00"));
        Holding h1 = new Holding(a, new BigDecimal("2"));
        Holding h2 = new Holding(a, new BigDecimal("3"));

        portfolio.addHolding(h1);
        portfolio.addHolding(h2);

        // remove in different order
        portfolio.removeHolding(new Holding(a, new BigDecimal("1")));
        portfolio.removeHolding(new Holding(a, new BigDecimal("4")));

        assertEquals(0, portfolio.getPortfolioValue().compareTo(BigDecimal.ZERO));
        assertEquals(0, portfolio.getHoldings().size());
    }

    @Test
    void addTwoHoldings_sameAsset_mergesIntoSingleHolding() {
        Asset a = mock(Asset.class);
        when(a.getSymbol()).thenReturn("SAME");
        when(a.getCurrMarketPrice()).thenReturn(new BigDecimal("4.00"));

        Holding first = new Holding(a, new BigDecimal("2"));
        Holding second = new Holding(a, new BigDecimal("3"));

        portfolio.addHolding(first);
        Holding updated = portfolio.addHolding(second);

        // portfolio should contain a single holding for the asset
        assertEquals(1, portfolio.getHoldings().size());

        Holding found = portfolio.findHoldingFromPortfolio("SAME");
        assertNotNull(found);
        // quantities should be merged (2 + 3 = 5)
        assertEquals(0, found.getQuantity().compareTo(new BigDecimal("5")));

        // the stored holding should be the same instance as the first one added
        assertTrue(portfolio.getHoldings().contains(first));
        assertSame(first, found);
        assertNotNull(updated);
    }

    @Test
    void addTwoDifferentAssets_createTwoSeparateHoldings_withCorrectTotals() {
        Asset a1 = mock(Asset.class);
        when(a1.getSymbol()).thenReturn("A1");
        when(a1.getCurrMarketPrice()).thenReturn(new BigDecimal("2.50"));

        Asset a2 = mock(Asset.class);
        when(a2.getSymbol()).thenReturn("A2");
        when(a2.getCurrMarketPrice()).thenReturn(new BigDecimal("4.00"));

        Holding h1 = new Holding(a1, new BigDecimal("4")); // purchasedValue = 10.00
        Holding h2 = new Holding(a2, new BigDecimal("2")); // purchasedValue = 8.00

        portfolio.addHolding(h1);
        portfolio.addHolding(h2);

        // Should have two separate holdings
        assertEquals(2, portfolio.getHoldings().size());

        // Totals should be sum of individual purchased values
        BigDecimal expectedTotal = h1.getPurchasedValue().add(h2.getPurchasedValue());
        assertEquals(0, portfolio.getPortfolioValue().compareTo(expectedTotal));

        // Ensure each holding can be found separately
        Holding found1 = portfolio.findHoldingFromPortfolio("A1");
        Holding found2 = portfolio.findHoldingFromPortfolio("A2");
        assertNotNull(found1);
        assertNotNull(found2);
        assertEquals(0, found1.getQuantity().compareTo(new BigDecimal("4")));
        assertEquals(0, found2.getQuantity().compareTo(new BigDecimal("2")));
    }
}
