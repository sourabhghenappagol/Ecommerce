package com.ecommerce.cartservice.kafka;

import com.ecommerce.cartservice.service.CartService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final CartService cartService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderEventConsumer(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     *  UPDATED: Now listens to ORDER_EVENTS topic instead of ORDER_CREATED
     * Handles:
     * - ORDER_COMPLETED: Clear cart (payment successful)
     * - ORDER_CANCELLED: Keep cart intact (payment failed, user can retry)
     */
    @KafkaListener(topics = "ORDER_EVENTS", groupId = "cart-service-group")
    public void consumeOrderEvent(String payload) {
        log.info("Received ORDER_EVENTS payload: {}", payload);

        if (payload == null || payload.trim().isEmpty()) {
            log.warn("Received empty ORDER_EVENTS payload, ignoring");
            return;
        }

        try {
            // Parse JSON to determine event type
            JsonNode rootNode = objectMapper.readTree(payload);
            String eventType = rootNode.path("eventType").asText();

            if (eventType == null || eventType.isEmpty()) {
                log.warn("Event type is missing in payload: {}", payload);
                return;
            }

            log.info("Processing event type: {}", eventType);

            // Route to appropriate handler based on event type
            switch (eventType) {
                case "ORDER_COMPLETED":
                    handleOrderCompleted(rootNode);
                    break;

                case "ORDER_CANCELLED":
                    handleOrderCancelled(rootNode);
                    break;

                default:
                    log.warn("Unknown event type '{}', ignoring", eventType);
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to parse ORDER_EVENTS payload: {}", payload, e);
        } catch (Exception e) {
            log.error("Unexpected error processing ORDER_EVENTS", e);
        }
    }

    /**
     * Handle ORDER_COMPLETED event
     * Payment successful → Clear cart
     */
    private void handleOrderCompleted(JsonNode eventNode) {
        try {
            String username = eventNode.path("username").asText();
            Long orderId = eventNode.path("orderId").asLong();

            if (username == null || username.isEmpty()) {
                log.warn("ORDER_COMPLETED event missing username: {}", eventNode);
                return;
            }

            log.info("ORDER_COMPLETED: orderId={}, username={}", orderId, username);

            // Clear cart for user (payment was successful)
            cartService.clearCart(username);
            log.info("Successfully cleared cart for user '{}' after order completion", username);

        } catch (Exception ex) {
            log.error("Failed to handle ORDER_COMPLETED event", ex);
        }
    }

    /**
     * Handle ORDER_CANCELLED event
     * Payment failed → Keep cart intact (user can retry)
     */
    private void handleOrderCancelled(JsonNode eventNode) {
        try {
            String username = eventNode.path("username").asText();
            Long orderId = eventNode.path("orderId").asLong();
            String reason = eventNode.path("reason").asText();

            log.info("ORDER_CANCELLED: orderId={}, username={}, reason={}",
                    orderId, username, reason);

            // Do NOT clear cart - user should be able to retry
            log.info("Keeping cart intact for user '{}' - order cancelled due to: {}",
                    username, reason);

            // Optional: You could send a notification to the user here
            // notificationService.sendOrderCancellationEmail(username, orderId, reason);

        } catch (Exception ex) {
            log.error("Failed to handle ORDER_CANCELLED event", ex);
        }
    }

    // Remove this after migration is complete
    @KafkaListener(topics = "ORDER_CREATED", groupId = "cart-service-group")
    public void consumeLegacyOrderCreated(String payload) {
        log.warn("Received legacy ORDER_CREATED event. This listener will be deprecated.");
        log.info("Legacy ORDER_CREATED payload: {}", payload);

        // For now, treat it like ORDER_COMPLETED (clear cart)
        try {
            JsonNode eventNode = objectMapper.readTree(payload);
            handleOrderCompleted(eventNode);
        } catch (Exception e) {
            log.error("Failed to process legacy ORDER_CREATED event", e);
        }
    }
}