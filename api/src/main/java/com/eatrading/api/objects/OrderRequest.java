package com.eatrading.api.objects;

import com.eatrading.api.entities.Order;

public class OrderRequest {
    private Order order;

    public OrderRequest(Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }
}
