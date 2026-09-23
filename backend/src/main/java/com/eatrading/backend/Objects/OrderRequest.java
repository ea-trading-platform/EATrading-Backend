package com.eatrading.backend.Objects;

public class OrderRequest {
    private Order order;

    public OrderRequest(Order order) {
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }
}
