package com.ecommerce.orderservice.kafka;

import com.ecommerce.orderservice.dto.OrderCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreatedEvent(String username, Long orderId) {
        OrderCreatedEvent event =
                new OrderCreatedEvent(username, orderId);

        kafkaTemplate.send("ORDER_CREATED", username, event);
    }
}
