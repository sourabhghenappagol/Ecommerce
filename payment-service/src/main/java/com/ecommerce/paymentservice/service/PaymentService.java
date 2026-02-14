package com.ecommerce.paymentservice.service;

import com.ecommerce.paymentservice.dto.PaymentRequest;
import com.ecommerce.paymentservice.dto.PaymentResponse;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Logger;

@Service
public class PaymentService {

    private static final Logger logger = Logger.getLogger(PaymentService.class.getName());

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayService paymentGatewayService;

    public PaymentService(PaymentRepository paymentRepository, PaymentGatewayService paymentGatewayService) {
        this.paymentRepository = paymentRepository;
        this.paymentGatewayService = paymentGatewayService;
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        logger.info(() -> "Starting payment processing for order: " + request.getOrderId());

        // Check if payment already exists for this order
        if (paymentRepository.findByOrderId(request.getOrderId()).isPresent()) {
            throw new IllegalStateException("Payment already processed for order: " + request.getOrderId());
        }

        // Create initial payment record using entity setters (no builder available)
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setUserId(request.getUserId());
        payment.setAmount(request.getAmount());
        payment.setStatus(Payment.PaymentStatus.PENDING);

        payment = paymentRepository.save(payment);
        logger.info("Payment record created with ID: " + payment.getId());

        // Call payment gateway
        PaymentGatewayService.GatewayResponse gatewayResponse =
                paymentGatewayService.processPayment(request);

        // Update payment based on gateway response
        if (gatewayResponse.isSuccess()) {
            payment.setStatus(Payment.PaymentStatus.SUCCESS);
            payment.setTransactionId(gatewayResponse.getTransactionId());
            payment.setGatewayResponse(gatewayResponse.getGatewayResponse());
            paymentRepository.save(payment);

            logger.info(() -> "Payment successful for order: " + request.getOrderId());
            return PaymentResponse.success(payment);

        } else {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setFailureReason(gatewayResponse.getFailureReason());
            payment.setGatewayResponse(gatewayResponse.getGatewayResponse());
            paymentRepository.save(payment);

            logger.warning("Payment failed for order: " + request.getOrderId() + ". Reason: " + gatewayResponse.getFailureReason());
            return PaymentResponse.failure(payment, gatewayResponse.getFailureReason());
        }
    }

    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderId));
    }
}