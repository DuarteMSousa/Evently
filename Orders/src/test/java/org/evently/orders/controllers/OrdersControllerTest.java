package org.evently.orders.controllers;

import org.evently.orders.enums.OrderStatus;
import org.evently.orders.exceptions.InvalidOrderException;
import org.evently.orders.exceptions.OrderNotFoundException;
import org.evently.orders.models.Order;
import org.evently.orders.services.OrdersService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = org.evently.orders.controllers.OrdersController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrdersControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean OrdersService ordersService;

    @Test
    void getOrder_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Order o = new Order();
        o.setId(id);
        o.setUserId(UUID.randomUUID());
        o.setStatus(OrderStatus.CREATED);
        o.setTotal(10);

        when(ordersService.getOrder(id)).thenReturn(o);

        mockMvc.perform(get("/orders/get-order/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void getOrder_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(ordersService.getOrder(id)).thenThrow(new OrderNotFoundException("Order not found"));

        mockMvc.perform(get("/orders/get-order/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Order not found"));
    }

    @Test
    void getOrder_genericError_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        when(ordersService.getOrder(id)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/orders/get-order/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("boom"));
    }

    @Test
    void getOrdersByUser_success_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        org.springframework.data.domain.Page<Order> page =
                new org.springframework.data.domain.PageImpl<>(Collections.singletonList(new Order()));

        when(ordersService.getOrdersByUser(eq(userId), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/orders/user/{userId}/{pageNumber}/{pageSize}", userId, 1, 10))
                .andExpect(status().isOk());
    }

    @Test
    void registerOrder_success_returns201() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Order saved = new Order();
        saved.setId(orderId);
        saved.setUserId(userId);
        saved.setStatus(OrderStatus.CREATED);
        saved.setTotal(20);

        when(ordersService.createOrder(any(Order.class))).thenReturn(saved);

        String body = "{"
                + "\"userId\":\"" + userId + "\","
                + "\"lines\":[{\"productId\":\"" + productId + "\",\"quantity\":2}]"
                + "}";

        mockMvc.perform(post("/orders/register-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void registerOrder_invalid_returns400() throws Exception {
        when(ordersService.createOrder(any(Order.class)))
                .thenThrow(new InvalidOrderException("User ID is required"));

        String body = "{\"userId\":null,\"lines\":[{\"productId\":\"" + UUID.randomUUID() + "\",\"quantity\":2}]}";

        mockMvc.perform(post("/orders/register-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerOrder_generic_returns500() throws Exception {
        when(ordersService.createOrder(any(Order.class))).thenThrow(new RuntimeException("boom"));

        String body = "{"
                + "\"userId\":\"" + UUID.randomUUID() + "\","
                + "\"lines\":[{\"productId\":\"" + UUID.randomUUID() + "\",\"quantity\":2}]"
                + "}";

        mockMvc.perform(post("/orders/register-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void markAsPaid_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Order o = new Order(); o.setId(id); o.setStatus(OrderStatus.PAYMENT_SUCCESS);

        when(ordersService.markAsPaid(id)).thenReturn(o);

        mockMvc.perform(put("/orders/mark-order-payment-success/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAYMENT_SUCCESS"));
    }

    @Test
    void markAsPaid_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(ordersService.markAsPaid(id)).thenThrow(new OrderNotFoundException("Order not found"));

        mockMvc.perform(put("/orders/mark-order-payment-success/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void markAsPaid_invalid_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        when(ordersService.markAsPaid(id)).thenThrow(new InvalidOrderException("Cannot pay order"));

        mockMvc.perform(put("/orders/mark-order-payment-success/{id}", id))
                .andExpect(status().isBadRequest());
    }

    @Test
    void markAsFailed_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Order o = new Order(); o.setId(id); o.setStatus(OrderStatus.PAYMENT_FAILED);

        when(ordersService.markAsPaymentFailed(id)).thenReturn(o);

        mockMvc.perform(put("/orders/mark-order-payment-failed/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAYMENT_FAILED"));
    }

    @Test
    void cancelOrder_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Order o = new Order(); o.setId(id); o.setStatus(OrderStatus.CANCELLED);

        when(ordersService.cancelOrder(id)).thenReturn(o);

        mockMvc.perform(put("/orders/cancel-order/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelOrder_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(ordersService.cancelOrder(id)).thenThrow(new OrderNotFoundException("Order not found"));

        mockMvc.perform(put("/orders/cancel-order/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelOrder_invalid_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        when(ordersService.cancelOrder(id)).thenThrow(new InvalidOrderException("Order already cancelled"));

        mockMvc.perform(put("/orders/cancel-order/{id}", id))
                .andExpect(status().isBadRequest());
    }
}
