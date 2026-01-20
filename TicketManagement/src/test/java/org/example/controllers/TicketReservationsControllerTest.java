package org.example.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dtos.ticketReservations.TicketReservationCreateDTO;
import org.example.enums.TicketReservationStatus;
import org.example.exceptions.InvalidTicketReservationException;
import org.example.exceptions.TicketStockNotFoundException;
import org.example.models.TicketReservation;
import org.example.services.TicketReservationsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(TicketReservationsController.class)
class TicketReservationsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private TicketReservationsService ticketReservationsService;

    @Test
    void reserveTicket_success_returns201() throws Exception {
        TicketReservationCreateDTO dto = new TicketReservationCreateDTO();
        dto.setUserId(UUID.randomUUID());
        dto.setOrderId(UUID.randomUUID());
        dto.setEventId(UUID.randomUUID());
        dto.setSessionId(UUID.randomUUID());
        dto.setTierId(UUID.randomUUID());
        dto.setQuantity(2);

        TicketReservation saved = new TicketReservation();
        saved.setId(UUID.randomUUID());
        saved.setUserId(dto.getUserId());
        saved.setOrderId(dto.getOrderId());
        saved.setEventId(dto.getEventId());
        saved.setSessionId(dto.getSessionId());
        saved.setTierId(dto.getTierId());
        saved.setQuantity(dto.getQuantity());
        saved.setStatus(TicketReservationStatus.HELD);

        when(ticketReservationsService.createTicketReservation(any(TicketReservation.class))).thenReturn(saved);

        mockMvc.perform(post("/ticketManagement/ticketReservations/reserve-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void reserveTicket_invalidQuantity_returns400() throws Exception {
        TicketReservationCreateDTO dto = new TicketReservationCreateDTO();
        dto.setUserId(UUID.randomUUID());
        dto.setOrderId(UUID.randomUUID());
        dto.setEventId(UUID.randomUUID());
        dto.setSessionId(UUID.randomUUID());
        dto.setTierId(UUID.randomUUID());
        dto.setQuantity(0);

        when(ticketReservationsService.createTicketReservation(any(TicketReservation.class)))
                .thenThrow(new InvalidTicketReservationException("Quantity must be greater than 0"));

        mockMvc.perform(post("/ticketManagement/ticketReservations/reserve-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Quantity must be greater than 0"));
    }

    @Test
    void reserveTicket_stockNotFound_returns400() throws Exception {
        TicketReservationCreateDTO dto = new TicketReservationCreateDTO();
        dto.setUserId(UUID.randomUUID());
        dto.setOrderId(UUID.randomUUID());
        dto.setEventId(UUID.randomUUID());
        dto.setSessionId(UUID.randomUUID());
        dto.setTierId(UUID.randomUUID());
        dto.setQuantity(1);

        when(ticketReservationsService.createTicketReservation(any(TicketReservation.class)))
                .thenThrow(new TicketStockNotFoundException("Ticket Stock not found"));

        mockMvc.perform(post("/ticketManagement/ticketReservations/reserve-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Ticket Stock not found"));
    }

    @Test
    void checkEventReservations_success_returns201WithBoolean() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(ticketReservationsService.eventHasReservations(eventId)).thenReturn(true);

        mockMvc.perform(get("/ticketManagement/ticketReservations/event-has-reservations/{eventId}", eventId))
                .andExpect(status().isCreated())
                .andExpect(content().string("true"));
    }

    @Test
    void checkEventReservations_error_returns500() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(ticketReservationsService.eventHasReservations(eventId)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/ticketManagement/ticketReservations/event-has-reservations/{eventId}", eventId))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("boom"));
    }

    @Test
    void checkSessionReservations_success_returns201WithBoolean() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(ticketReservationsService.sessionHasReservations(sessionId)).thenReturn(false);

        mockMvc.perform(get("/ticketManagement/ticketReservations/session-has-reservations/{sessionId}", sessionId))
                .andExpect(status().isCreated())
                .andExpect(content().string("false"));
    }

    @Test
    void checkTierReservations_success_returns201WithBoolean() throws Exception {
        UUID tierId = UUID.randomUUID();
        when(ticketReservationsService.tierHasReservations(tierId)).thenReturn(true);

        mockMvc.perform(get("/ticketManagement/ticketReservations/tier-has-reservations/{tierId}", tierId))
                .andExpect(status().isCreated())
                .andExpect(content().string("true"));
    }

}