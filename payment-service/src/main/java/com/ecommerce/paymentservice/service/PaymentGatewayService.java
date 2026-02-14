package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class PaymentGatewayService {

    private static final Logger logger = Logger.getLogger(PaymentGatewayService.class.getName());
    private final Random random = new Random();

    /**
     * Mock payment gateway call
     * In production, replace with real gateway (Stripe, Razorpay, PayPal)
     */
    public GatewayResponse processPayment(PaymentRequest request) {
        logger.info(() -> "Processing payment through gateway for order: " + request.getOrderId());

        // Simulate network delay
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Mock logic: 80% success rate
        boolean isSuccess = random.nextInt(100) < 80;

        if (isSuccess) {
            String transactionId = "TXN_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            logger.info(() -> "Payment successful. Transaction ID: " + transactionId);

            GatewayResponse response = new GatewayResponse();
            response.setSuccess(true);
            response.setTransactionId(transactionId);
            response.setMessage("Payment approved");
            response.setGatewayResponse("Mock gateway approved the transaction");
            return response;
        } else {
            String[] failureReasons = {
                    "Insufficient funds",
                    "Card declined",
                    "Invalid card details",
                    "Transaction limit exceeded",
                    "Payment gateway timeout"
            };
            String reason = failureReasons[random.nextInt(failureReasons.length)];
            logger.log(Level.WARNING, "Payment failed. Reason: {0}", reason);

            GatewayResponse response = new GatewayResponse();
            response.setSuccess(false);
            response.setMessage("Payment declined");
            response.setFailureReason(reason);
            response.setGatewayResponse("Mock gateway declined the transaction");
            return response;
        }
    }

    /**
     * Gateway response wrapper
     */
    public static class GatewayResponse {
        private boolean success;
        private String transactionId;
        private String message;
        private String failureReason;
        private String gatewayResponse;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
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

        public String getGatewayResponse() {
            return gatewayResponse;
        }

        public void setGatewayResponse(String gatewayResponse) {
            this.gatewayResponse = gatewayResponse;
        }
    }
}