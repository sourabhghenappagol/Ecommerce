package com.ecommerce.orderservice.entity;

public enum OrderStatus {
    PENDING,      // NEW - Order created, awaiting payment
    PAID,         // Payment successful
    CANCELLED,    // Payment failed or order cancelled
    SHIPPED,
    DELIVERED
    // CREATED removed - we use PENDING instead
}