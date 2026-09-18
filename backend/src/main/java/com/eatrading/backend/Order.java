package com.eatrading.backend;

import java.util.*;
import java.math.BigDecimal;

public class Order {
    // private UUID orderId;
    private Client client;
    private boolean isBuy;
    private Date orderDate;
    private BigDecimal price;
    private BigDecimal quantity;
    private Asset asset;
    private Map<Status, Date> statusChangeLog;

    public Order(Client client, Asset asset, BigDecimal quantity,
     boolean isBuy) {
        this.client = client;
        this.asset = asset;
        this.quantity = quantity;
        this.isBuy = isBuy;
        // this.orderId = UUID.randomUUID();
        this.orderDate = new Date();
        this.price = this.asset.getCurrMarketPrice();
        this.statusChangeLog = new HashMap<Status, Date>();
        statusChangeLog.put(Status.SUBMITTED, new Date());
        // add to database
    }



}
