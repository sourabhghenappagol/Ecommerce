package com.ecommerce.paymentservice.dto;

import com.ecommerce.paymentservice.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {

    private Long paymentId;
    private Long orderId;
    private String userId;
    private BigDecimal amount;
    private Payment.PaymentStatus status;
    private String transactionId;
    private String message;
    private String failureReason;
    private LocalDateTime timestamp;

    public PaymentResponse() {
    }

    public PaymentResponse(Long paymentId, Long orderId, String userId, BigDecimal amount, Payment.PaymentStatus status, String transactionId, String message, String failureReason, LocalDateTime timestamp) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.transactionId = transactionId;
        this.message = message;
        this.failureReason = failureReason;
        this.timestamp = timestamp;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

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

    public Payment.PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(Payment.PaymentStatus status) {
        this.status = status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    // Factory method for success
    public static PaymentResponse success(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                Payment.PaymentStatus.SUCCESS,
                payment.getTransactionId(),
                "Payment processed successfully",
                null,
                payment.getCreatedAt()
        );
    }

    // Factory method for failure
    public static PaymentResponse failure(Payment payment, String reason) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                Payment.PaymentStatus.FAILED,
                payment.getTransactionId(),
                "Payment failed",
                reason,
                payment.getCreatedAt()
        );
    }
}