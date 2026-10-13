package com.eatrading.api.services;

import java.util.List;

import org.springframework.stereotype.Service;

import org.springframework.scheduling.annotation.Scheduled;

import com.eatrading.api.objects.BaseMarket;
import com.eatrading.api.objects.Status;
import com.eatrading.api.dto.OrderTransactionRequest;
import com.eatrading.api.entities.Order;
import com.eatrading.api.messaging.Producers;
import com.eatrading.api.repository.OrderRepository;

@Service
public class MarketOpenBatchExecutor {    
    private final Producers producers;
    private  OrderRepository orderRepo;

    public MarketOpenBatchExecutor(Producers producers, OrderRepository orderRepo) {
        this.orderRepo = orderRepo;
        this.producers = producers;
    }

    @Scheduled(cron = "0 30 9 * * MON-FRI", zone = "America/New_York")
    public void processAfterHoursOrdersUS() {
        List<Order> orders = orderRepo.findByCurrentStatusAndBaseMarket(Status.SUBMITTED, BaseMarket.US);
        for (Order order : orders) {
            OrderTransactionRequest request = orderTransactionRequestFromOrder(order);
            producers.addToValidationQueue(request);
        }
    }

    
    @Scheduled(cron = "0 15 9 * * MON-FRI", zone = "Asia/Kolkata")
    public void processAfterHoursOrdersIN() {
        List<Order> orders = orderRepo.findByCurrentStatusAndBaseMarket(Status.SUBMITTED, BaseMarket.IN);
        for (Order order : orders) {
            OrderTransactionRequest request = orderTransactionRequestFromOrder(order);
            producers.addToValidationQueue(request);
        }
    }

    private OrderTransactionRequest orderTransactionRequestFromOrder(Order order) {
        OrderTransactionRequest request = new OrderTransactionRequest();
        request.setTrackingId(order.getTrackingId());
        request.setClientId(order.getClientId().toString());
        request.setSymbol(order.getAsset().getSymbol());
        request.setName(order.getAsset().getName());
        request.setTransactionType(order.isBuy() ? "BUY" : "SELL");
        request.setQuantity(order.getQuantity());

        return request;
    }
}
