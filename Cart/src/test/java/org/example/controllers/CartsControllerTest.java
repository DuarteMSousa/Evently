package org.example.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.exceptions.CartNotFoundException;
import org.example.exceptions.EmptyCartException;
import org.example.models.Cart;
import org.example.services.CartsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(CartsController.class)
class CartsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CartsService cartsService;

    @Test
    void getCart_success_returns200() throws Exception {
        UUID userId = UUID.randomUUID();

        Cart cart = new Cart();
        cart.setUserId(userId);

        when(cartsService.getCart(userId)).thenReturn(cart);

        mockMvc.perform(get("/carts/get-cart/{userId}", userId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void getCart_genericError_returns500() throws Exception {
        UUID userId = UUID.randomUUID();
        when(cartsService.getCart(userId)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/carts/get-cart/{userId}", userId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("boom"));
    }

    @Test
    void clearCart_success_returns200() throws Exception {
        UUID userId = UUID.randomUUID();

        Cart cart = new Cart();
        cart.setUserId(userId);

        when(cartsService.clearCart(userId)).thenReturn(cart);

        mockMvc.perform(put("/carts/clear-cart/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void clearCart_notFound_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        when(cartsService.clearCart(userId)).thenThrow(new CartNotFoundException("Cart not found"));

        mockMvc.perform(put("/carts/clear-cart/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Cart not found"));
    }

    @Test
    void clearCart_genericError_returns500() throws Exception {
        UUID userId = UUID.randomUUID();
        when(cartsService.clearCart(userId)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(put("/carts/clear-cart/{userId}", userId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("boom"));
    }

    @Test
    void checkoutCart_success_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        Cart cart = new Cart();
        cart.setUserId(userId);

        when(cartsService.checkoutCart(userId)).thenReturn(cart);

        mockMvc.perform(post("/carts/checkout-cart/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void checkoutCart_cartNotFound_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        when(cartsService.checkoutCart(userId)).thenThrow(new CartNotFoundException("Cart not found"));

        mockMvc.perform(post("/carts/checkout-cart/{userId}", userId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Cart not found"));
    }

    @Test
    void checkoutCart_emptyCart_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        when(cartsService.checkoutCart(userId)).thenThrow(new EmptyCartException("Empty Cart"));

        mockMvc.perform(post("/carts/checkout-cart/{userId}", userId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Empty Cart"));
    }

    @Test
    void checkoutCart_genericError_returns500() throws Exception {
        UUID userId = UUID.randomUUID();
        when(cartsService.checkoutCart(userId)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/carts/checkout-cart/{userId}", userId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("boom"));
    }
}
