package com.ecommerce.orderservice.kafka;

import com.ecommerce.orderservice.dto.OrderCancelledEvent;
import com.ecommerce.orderservice.dto.OrderCompletedEvent;
import com.ecommerce.orderservice.dto.OrderCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;


    public OrderEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreatedEvent(String username, Long orderId) {
        OrderCreatedEvent event = new OrderCreatedEvent(username, orderId);
        kafkaTemplate.send("ORDER_CREATED", username, event);
    }

    public void publishOrderCompletedEvent(String username, Long orderId) {
        OrderCompletedEvent event = new OrderCompletedEvent(username, orderId);
        kafkaTemplate.send("ORDER_EVENTS", username, event);
        System.out.println("Published ORDER_COMPLETED event for order: " + orderId);
    }

    public void publishOrderCancelledEvent(String username, Long orderId, String reason) {
        OrderCancelledEvent event = new OrderCancelledEvent(username, orderId, reason);
        kafkaTemplate.send("ORDER_EVENTS", username, event);
        System.out.println("Published ORDER_CANCELLED event for order: " + orderId + ", reason: " + reason);
    }
}