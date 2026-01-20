package org.example.controllers;

import org.example.exceptions.CartItemAlreadyExistsException;
import org.example.exceptions.CartItemNotFoundException;
import org.example.exceptions.CartNotFoundException;
import org.example.models.CartItem;
import org.example.services.CartItemsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(CartItemsController.class)
class CartItemsControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private CartItemsService cartItemsService;

    @Test
    void addCartItem_success_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        CartItem saved = new CartItem();
        saved.setId(UUID.randomUUID());
        saved.setProductId(itemId);
        saved.setQuantity(2);

        when(cartItemsService.addItemToCart(eq(userId), eq(itemId), eq(2))).thenReturn(saved);

        mockMvc.perform(post("/carts/items/add-item/{userId}/{itemId}/{quantity}", userId, itemId, 2)
                        .accept(MediaType.ALL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(itemId.toString()))
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void addCartItem_cartNotFound_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(cartItemsService.addItemToCart(eq(userId), eq(itemId), eq(1)))
                .thenThrow(new CartNotFoundException("Cart not found"));

        mockMvc.perform(post("/carts/items/add-item/{userId}/{itemId}/{quantity}", userId, itemId, 1))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Cart not found"));
    }

    @Test
    void addCartItem_itemAlreadyExists_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(cartItemsService.addItemToCart(eq(userId), eq(itemId), eq(1)))
                .thenThrow(new CartItemAlreadyExistsException("Cart Item already exists"));

        mockMvc.perform(post("/carts/items/add-item/{userId}/{itemId}/{quantity}", userId, itemId, 1))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Cart Item already exists"));
    }

    @Test
    void addCartItem_genericError_returns500() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(cartItemsService.addItemToCart(eq(userId), eq(itemId), anyInt()))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/carts/items/add-item/{userId}/{itemId}/{quantity}", userId, itemId, 1))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("boom"));
    }

    // -------- update --------

    @Test
    void updateCartItem_success_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        CartItem updated = new CartItem();
        updated.setId(UUID.randomUUID());
        updated.setProductId(itemId);
        updated.setQuantity(5);

        when(cartItemsService.updateCartItem(eq(userId), eq(itemId), eq(5))).thenReturn(updated);

        mockMvc.perform(put("/carts/items/update-item/{userId}/{itemId}/{quantity}", userId, itemId, 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(itemId.toString()))
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void updateCartItem_cartNotFound_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(cartItemsService.updateCartItem(eq(userId), eq(itemId), eq(2)))
                .thenThrow(new CartNotFoundException("Cart not found"));

        mockMvc.perform(put("/carts/items/update-item/{userId}/{itemId}/{quantity}", userId, itemId, 2))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Cart not found"));
    }

    @Test
    void updateCartItem_itemNotFound_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(cartItemsService.updateCartItem(eq(userId), eq(itemId), eq(2)))
                .thenThrow(new CartItemNotFoundException("Cart Item not found"));

        mockMvc.perform(put("/carts/items/update-item/{userId}/{itemId}/{quantity}", userId, itemId, 2))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Cart Item not found"));
    }

    @Test
    void updateCartItem_genericError_returns500() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(cartItemsService.updateCartItem(eq(userId), eq(itemId), anyInt()))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(put("/carts/items/update-item/{userId}/{itemId}/{quantity}", userId, itemId, 2))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("boom"));
    }

    // -------- remove --------

    @Test
    void removeCartItem_success_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(delete("/carts/items/remove-item/{userId}/{itemId}", userId, itemId))
                .andExpect(status().isOk());
    }

    @Test
    void removeCartItem_cartNotFound_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        doThrow(new CartNotFoundException("Cart not found"))
                .when(cartItemsService).removeItemFromCart(userId, itemId);

        mockMvc.perform(delete("/carts/items/remove-item/{userId}/{itemId}", userId, itemId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Cart not found"));
    }

    @Test
    void removeCartItem_itemNotFound_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        doThrow(new CartItemNotFoundException("Item not found in cart"))
                .when(cartItemsService).removeItemFromCart(userId, itemId);

        mockMvc.perform(delete("/carts/items/remove-item/{userId}/{itemId}", userId, itemId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Item not found in cart"));
    }

    @Test
    void removeCartItem_genericError_returns500() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        doThrow(new RuntimeException("boom"))
                .when(cartItemsService).removeItemFromCart(userId, itemId);

        mockMvc.perform(delete("/carts/items/remove-item/{userId}/{itemId}", userId, itemId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("boom"));
    }
}
