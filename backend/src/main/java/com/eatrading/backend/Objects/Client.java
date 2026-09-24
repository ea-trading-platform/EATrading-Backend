package com.eatrading.backend.Objects;

import java.math.BigDecimal;
import java.util.*;

import com.eatrading.backend.Services.OrderProcessor;

public class Client extends User {

    private final Portfolio portfolio;
    private final HashSet<Asset> watchlist;

    public Client(String name, String email) {
        super(name, email);
        this.portfolio = new Portfolio(); 
        this.watchlist = new HashSet<Asset>();
    }

    public void addHolding(Holding holding) {
        this.portfolio.addHolding(holding);
    }
    public void removeHolding(Holding holding) {
        this.portfolio.removeHolding(holding);
    }

    public Holding getHolding(String holdingKey) {
        Holding result = portfolio.findHoldingFromPortfolio(holdingKey);
        if (result.getAsset() != null) {
            return result;
        }

        return null;
    }

    public void addToWatchlist(Asset asset) {
        watchlist.add(asset);
    }

    public Iterable<Asset> getWatchlist() {
        return this.watchlist;
    }

    public Holding getUSDHolding() {
        return this.getHolding("USD");
    }

    public BigDecimal getPortfolioValue() {
        return portfolio.getPortfolioValue();
    }

    // Make order detail object
    public OrderResponse placeOrder(Asset asset, BigDecimal quantity, 
        boolean isBuy) {
        Order order = new Order(super.getId(), asset, quantity, isBuy);
        // Order Srvice:
        OrderRequest req = new OrderRequest(order);
        OrderProcessor processor = new OrderProcessor();
        OrderResponse resp = processor.process(req);

        // update status of order
        // end order service

        return resp;
    }

}
