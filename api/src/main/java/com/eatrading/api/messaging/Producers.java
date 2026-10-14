package com.eatrading.api.messaging;

import java.util.Objects;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.eatrading.api.dto.OrderTransactionRequest;

@Service
public class Producers {

    private static final String REQUEST_NOT_NULL_MSG = "request must not be null";
    
    private final KafkaTemplate<String, OrderTransactionRequest> kafka;

    public Producers(KafkaTemplate<String, OrderTransactionRequest> kafka) {
        this.kafka = kafka;
    }

    public void addToIncomingQueue(OrderTransactionRequest request) {
        OrderTransactionRequest req = Objects.requireNonNull(request, REQUEST_NOT_NULL_MSG);
        kafka.send("order.incoming", req);
    }

    public void addToValidationQueue(OrderTransactionRequest request) {
        OrderTransactionRequest req = Objects.requireNonNull(request, REQUEST_NOT_NULL_MSG);
        kafka.send("order.unvalidated", req);
    }

    public void addToExecutionQueue(OrderTransactionRequest request) {
        OrderTransactionRequest req = Objects.requireNonNull(request, REQUEST_NOT_NULL_MSG);
        kafka.send("order.unexecuted", req);
    }
}
