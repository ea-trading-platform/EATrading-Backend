package com.eatrading.api.messaging;

import java.util.Objects;
import java.util.UUID;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class Producers {

    private final KafkaTemplate<String, String> kafka;

    public Producers(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    public void validate(UUID orderId) {
        UUID id = Objects.requireNonNull(orderId, "orderId must not be null");
        kafka.send("order.validate", id.toString());
    }

    public void execute(UUID orderId) {
        UUID id = Objects.requireNonNull(orderId, "orderId must not be null");
        kafka.send("order.execute", id.toString());
    }
}
