package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.PaymentServiceClient;
import com.ecommerce.orderservice.dto.PaymentRequest;
import com.ecommerce.orderservice.dto.PaymentResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.kafka.OrderEventProducer;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderSagaOrchestrator {

    private final OrderRepository orderRepository;
    private final PaymentServiceClient paymentServiceClient;
    private final OrderEventProducer eventProducer;

    public OrderSagaOrchestrator(OrderRepository orderRepository,
                                 PaymentServiceClient paymentServiceClient,
                                 OrderEventProducer eventProducer) {
        this.orderRepository = orderRepository;
        this.paymentServiceClient = paymentServiceClient;
        this.eventProducer = eventProducer;
    }

    /**
     * Execute the complete order saga:
     * 1. Create order (PENDING)
     * 2. Process payment
     * 3. Update order based on payment result
     * 4. Publish appropriate event
     */
    @Transactional
    public Order executeOrderSaga(Order order) {
        System.out.println("=== Starting Order Saga for user: " + order.getUsername() + " ===");

        // Step 1: Create order in PENDING status
        order.setStatus(OrderStatus.PENDING);
        Order savedOrder = orderRepository.save(order);
        System.out.println("Step 1: Order created with ID: " + savedOrder.getId() + ", Status: PENDING");

        try {
            // Step 2: Process payment via Payment Service
            System.out.println("Step 2: Calling Payment Service...");
            PaymentRequest paymentRequest = new PaymentRequest(
                    savedOrder.getId(),
                    savedOrder.getUsername(),
                    savedOrder.getTotalAmount()
            );

            PaymentResponse paymentResponse = paymentServiceClient.processPayment(paymentRequest);

            // Step 3: Handle payment response
            if ("SUCCESS".equals(paymentResponse.getStatus())) {
                return handlePaymentSuccess(savedOrder, paymentResponse);
            } else {
                return handlePaymentFailure(savedOrder, paymentResponse);
            }

        } catch (Exception e) {
            // Step 3 (Error): Handle unexpected errors
            System.err.println("Error during payment processing: " + e.getMessage());
            return handlePaymentFailure(savedOrder, null);
        }
    }

    /**
     * Handle successful payment
     */
    private Order handlePaymentSuccess(Order order, PaymentResponse paymentResponse) {
        System.out.println("Step 3: Payment SUCCESS - Transaction ID: " + paymentResponse.getTransactionId());

        // Update order to PAID
        order.setStatus(OrderStatus.PAID);
        order.setPaymentId(paymentResponse.getTransactionId());
        Order updatedOrder = orderRepository.save(order);

        // Step 4: Publish ORDER_COMPLETED event (Cart Service will clear cart)
        System.out.println("Step 4: Publishing ORDER_COMPLETED event");
        eventProducer.publishOrderCompletedEvent(order.getUsername(), order.getId());

        System.out.println("=== Order Saga Completed Successfully ===");
        return updatedOrder;
    }

    /**
     * Compensating transaction: Handle payment failure
     */
    private Order handlePaymentFailure(Order order, PaymentResponse paymentResponse) {
        String failureReason = (paymentResponse != null && paymentResponse.getFailureReason() != null)
                ? paymentResponse.getFailureReason()
                : "Payment service unavailable";

        System.out.println("Step 3: Payment FAILED - Reason: " + failureReason);

        // Compensating transaction: Cancel order
        order.setStatus(OrderStatus.CANCELLED);
        order.setFailureReason(failureReason);
        Order cancelledOrder = orderRepository.save(order);

        // Step 4: Publish ORDER_CANCELLED event (optional - cart remains for retry)
        System.out.println("Step 4: Publishing ORDER_CANCELLED event");
        eventProducer.publishOrderCancelledEvent(order.getUsername(), order.getId(), failureReason);

        System.out.println("=== Order Saga Failed - Order Cancelled ===");
        return cancelledOrder;
    }
}