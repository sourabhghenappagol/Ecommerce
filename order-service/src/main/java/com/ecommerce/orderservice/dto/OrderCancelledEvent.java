package com.ecommerce.orderservice.dto;

public class OrderCancelledEvent {
    private String username;
    private Long orderId;
    private String reason;
    private String eventType = "ORDER_CANCELLED";

    public OrderCancelledEvent() {}

    public OrderCancelledEvent(String username, Long orderId, String reason) {
        this.username = username;
        this.orderId = orderId;
        this.reason = reason;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
}