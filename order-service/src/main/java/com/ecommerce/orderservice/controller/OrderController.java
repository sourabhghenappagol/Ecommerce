package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Place an order for the logged-in user
     * Reads cart from Cart Service and creates order
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
     * Get order history of logged-in user
     */
    @GetMapping
    public ResponseEntity<List<Order>> getMyOrders(
            Authentication authentication) {

        List<Order> orders =
                orderService.getOrdersByUsername(authentication.getName());

        return ResponseEntity.ok(orders);
    }

    /**
     * Health check endpoint for the service. Returns simple JSON with status.
     * Example: GET /api/orders/health -> { "service":"order-service", "status":"UP" }
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> body = new HashMap<>();
        body.put("service", "order-service");
        body.put("status", "UP");
        return ResponseEntity.ok(body);
    }

}
