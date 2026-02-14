package com.ecommerce.orderservice.dto;

public class OrderCompletedEvent {
    private String username;
    private Long orderId;
    private String eventType = "ORDER_COMPLETED";

    public OrderCompletedEvent() {}

    public OrderCompletedEvent(String username, Long orderId) {
        this.username = username;
        this.orderId = orderId;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
}