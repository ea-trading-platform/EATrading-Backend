package com.eatrading.api.messaging;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.eatrading.api.entities.Order;
import com.eatrading.api.objects.OrderResponse;
import com.eatrading.api.objects.Status;
import com.eatrading.api.repository.OrderRepository;
import com.eatrading.api.services.OrderProcessor;

@Service
public class Consumers {

	private static final Logger logger = LoggerFactory.getLogger(Consumers.class);

	private final OrderRepository orderRepository;
	private final OrderProcessor orderProcessor;
	private final Producers producers;

	public Consumers(OrderRepository orderRepository, OrderProcessor orderProcessor, Producers producers) {
		this.orderRepository = orderRepository;
		this.orderProcessor = orderProcessor;
		this.producers = producers;
	}

	@KafkaListener(topics = "order.incoming", groupId = "eatrading-order-arrival")
	public void consumeIncoming(String orderIdMessage) {
		UUID orderId = UUID.fromString(orderIdMessage);
	}

	@KafkaListener(topics = "order.unvalidated", groupId = "eatrading-order-validation")
	public void consumeValidate(String orderIdMessage) {
		UUID orderId = UUID.fromString(orderIdMessage);
		Optional<Order> orderOptional = orderRepository.findById(orderId);

		if (!orderOptional.isPresent()) {
			logger.warn("Order not found for validation: {}", orderId);
			return;
		}

		Order order = orderOptional.get();
		OrderResponse validationResponse = orderProcessor.validate(order);
		Status validationStatus = validationResponse.getStatusCode();

		order.setStatus(validationStatus);
		orderRepository.save(order);

		if (Status.ACCEPTED.equals(validationStatus)) {
			producers.addToExecutionQueue(order.getOrderId());
			logger.info("Order accepted and sent to execute queue: {}", orderId);
		} else {
			logger.info("Order rejected during validation: {}", orderId);
		}
	}

	@KafkaListener(topics = "order.unexecuted", groupId = "eatrading-order-execution")
	public void consumeExecute(String orderIdMessage) {
		UUID orderId = UUID.fromString(orderIdMessage);
		Optional<Order> orderOptional = orderRepository.findById(orderId);

		if (!orderOptional.isPresent()) {
			logger.warn("Order not found for execution: {}", orderId);
			return;
		}

		Order order = orderOptional.get();
		if (!Status.ACCEPTED.equals(order.getCurrentStatus())) {
			logger.warn("Skipping execution for non-accepted order {} with status {}", orderId, order.getCurrentStatus());
			return;
		}

		OrderResponse executionResponse = orderProcessor.executeOrder(order);
		Status executionStatus = executionResponse.getStatusCode();

		order.setStatus(executionStatus);
		orderRepository.save(order);
		logger.info("Order execution completed with status {} for order {}", executionStatus, orderId);

	}
}
