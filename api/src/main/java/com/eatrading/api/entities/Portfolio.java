package com.eatrading.api.entities;

import java.util.*;
import java.math.BigDecimal;
import jakarta.persistence.*;
import java.util.UUID;

import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Holding;

@Entity
@Table(name = "portfolio")
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "portfolio_holdings", joinColumns = @JoinColumn(name = "portfolio_id"))
    private Set<Holding> holdings;
    
    private BigDecimal totalValue; // recalculate on update

    public Portfolio() {
        this.holdings = new HashSet<Holding>();
        this.totalValue = BigDecimal.ZERO;
    }

    public BigDecimal getPortfolioValue() {
        return this.totalValue;
    }

    public Set<Holding> getHoldings() {
        return this.holdings;
    }

    public Holding findHoldingFromPortfolio(String holdingKey) {
        for (Holding holding : holdings) {
            Asset asset = holding.getAsset();
            if (asset.getSymbol().equals(holdingKey) || 
                    asset.getName().equals(holdingKey)) {
                return holding;
            }
        }
        return null;
    }

    public Holding addHolding(Holding newHolding) {
        Asset asset = newHolding.getAsset();
        Holding current = this.findHoldingFromPortfolio(asset.getSymbol());

        if (current.getAsset() == null) {
            this.holdings.add(newHolding);
        } else {
            BigDecimal totalShares = current.getQuantity().add(newHolding.getQuantity());
            BigDecimal oldValue = current.getPurchasedValue();
            BigDecimal newValue = newHolding.getPurchasedValue();
            current.setAvgBuyPrice(oldValue.add(newValue).divide(totalShares));
            current.setQuantity(totalShares);
        }

        totalValue = totalValue.add(newHolding.getPurchasedValue());    
        return current;
    }

    public Holding removeHolding(Holding removedHolding) {
        Asset asset = removedHolding.getAsset();
        Holding current = this.findHoldingFromPortfolio(asset.getSymbol());

        if (current.getAsset() == null) {
            return null;
        }

        current.setQuantity(current.getQuantity().subtract(removedHolding.getQuantity()));
        this.totalValue = this.totalValue.subtract(removedHolding.getPurchasedValue());
        if (current.getQuantity().compareTo(BigDecimal.valueOf(0)) == 0) {
            this.holdings.remove(current);
        }

        return current;
    }
}