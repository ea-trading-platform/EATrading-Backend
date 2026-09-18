package com.eatrading.backend;

import java.math.BigDecimal;

public class Client extends User {
    private Portfolio portfolio;

    public Client(String name, String email) {
        super(name, email);
        this.portfolio = new Portfolio(); 
    }

    // Friday deliverables
    public Holding getHolding(String holdingKey) {
        Holding result = portfolio.getHolding(holdingKey);
        if (result.getAsset() != null) {
            return result;
        }

        return null;
    }

    public Holding getUSDHolding() {
        return this.getHolding("USD");
    }

    public BigDecimal getPortfolioValue() {
        return portfolio.getPortfolioValue();
    }
    // end friday deliverables 

    // Make order detail object
    public OrderResponse placeOrder(Asset asset, BigDecimal quantity, 
        boolean isBuy) {
        Order order = new Order(this, asset, quantity, isBuy);
        // Order Srvice:
        OrderRequest req = new OrderRequest(order);
        OrderProcessor processor = new OrderProcessor();
        OrderResponse resp = processor.process(req);

        // update status of order
        // end order service

        return resp;
    }

}
