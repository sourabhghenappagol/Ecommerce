package com.ecommerce.cartservice.kafka;

import com.ecommerce.cartservice.dto.OrderCreatedEvent;
import com.ecommerce.cartservice.service.CartService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    private final CartService cartService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderCreatedConsumer(CartService cartService) {
        this.cartService = cartService;
    }

    // Accept raw payload as String and parse manually to avoid container-level deserialization failures
    @KafkaListener(topics = "ORDER_CREATED", groupId = "cart-service-group")
    public void consume(String payload) {
        log.info("Received raw ORDER_CREATED payload: {}", payload);

        if (payload == null || payload.trim().isEmpty()) {
            log.warn("Received empty ORDER_CREATED payload, ignoring");
            return;
        }

        OrderCreatedEvent event;
        try {
            event = objectMapper.readValue(payload, OrderCreatedEvent.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize ORDER_CREATED payload to OrderCreatedEvent. Payload='{}'", payload, e);
            // Optionally: publish to a dead-letter topic or increment a metric
            return;
        }

        if (event == null || event.getUsername() == null) {
            log.warn("Deserialized OrderCreatedEvent is null or missing username: {}", event);
            return;
        }

        log.info("Processed ORDER_CREATED event: orderId={}, username={}", event.getOrderId(), event.getUsername());

        // Clear cart for user
        try {
            cartService.clearCart(event.getUsername());
            log.info("Cleared cart for user {}", event.getUsername());
        } catch (Exception ex) {
            log.error("Failed to clear cart for user {}", event.getUsername(), ex);
        }
    }
}
