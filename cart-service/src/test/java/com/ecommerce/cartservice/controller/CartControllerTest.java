package com.ecommerce.cartservice.controller;

import com.ecommerce.cartservice.entity.Cart;
import com.ecommerce.cartservice.service.CartService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void shouldReturnCart() throws Exception {
        // Setup
        Cart cart = new Cart();
        cart.setUsername("testuser");
        when(cartService.getCart("testuser")).thenReturn(cart);

        // Execute & Assert
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isOk());
        
        // Verify mock was called
        verify(cartService, times(1)).getCart("testuser");
    }

    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void shouldAddCart() throws Exception {
        // Setup
        Cart cart = new Cart();
        cart.setUsername("testuser");
        
        when(cartService.addItem(
                eq("testuser"),
                eq(1L),
                eq(5),
                anyString()
        )).thenReturn(cart);

        // Create request body
        String requestBody = objectMapper.writeValueAsString(
                Map.of("productId", 1L, "quantity", 5)
        );

        // Execute & Assert
        mockMvc.perform(post("/api/cart/add")
                .header("Authorization", "Bearer test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        
        // Verify mock was called
        verify(cartService, times(1)).addItem(
                eq("testuser"),
                eq(1L),
                eq(5),
                anyString()
        );
    }
}