package com.eatrading.api.entities;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Holding;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "portfolio")
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "portfolio_holdings", joinColumns = @JoinColumn(name = "portfolio_id"))
    private Set<Holding> holdings;
    
    private BigDecimal totalValue;

    public Portfolio() {
        this.holdings = new HashSet<>();
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
            if (asset != null && (Objects.equals(asset.getSymbol(), holdingKey)
                    || Objects.equals(asset.getName(), holdingKey))) {
                return holding;
            }
        }
        return null;
    }

    public Holding addHolding(Holding newHolding) {
        Asset asset = newHolding.getAsset();
        Holding current = this.findHoldingFromPortfolio(asset.getSymbol());

        if (current == null || current.getAsset() == null) {
            this.holdings.add(newHolding);
            current = newHolding;
        } else {
            BigDecimal totalShares = current.getQuantity().add(newHolding.getQuantity());
            BigDecimal oldValue = current.getPurchasedValue();
            BigDecimal newValue = newHolding.getPurchasedValue();
            current.setAvgBuyPrice(oldValue.add(newValue).divide(totalShares, 8, java.math.RoundingMode.HALF_UP));
            current.setQuantity(totalShares);
        }

        recalculateTotalValue();
        return current;
    }

    public Holding removeHolding(Holding removedHolding) {
        Asset asset = removedHolding.getAsset();
        Holding current = this.findHoldingFromPortfolio(asset.getSymbol());

        if (current == null || current.getAsset() == null) {
            return null;
        }

        current.setQuantity(current.getQuantity().subtract(removedHolding.getQuantity()));
        if (current.getQuantity().compareTo(BigDecimal.valueOf(0)) == 0) {
            this.holdings.remove(current);
        }

        recalculateTotalValue();

        return current;
    }

    private void recalculateTotalValue() {
        BigDecimal recalculated = BigDecimal.ZERO;
        for (Holding holding : this.holdings) {
            if (holding != null && holding.getAsset() != null && holding.getQuantity() != null
                    && holding.getAvgBuyPrice() != null) {
                recalculated = recalculated.add(holding.getPurchasedValue());
            }
        }
        this.totalValue = recalculated;
    }
}