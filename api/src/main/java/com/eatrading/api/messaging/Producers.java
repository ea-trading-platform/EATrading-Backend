package com.eatrading.api.messaging;

import java.util.Objects;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import com.eatrading.api.dto.OrderTransactionRequest;

@Service
public class Producers {

    private final KafkaTemplate<String, OrderTransactionRequest> kafka;

    public Producers(KafkaTemplate<String, OrderTransactionRequest> kafka) {
        this.kafka = kafka;
    }

    public void addToIncomingQueue(OrderTransactionRequest request) {
        OrderTransactionRequest req = Objects.requireNonNull(request, "request must not be null");
        kafka.send("order.incoming", req);
    }

    public void addToValidationQueue(OrderTransactionRequest request) {
        OrderTransactionRequest req = Objects.requireNonNull(request, "request must not be null");
        kafka.send("order.unvalidated", req);
    }

    public void addToExecutionQueue(OrderTransactionRequest request) {
        OrderTransactionRequest req = Objects.requireNonNull(request, "request must not be null");
        kafka.send("order.unexecuted", req);
    }
}
