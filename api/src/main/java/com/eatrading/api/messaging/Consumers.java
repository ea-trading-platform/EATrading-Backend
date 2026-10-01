package com.eatrading.api.messaging;

import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.eatrading.api.entities.Order;
import com.eatrading.api.objects.OrderResponse;
import com.eatrading.api.objects.Status;
import com.eatrading.api.objects.Asset;
import com.eatrading.api.objects.Instrument;
import com.eatrading.api.repository.OrderRepository;
import com.eatrading.api.services.OrderProcessor;
import com.eatrading.api.dto.OrderTransactionRequest;

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
	public void consumeIncoming(OrderTransactionRequest request) {

		String clientIdString = request.getClientId();
		UUID clientId = UUID.fromString(clientIdString);
		String transactionType = request.getTransactionType();
		boolean isBuy = "BUY".equalsIgnoreCase(transactionType);
		Asset asset = new Asset(request.getSymbol().toUpperCase(), request.getName().toUpperCase(), Instrument.EQUITY);
		UUID trackingId = request.getTrackingId();

		Order order = new Order(trackingId, clientId, asset, request.getQuantity(), isBuy);
		orderRepository.save(order);
		producers.addToValidationQueue(request);
	}

	@KafkaListener(topics = "order.unvalidated", groupId = "eatrading-order-validation")
	public void consumeValidate(OrderTransactionRequest request) {
		Order order = orderRepository.findByTrackingId(request.getTrackingId());

		if (Objects.nonNull(order)) {
			logger.warn("Order not found for validation: Tracking number {}", order.getTrackingId());
			return;
		}

		OrderResponse validationResponse = orderProcessor.validate(order);
		Status validationStatus = validationResponse.getStatusCode();

		order.setStatus(validationStatus);
		orderRepository.save(order);

		if (Status.ACCEPTED.equals(validationStatus)) {
			producers.addToExecutionQueue(request);
			logger.info("Order accepted and sent to execute queue: Tracking number {}", order.getTrackingId());
		} else {
			logger.info("Order rejected during validation: Tracking number {}", order.getTrackingId());
		}
	}

	@KafkaListener(topics = "order.unexecuted", groupId = "eatrading-order-execution")
	public void consumeExecute(OrderTransactionRequest request) {
		Order order = orderRepository.findByTrackingId(request.getTrackingId());

		if (Objects.nonNull(order)) {
			logger.warn("Order not found for validation: Tracking number {}", order.getTrackingId());
			return;
		}

		if (!Status.ACCEPTED.equals(order.getCurrentStatus())) {
			logger.warn("Skipping execution for non-accepted order (tracking number {}) with status {}", order.getTrackingId(), order.getCurrentStatus());
			return;
		}

		OrderResponse executionResponse = orderProcessor.executeOrder(order);
		Status executionStatus = executionResponse.getStatusCode();

		order.setStatus(executionStatus);
		orderRepository.save(order);
		logger.info("Order execution completed with status {} for order tracking number {}", executionStatus, order.getTrackingId());
	}
}
