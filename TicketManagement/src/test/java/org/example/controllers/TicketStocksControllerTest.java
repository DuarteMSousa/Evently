package org.example.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.exceptions.TicketStockAlreadyExistsException;
import org.example.exceptions.TicketStockNotFoundException;
import org.example.models.TicketStock;
import org.example.models.TicketStockId;
import org.example.services.TicketStocksService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(TicketStocksController.class)
class TicketStocksControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private TicketStocksService ticketStocksService;

    @Test
    void createStock_success_returns201() throws Exception {
        TicketStock stock = new TicketStock();
        TicketStockId id = new TicketStockId(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        stock.setId(id);
        stock.setAvailableQuantity(0);

        when(ticketStocksService.createTicketStock(any(TicketStock.class))).thenReturn(stock);

        mockMvc.perform(post("/ticketManagement/ticketStocks/create-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stock)))
                .andExpect(status().isCreated());
    }

    @Test
    void createStock_alreadyExists_returns409() throws Exception {
        TicketStock stock = new TicketStock();
        TicketStockId id = new TicketStockId(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        stock.setId(id);

        when(ticketStocksService.createTicketStock(any(TicketStock.class)))
                .thenThrow(new TicketStockAlreadyExistsException("Ticket stock already exists"));

        mockMvc.perform(post("/ticketManagement/ticketStocks/create-stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stock)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Ticket stock already exists"));
    }

    @Test
    void deleteEventStock_success_returns200() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(ticketStocksService.deleteEventTicketStock(eventId)).thenReturn(Collections.emptyList());

        mockMvc.perform(delete("/ticketManagement/ticketStocks/delete-event-stock/{eventId}", eventId))
                .andExpect(status().isOk());
    }

    @Test
    void deleteEventStock_notFound_returns404() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(ticketStocksService.deleteEventTicketStock(eventId))
                .thenThrow(new TicketStockNotFoundException("Ticket stock not found for event"));

        mockMvc.perform(delete("/ticketManagement/ticketStocks/delete-event-stock/{eventId}", eventId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Ticket stock not found for event"));
    }
}