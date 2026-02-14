package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger logger = Logger.getLogger(PaymentController.class.getName());

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Process payment for an order
     * This endpoint will be called by Order Service (Saga Orchestrator)
     */
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        logger.info(() -> "Received payment request for order: " + request.getOrderId());

        try {
            PaymentResponse response = paymentService.processPayment(request);

            if (response.getStatus() == Payment.PaymentStatus.SUCCESS) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(response);
            }

        } catch (IllegalStateException e) {
            logger.log(Level.SEVERE, "Payment processing error: " + e.getMessage(), e);
            PaymentResponse resp = new PaymentResponse();
            resp.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(resp);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error during payment processing", e);
            PaymentResponse resp = new PaymentResponse();
            resp.setMessage("Payment processing failed due to internal error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }
    }

    /**
     * Get payment details by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getPaymentByOrderId(@PathVariable Long orderId) {
        try {
            var payment = paymentService.getPaymentByOrderId(orderId);
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }
}