package com.eatrading.api.services;

import java.math.BigDecimal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eatrading.api.dto.Quote;
import com.eatrading.api.entities.Client;
import com.eatrading.api.entities.Order;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Holding;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.objects.OrderResponse;
import com.eatrading.api.objects.Status;
import com.eatrading.api.repository.ClientRepository;

@Service
public class OrderProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderProcessor.class);
    private static final double MAX_PRICE_VARIANCE_PERCENT = 5.0; // 5% variance tolerance
    
    private final ClientRepository clientRepository;
    private final QuoteService quoteService;

    public OrderProcessor(ClientRepository clientRepository, QuoteService quoteService) {
        this.clientRepository = clientRepository;
        this.quoteService = quoteService;
    }

    public OrderResponse validate(Order order) {
        OrderResponse resp = new OrderResponse();

        Quote quote = quoteService.getQuote(order.getAsset().getSymbol());
        if (quote.getMarketState().toLowerCase() != "open") {
            resp.setStatusCode(Status.SUBMITTED);
            return resp;
        }

        if (order.isBuy()) {
            resp = validateBuy(order);
        } else {
            resp = validateSell(order);
        }

        return resp;
    }

    private OrderResponse validateBuy(Order order) {
        OrderResponse resp = new OrderResponse();
        try {
            //-- 1. Check if order price is too far from market price --//
            Quote quote = quoteService.getQuote(order.getAsset().getSymbol());
            BigDecimal marketPrice = BigDecimal.valueOf(quote.getPrice());
            BigDecimal orderPrice = order.getPrice();
            
            BigDecimal variance = orderPrice.subtract(marketPrice).abs()
                    .divide(marketPrice, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
            
            if (variance.compareTo(new BigDecimal(MAX_PRICE_VARIANCE_PERCENT)) > 0) {
                logger.warn("Buy order variance too high: {} vs market {}. Variance: {}%", 
                    orderPrice, marketPrice, variance);
                resp.setStatusCode(Status.REJECTED);
                resp.setRejectionReason("Stock price variance exceeds " + MAX_PRICE_VARIANCE_PERCENT + "% of order price");
                return resp;
            }
            
            //-- 2. Validate client has sufficient USD cash holdings --//
            Optional<Client> clientOptional = clientRepository.findById(order.getClientId());
            if (!clientOptional.isPresent()) {
                logger.warn("Client not found for buy order: {}", order.getClientId());
                resp.setStatusCode(Status.REJECTED);
                resp.setRejectionReason("Client not found");
                return resp;
            }
            
            Client client = clientOptional.get();
            BigDecimal requiredCash = orderPrice.multiply(order.getQuantity());
            BigDecimal availableCash;
            
            // Find USD cash holding
            Holding cashHolding = client.getUSDHolding();
            availableCash = cashHolding.getQuantity();
    
            if (availableCash.compareTo(requiredCash) < 0) {
                logger.warn("Insufficient USD cash for buy order. Required: {}, Available: {}", 
                    requiredCash, availableCash);
                resp.setStatusCode(Status.REJECTED);
                resp.setRejectionReason("Insufficient USD cash. Required: $" + requiredCash + ", Available: $" + availableCash);
                return resp;
            }
            
            logger.info("Buy order validated. Order price: {}, Market price: {}, Variance: {}%, Required cash: {}, Available cash: {}", 
                orderPrice, marketPrice, variance, requiredCash, availableCash);
            resp.setStatusCode(Status.ACCEPTED);
        } catch (Exception e) {
            logger.error("Error validating buy order: {}", e.getMessage());
            resp.setStatusCode(Status.REJECTED);
        }
        return resp;
    }

    private OrderResponse validateSell(Order order) {
        OrderResponse resp = new OrderResponse();
        try {
            //-- 1. Check if order price is too far from market price --//
            Quote quote = quoteService.getQuote(order.getAsset().getSymbol());
            BigDecimal marketPrice = BigDecimal.valueOf(quote.getPrice());
            BigDecimal orderPrice = order.getPrice();
            
            // Only reject if market price has fallen 5%+ compared to order price
            // (orderPrice - marketPrice) / marketPrice >= 5%
            BigDecimal variance = orderPrice.subtract(marketPrice)
                    .divide(marketPrice, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100));
            
            if (variance.compareTo(new BigDecimal(MAX_PRICE_VARIANCE_PERCENT)) > 0) {
                logger.warn("Sell order rejected: market price fell below threshold. Order price: {}, Market price: {}, Fall: {}%", 
                    orderPrice, marketPrice, variance);
                resp.setStatusCode(Status.REJECTED);
                resp.setRejectionReason("Market price has fallen more than " + MAX_PRICE_VARIANCE_PERCENT + "% below order price");
                return resp;
            }
            
            //-- 2. Validate client has sufficient shares of the holding --//
            Optional<Client> clientOptional = clientRepository.findById(order.getClientId());
            if (!clientOptional.isPresent()) {
                logger.warn("Client not found for sell order: {}", order.getClientId());
                resp.setStatusCode(Status.REJECTED);
                resp.setRejectionReason("Client not found");
                return resp;
            }
            
            Client client = clientOptional.get();
            Holding holding = client.getHolding(order.getAsset().getSymbol());
            BigDecimal availableShares;
            
            if (holding == null || holding.getAsset() == null) {
                logger.warn("Client does not own holding for sell order. Asset: {}", order.getAsset().getSymbol());
                resp.setStatusCode(Status.REJECTED);
                resp.setRejectionReason("Client does not own " + order.getAsset().getSymbol());
                return resp;
            }
            
            availableShares = holding.getQuantity();
            
            if (availableShares.compareTo(order.getQuantity()) < 0) {
                logger.warn("Insufficient shares for sell order. Required: {}, Available: {}", 
                    order.getQuantity(), availableShares);
                resp.setStatusCode(Status.REJECTED);
                resp.setRejectionReason("Insufficient shares. Required: " + order.getQuantity() + ", Available: " + availableShares);
                return resp;
            }
            
            logger.info("Sell order validated. Order price: {}, Market price: {}, Variance: {}%, Required shares: {}, Available shares: {}", 
                orderPrice, marketPrice, variance, order.getQuantity(), availableShares);
            resp.setStatusCode(Status.ACCEPTED);
        } catch (Exception e) {
            logger.error("Error validating sell order: {}", e.getMessage());
            resp.setStatusCode(Status.REJECTED);
        }
        return resp;
    }

    @Transactional
    public OrderResponse executeOrder(Order order) {
        Optional<Client> clientOptional = clientRepository.findById(order.getClientId());
        
        if (!clientOptional.isPresent()) {
            OrderResponse resp = new OrderResponse();
            resp.setStatusCode(Status.REJECTED);
            return resp;
        }
        
        Client client = clientOptional.get();
        BigDecimal executionPrice = order.getPrice();
        if (executionPrice == null || executionPrice.compareTo(BigDecimal.ZERO) <= 0) {
            OrderResponse resp = new OrderResponse();
            resp.setStatusCode(Status.REJECTED);
            resp.setRejectionReason("Invalid execution price on order");
            return resp;
        }
        
        Holding newHolding = new Holding(order.getAsset(), order.getQuantity(), executionPrice);
        Asset cashAsset = new Asset("USD", "US DOLLAR", Instrument.CASH);
        Holding cashHolding = new Holding(cashAsset, newHolding.getPurchasedValue(), BigDecimal.ONE);
        
        if (order.isBuy()) {
            client.removeHolding(cashHolding);
            client.addHolding(newHolding);
        } else {
            client.removeHolding(newHolding);
            client.addHolding(cashHolding);  
        }
        
        clientRepository.save(client);
        
        OrderResponse resp = new OrderResponse();
        resp.setStatusCode(Status.FILLED);

        return resp;
    }
}
