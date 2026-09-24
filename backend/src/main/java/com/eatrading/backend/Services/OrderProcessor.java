package com.eatrading.backend.Services;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.eatrading.backend.Objects.Asset;
import com.eatrading.backend.Objects.Client;
import com.eatrading.backend.Objects.Holding;
import com.eatrading.backend.Objects.Instrument;
import com.eatrading.backend.Objects.Order;
import com.eatrading.backend.Objects.OrderRequest;
import com.eatrading.backend.Objects.OrderResponse;
import com.eatrading.backend.Objects.Status;
import com.eatrading.backend.Repository.ClientRepository;

@Service
public class OrderProcessor {
    
    private final ClientRepository clientRepository;

    public OrderProcessor(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    private OrderResponse validateBuy(Order order) {
        OrderResponse resp = new OrderResponse();
        resp.setStatusCode(Status.ACCEPTED);
        return resp;
    }

    private OrderResponse validateSell(Order order) {
        OrderResponse resp = new OrderResponse();
        resp.setStatusCode(Status.ACCEPTED);
        return resp;
    }

    private OrderResponse executeOrder(Order order) {
        Optional<Client> clientOptional = clientRepository.findById(order.getClientId());
        
        if (!clientOptional.isPresent()) {
            OrderResponse resp = new OrderResponse();
            resp.setStatusCode(Status.REJECTED);
            return resp;
        }
        
        Client client = clientOptional.get();
        
        Holding newHolding = new Holding(order.getAsset(), order.getQuantity());
        Asset cashAsset = new Asset("USD", "US DOLLAR", Instrument.CASH);
        Holding cashHolding = new Holding(cashAsset, newHolding.getPurchasedValue());
        
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

    public OrderResponse process(OrderRequest request) {
        OrderResponse response;
        Order currentOrder = request.getOrder();
        currentOrder.setStatus(Status.SUBMITTED);

        if (request.getOrder().isBuy()) {
            response = validateBuy(request.getOrder());
        }
        else {
            response = validateSell(request.getOrder());
        }

        if (response.getStatusCode() == Status.REJECTED) {
            // write order to DB
            return response;
        }

        currentOrder.setStatus(Status.ACCEPTED);
        response = executeOrder(request.getOrder());

        if (response.getStatusCode() == Status.FILLED) {
            currentOrder.setStatus(Status.FILLED);
        }

        return response;
    }

}
