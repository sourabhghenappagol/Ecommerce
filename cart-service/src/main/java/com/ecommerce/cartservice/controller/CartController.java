package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.dto.AddToCartRequest;
import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Get logged-in user's cart
     */
    @GetMapping
    public ResponseEntity<Cart> getCart(Authentication authentication) {
        Cart cart = cartService.getCart(authentication.getName());
        return ResponseEntity.ok(cart);
    }

    /**
     * Add product to cart
     * JWT is forwarded internally to Product Service
     */
    @PostMapping("/add")
    public ResponseEntity<Cart> addToCart(
            Authentication authentication,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody AddToCartRequest request) {

        Cart cart = cartService.addItem(
                authentication.getName(),
                request.getProductId(),
                request.getQuantity(),
                authorizationHeader
        );

        return ResponseEntity.ok(cart);
    }

    /**
     * Remove product from cart
     */
    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<Cart> removeItem(
            Authentication authentication,
            @PathVariable Long productId) {

        Cart cart = cartService.removeItem(authentication.getName(), productId);
        return ResponseEntity.ok(cart);
    }

    /**
     * Clear entire cart
     */
    @DeleteMapping("/clear")
    public ResponseEntity<String> clearCart(Authentication authentication) {
        cartService.clearCart(authentication.getName());
        return ResponseEntity.ok("Cart cleared successfully");
    }
}
