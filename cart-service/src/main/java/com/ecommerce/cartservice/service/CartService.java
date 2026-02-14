package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.ProductResponse;
import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.entity.CartItem;
import com.ecommerce.cartservice.repository.CartRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final RestTemplate restTemplate;

    public CartService(CartRepository cartRepository,
                       RestTemplate restTemplate) {
        this.cartRepository = cartRepository;
        this.restTemplate = restTemplate;
    }

    /**
     * Get or create cart for user
     */
    public Cart getCart(String username) {
        return cartRepository.findByUsername(username)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUsername(username);
                    cart.setItems(new ArrayList<>());
                    return cartRepository.save(cart);
                });
    }

    /**
     * Add item to cart.
     * Product details are fetched from Product Service.
     * JWT is forwarded for authorization.
     */
    public Cart addItem(String username,
                        Long productId,
                        int quantity,
                        String authorizationHeader) {

        // 1️⃣ Call Product Service with JWT
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorizationHeader);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String productUrl = "http://localhost:8081/api/products/" + productId;

        ResponseEntity<ProductResponse> response =
                restTemplate.exchange(
                        productUrl,
                        HttpMethod.GET,
                        entity,
                        ProductResponse.class
                );

        ProductResponse product = response.getBody();

        if (product == null) {
            throw new RuntimeException("Product not found");
        }

        if (!"ACTIVE".equals(product.getStatus())) {
            throw new RuntimeException("Product is not available");
        }

        if (product.getQuantity() < quantity) {
            throw new RuntimeException("Insufficient stock");
        }

        // 2️ Get or create cart
        Cart cart = getCart(username);

        // 3 If item already exists, update quantity
        for (CartItem item : cart.getItems()) {
            if (item.getProductId().equals(productId)) {
                item.setQuantity(item.getQuantity() + quantity);
                return cartRepository.save(cart);
            }
        }

        // 4 Add new cart item (snapshot)
        CartItem newItem = new CartItem();
        newItem.setProductId(product.getId());
        newItem.setProductName(product.getName());
        newItem.setPrice(product.getPrice());
        newItem.setQuantity(quantity);
        newItem.setCart(cart);

        cart.getItems().add(newItem);
        return cartRepository.save(cart);
    }

    /**
     * Remove item from cart
     */
    public Cart removeItem(String username, Long productId) {
        Cart cart = getCart(username);
        cart.getItems().removeIf(item ->
                item.getProductId().equals(productId)
        );
        return cartRepository.save(cart);
    }

    /**
     * Clear cart
     */
    @Transactional
    public void clearCart(String username) {
        // fetch the cart inside a transaction so Hibernate session is open
        Cart cart = cartRepository.findByUsername(username).orElse(null);
        if (cart == null) {
            return;
        }
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}
