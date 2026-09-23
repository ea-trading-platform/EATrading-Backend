package com.eatrading.backend.Objects;

import java.util.*;
import java.math.BigDecimal;

public class Portfolio {
    private Set<Holding> holdings;
    private BigDecimal totalValue; // recalculate on update

    public Portfolio() {
        this.holdings = new HashSet<Holding>();
    }

    public BigDecimal getPortfolioValue() {
        return this.totalValue;
    }

    public Holding findHoldingFromPortfolio(String holdingKey) {
        Holding target = new Holding(null, new BigDecimal(0));
        for (Holding holding : holdings) {
            Asset asset = holding.getAsset();
            if (asset.getSymbol().equals(holdingKey) || 
                    asset.getName().equals(holdingKey)) {
                target = holding;
                break;
            }
        }
        return target;
    }

    public void addStockHolding(Holding newHolding, String holdingKey) {
        Holding current = this.findHoldingFromPortfolio(holdingKey);
        if (current.getAsset() == null) {
            this.holdings.add(newHolding);
        }
        else {
            BigDecimal totalShares = current.getQuantity().add(newHolding.getQuantity());
            BigDecimal oldValue = current.getPurchasedValue();
            BigDecimal newValue = newHolding.getPurchasedValue();
            current.setAvgBuyPrice(oldValue.add(newValue).divide(totalShares));
            current.setQuantity(totalShares);
        }

        totalValue = totalValue.add(newHolding.getPurchasedValue());    
    }


}
