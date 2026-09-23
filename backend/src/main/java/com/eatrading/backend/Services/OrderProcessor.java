package com.eatrading.backend.Services;

import java.math.BigDecimal;

import com.eatrading.backend.Objects.Asset;
import com.eatrading.backend.Objects.Holding;
import com.eatrading.backend.Objects.Instrument;
import com.eatrading.backend.Objects.Order;
import com.eatrading.backend.Objects.OrderRequest;
import com.eatrading.backend.Objects.OrderResponse;
import com.eatrading.backend.Objects.Trade;

public class OrderProcessor {
    private OrderResponse validateBuy(Order order) {
        return new OrderResponse();
    }

    private OrderResponse validateSell(Order order) {
        return new OrderResponse();
    }

    private OrderResponse executeOrder(Order order) {
        // get price of stock
        // if buy
        //  reduce client's usd cash
        //  increase client's desired asset holding

        BigDecimal stockPrice = order.getAsset().getCurrMarketPrice();
        
        Holding newHolding = new Holding(order.getAsset(), order.getQuantity());
        Asset cashAsset = new Asset("USD", "US DOLLAR", Instrument.CASH);
        Holding cashHolding = new Holding(cashAsset, newHolding.getPurchasedValue());
        Trade newTrade = new Trade(order.getOrderID(), newHolding, null);
        
        // else reverse
        // create trade object containing both holdings

        
        
        
        return new OrderResponse(); // add trade, update status
    }

    public OrderResponse process(OrderRequest request) {
        OrderResponse response;
        //status = SUBMITTED
        if (request.getOrder().isBuy()) {
            response = validateBuy(request.getOrder());
        }
        else {
            response = validateSell(request.getOrder());
        }
        //status = ACCEPTED
        //status = FILLED
        executeOrder(request.getOrder());

        return new OrderResponse();
    }

}
