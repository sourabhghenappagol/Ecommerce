package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.ProductResponse;
import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.entity.CartItem;
import com.ecommerce.cartservice.repository.CartRepository;
import org.springframework.stereotype.Service;
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
     * Get cart for logged-in user.
     * If cart does not exist, create a new one.
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
     * Add product to cart.
     * Frontend sends ONLY productId and quantity.
     * Product details are fetched from Product Service.
     */
    public Cart addItem(String username, Long productId, int quantity) {

        // 1️ Fetch product from Product Service (source of truth)
        String productUrl = "http://localhost:8081/api/products/" + productId;
        ProductResponse product =
                restTemplate.getForObject(productUrl, ProductResponse.class);

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

        // 3️ If product already exists in cart → update quantity
        for (CartItem item : cart.getItems()) {
            if (item.getProductId().equals(productId)) {
                item.setQuantity(item.getQuantity() + quantity);
                return cartRepository.save(cart);
            }
        }

        // 4️ Add new item
        CartItem newItem = new CartItem();
        newItem.setProductId(product.getId());
        newItem.setProductName(product.getName());   // snapshot
        newItem.setPrice(product.getPrice());        // snapshot
        newItem.setQuantity(quantity);
        newItem.setCart(cart);

        cart.getItems().add(newItem);
        return cartRepository.save(cart);
    }

    /**
     * Remove a product from cart.
     */
    public Cart removeItem(String username, Long productId) {
        Cart cart = getCart(username);
        cart.getItems().removeIf(item ->
                item.getProductId().equals(productId)
        );
        return cartRepository.save(cart);
    }

    /**
     * Clear entire cart.
     */
    public void clearCart(String username) {
        Cart cart = getCart(username);
        cart.getItems().clear();
        cartRepository.save(cart);
    }
}
