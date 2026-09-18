package com.eatrading.backend;

import java.util.*;
import java.math.BigDecimal;

public class Portfolio {
    private Set<Holding> holdings;
    private BigDecimal totalValue;

    public Portfolio() {
        this.holdings = new HashSet<Holding>();
    }

    // Friday deliverable
    public Holding getHolding(String holdingKey) {
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

    public void addStockHolding(String holdingKey) {
        
    }

    // end friday deliverable

    public void updateHolding(Holding holding) {
        //update hildings
        //udpate balance
    }

    public BigDecimal getPortfolioValue() {
        return totalValue;
    }
}
