package com.ecommerce.orderservice.dto;

public class OrderCreatedEvent {

    private String username;
    private Long orderId;

    public OrderCreatedEvent() {}

    public OrderCreatedEvent(String username, Long orderId) {
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
