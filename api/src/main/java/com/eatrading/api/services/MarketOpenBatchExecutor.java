package com.eatrading.api.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(MarketOpenBatchExecutor.class);

    private final Producers producers;
    private final OrderRepository orderRepo;
    private final QuoteService quoteService;

    public MarketOpenBatchExecutor(Producers producers, OrderRepository orderRepo, QuoteService quoteService) {
        this.orderRepo = orderRepo;
        this.producers = producers;
        this.quoteService = quoteService;
    }

    @Scheduled(cron = "0 30 9 * * MON-FRI", zone = "America/New_York")
    public void processAfterHoursOrdersUS() {
        List<Order> orders = orderRepo.findByCurrentStatusAndBaseMarket(Status.SUBMITTED, BaseMarket.US);
        for (Order order : orders) {
            if (!refreshOrderPrice(order)) {
                continue;
            }
            OrderTransactionRequest request = orderTransactionRequestFromOrder(order);
            producers.addToValidationQueue(request);
        }
    }

    
    @Scheduled(cron = "0 15 9 * * MON-FRI", zone = "Asia/Kolkata")
    public void processAfterHoursOrdersIN() {
        List<Order> orders = orderRepo.findByCurrentStatusAndBaseMarket(Status.SUBMITTED, BaseMarket.IN);
        for (Order order : orders) {
            if (!refreshOrderPrice(order)) {
                continue;
            }
            OrderTransactionRequest request = orderTransactionRequestFromOrder(order);
            producers.addToValidationQueue(request);
        }
    }

    private boolean refreshOrderPrice(Order order) {
        String symbol = order.getAsset() == null ? null : order.getAsset().getSymbol();
        if (symbol == null || symbol.isBlank()) {
            logger.warn("Skipping queued order {} due to missing asset symbol", order.getTrackingId());
            return false;
        }

        try {
            order.setPrice(java.math.BigDecimal.valueOf(quoteService.getCurrentPrice(symbol)));
            orderRepo.save(order);
            logger.info("Refreshed queued order {} price to live quote for symbol {}", order.getTrackingId(), symbol);
            return true;
        } catch (Exception exception) {
            logger.error("Failed to refresh live price for queued order {} symbol {}: {}",
                    order.getTrackingId(), symbol, exception.getMessage(), exception);
            return false;
        }
    }

    private OrderTransactionRequest orderTransactionRequestFromOrder(Order order) {
        OrderTransactionRequest request = new OrderTransactionRequest();
        request.setTrackingId(order.getTrackingId());
        request.setClientId(order.getClientId().toString());
        request.setSymbol(order.getAsset().getSymbol());
        request.setName(order.getAsset().getName());
        request.setInstrument(order.getAsset().getInstrument());
        request.setTransactionType(order.isBuy() ? "BUY" : "SELL");
        request.setQuantity(order.getQuantity());

        return request;
    }
}
