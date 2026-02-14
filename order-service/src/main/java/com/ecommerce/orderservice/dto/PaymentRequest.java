package com.ecommerce.orderservice.dto;

import java.math.BigDecimal;

public class PaymentRequest {
    private Long orderId;
    private String userId;
    private BigDecimal amount;

    public PaymentRequest() {}

    public PaymentRequest(Long orderId, String userId, BigDecimal amount) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
    }

    // Getters and Setters
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}