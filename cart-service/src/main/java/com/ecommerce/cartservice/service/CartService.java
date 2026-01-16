package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.entity.CartItem;
import com.ecommerce.cartservice.repository.CartRepository;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class CartService {

    private final CartRepository cartRepository;

    public CartService(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public Cart getCart(String username) {
        return cartRepository.findByUsername(username)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUsername(username);
                    cart.setItems(new ArrayList<>());
                    return cartRepository.save(cart);
                });
    }

    public Cart addItem(String username, CartItem item) {
        Cart cart = getCart(username);

        for (CartItem ci : cart.getItems()) {
            if (ci.getProductId().equals(item.getProductId())) {
                ci.setQuantity(ci.getQuantity() + item.getQuantity());
                return cartRepository.save(cart);
            }
        }

        item.setCart(cart);
        cart.getItems().add(item);
        return cartRepository.save(cart);
    }

    public Cart removeItem(String username, Long productId) {
        Cart cart = getCart(username);
        cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        return cartRepository.save(cart);
    }

    /**
     * Clear all items from the user's cart and persist the change.
     * Uses orphanRemoval on the relationship so JPA will delete cart item rows.
     */
    public Cart clearCart(String username) {
        Cart cart = getCart(username);
        if (cart.getItems() == null) {
            cart.setItems(new ArrayList<>());
        } else {
            cart.getItems().clear();
        }
        return cartRepository.save(cart);
    }
}
