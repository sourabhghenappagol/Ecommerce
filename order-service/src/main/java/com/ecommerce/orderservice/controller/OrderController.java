package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Place order for the logged-in user
     * Flow:
     * 1. Read username from JWT
     * 2. Fetch cart from Cart Service
     * 3. Create order
     * 4. Clear cart
     */
    @PostMapping
    public ResponseEntity<Order> placeOrder(
            Authentication authentication,
            @RequestHeader("Authorization") String authorizationHeader) {

        Order order = orderService.placeOrder(
                authentication.getName(),
                authorizationHeader
        );

        return ResponseEntity.ok(order);
    }

    /**
     * Get order history for logged-in user
     */
    @GetMapping
    public ResponseEntity<List<Order>> getMyOrders(
            Authentication authentication) {

        List<Order> orders =
                orderService.getOrdersByUsername(authentication.getName());

        return ResponseEntity.ok(orders);
    }
}
