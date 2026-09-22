package com.eatrading.backend;

public class OrderProcessor {
    private OrderResponse validateBuy() {
        return new OrderResponse();
    }

    private OrderResponse validateSell() {
        return new OrderResponse();
    }

    private OrderResponse executeOrder() {
        // get price of stock
        // if buy
        //  reduce client's usd cash
        //  increase client's desired asset holding
        // else reverse
        // create trade object containing both holdings
        
        return new OrderResponse(); // add trade, update status
    }

    public OrderResponse process(OrderRequest request) {
        //
        return new OrderResponse();
    }

}
