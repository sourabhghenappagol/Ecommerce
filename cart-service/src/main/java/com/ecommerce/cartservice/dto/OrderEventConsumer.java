package com.ecommerce.cartservice.dto;

public class OrderEventConsumer {

    private String username;
    private Long orderId;

    public OrderEventConsumer() {}

    public OrderEventConsumer(String username, Long orderId) {
        this.username = username;
        this.orderId = orderId;
    }

    public String getUsername() {
        return username;
    }

    public Long getOrderId() {
        return orderId;
    }
}
