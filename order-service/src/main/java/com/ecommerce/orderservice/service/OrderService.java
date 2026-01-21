package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.CartItemResponse;
import com.ecommerce.orderservice.dto.CartResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
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

    public OrderService(OrderRepository orderRepository,
                        RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * Place order for logged-in user.
     * Steps:
     * 1. Fetch cart from Cart Service
     * 2. Create immutable order with snapshot data
     * 3. Save order
     * 4. Clear cart after successful order creation
     */
    public Order placeOrder(String username, String authorizationHeader) {

        // -------------------------------
        // 1️⃣ Fetch cart from Cart Service
        // -------------------------------
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String cartUrl = "http://localhost:8083/api/cart";

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
        // 2️⃣ Create Order
        // -------------------------------
        Order order = new Order();
        order.setUsername(username);
        order.setStatus(OrderStatus.CREATED);
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
        // 4️⃣ Save Order (cascade saves items)
        // -------------------------------
        Order savedOrder = orderRepository.save(order);

        // -------------------------------
        // 5️⃣ Clear cart AFTER order success
        // -------------------------------
        String clearCartUrl = "http://localhost:8083/api/cart/clear";

        restTemplate.exchange(
                clearCartUrl,
                HttpMethod.DELETE,
                entity,
                Void.class
        );

        return savedOrder;
    }

    /**
     * Fetch all orders of logged-in user
     */
    public List<Order> getOrdersByUsername(String username) {
        return orderRepository.findByUsername(username);
    }
}
