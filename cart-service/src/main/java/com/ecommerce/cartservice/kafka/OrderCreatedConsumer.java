package com.ecommerce.cartservice.kafka;

import com.ecommerce.cartservice.dto.OrderCreatedEvent;
import com.ecommerce.cartservice.service.CartService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderCreatedConsumer {

    private final CartService cartService;

    public OrderCreatedConsumer(CartService cartService) {
        this.cartService = cartService;
    }

    @KafkaListener(topics = "ORDER_CREATED", groupId = "cart-service-group")
    public void consume(OrderCreatedEvent event) {

        String username = event.getUsername();

        // Clear cart for user
        cartService.clearCart(username);
    }
}
