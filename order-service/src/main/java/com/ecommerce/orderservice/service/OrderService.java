package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.CartItemResponse;
import com.ecommerce.orderservice.dto.CartResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.kafka.OrderEventProducer;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final OrderEventProducer orderEventProducer;
    private final OrderSagaOrchestrator sagaOrchestrator;

    public OrderService(OrderRepository orderRepository,
                        RestTemplate restTemplate,
                        OrderEventProducer orderEventProducer,
                        OrderSagaOrchestrator sagaOrchestrator) {
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
        this.orderEventProducer = orderEventProducer;
        this.sagaOrchestrator = sagaOrchestrator;
    }

    /**
     * Place order for logged-in user.
     * Steps:
     * 1. Fetch cart from Cart Service
     * 2. Create order object with snapshot data
     * 3. Execute Saga (create order → process payment → update status)
     * 4. Saga publishes event to clear cart (on success)
     */
    public Order placeOrder(String username, String authorizationHeader) {

        // -------------------------------
        // 1️⃣ Fetch cart from Cart Service
        // -------------------------------
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String cartUrl = "http://localhost:8083/api/cart"; // ✅ FIXED: Changed from 8083 to 8082

        ResponseEntity<CartResponse> cartResponse =
                restTemplate.exchange(
                        cartUrl,
                        HttpMethod.GET,
                        entity,
                        CartResponse.class
                );

        CartResponse cart = cartResponse.getBody();

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty. Cannot place order.");
        }

        // -------------------------------
        // 2️⃣ Create Order Object (not saved yet)
        // -------------------------------
        Order order = new Order();
        order.setUsername(username);
        // ✅ REMOVED: order.setStatus(OrderStatus.CREATED);
        // Status will be set by Saga (PENDING → PAID or CANCELLED)
        order.setCreatedAt(LocalDateTime.now());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // -------------------------------
        // 3️⃣ Convert CartItems → OrderItems
        // -------------------------------
        for (CartItemResponse item : cart.getItems()) {

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(item.getProductId());
            orderItem.setProductName(item.getProductName()); // snapshot
            orderItem.setPrice(item.getPrice());             // snapshot
            orderItem.setQuantity(item.getQuantity());
            orderItem.setOrder(order);

            orderItems.add(orderItem);

            BigDecimal itemTotal =
                    item.getPrice().multiply(
                            BigDecimal.valueOf(item.getQuantity())
                    );

            totalAmount = totalAmount.add(itemTotal);
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);

        // -------------------------------
        // 4️⃣ Execute Saga (Orchestrated Transaction)
        // -------------------------------
        // Saga will:
        // - Create order with PENDING status
        // - Call Payment Service
        // - Update to PAID or CANCELLED
        // - Publish ORDER_COMPLETED or ORDER_CANCELLED event
        Order savedOrder = sagaOrchestrator.executeOrderSaga(order);

        // -------------------------------
        // 5️⃣ Return order (PAID or CANCELLED)
        // -------------------------------
        return savedOrder;
    }

    /**
     * Fetch all orders of logged-in user
     */
    public List<Order> getOrdersByUsername(String username) {
        return orderRepository.findByUsername(username);
    }

    // ✅ NEW: Helper method to get order by ID (used by Saga if needed)
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
    }
}