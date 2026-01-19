package org.evently.tickets.controllers;

import org.evently.tickets.dtos.TicketCreateDTO;
import org.evently.tickets.enums.TicketStatus;
import org.evently.tickets.exceptions.InvalidTicketUpdateException;
import org.evently.tickets.exceptions.TicketNotFoundException;
import org.evently.tickets.models.Ticket;
import org.evently.tickets.services.TicketsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketsController.class)
@AutoConfigureMockMvc(addFilters = false)
class TicketsControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TicketsService ticketsService;

    // -------------------------
    // GET /tickets/get-ticket/{id}
    // -------------------------

    @Test
    void getTicket_exists_returns200AndBody() throws Exception {
        UUID id = UUID.randomUUID();
        Ticket t = new Ticket();
        t.setId(id);
        t.setReservationId(UUID.randomUUID());
        t.setOrderId(UUID.randomUUID());
        t.setUserId(UUID.randomUUID());
        t.setEventId(UUID.randomUUID());
        t.setSessionId(UUID.randomUUID());
        t.setTierId(UUID.randomUUID());
        t.setStatus(TicketStatus.ISSUED);
        t.setIssuedAt(new Date());

        when(ticketsService.getTicket(id)).thenReturn(t);

        mockMvc.perform(get("/tickets/get-ticket/{id}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("ISSUED"));
    }

    @Test
    void getTicket_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketsService.getTicket(id)).thenThrow(new TicketNotFoundException("Ticket not found"));

        mockMvc.perform(get("/tickets/get-ticket/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Ticket not found"));
    }

    @Test
    void getTicket_genericError_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketsService.getTicket(id)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/tickets/get-ticket/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("boom"));
    }

    // -------------------------
    // POST /tickets/issue-ticket
    // -------------------------

    @Test
    void issueTicket_valid_returns201AndDto() throws Exception {
        Ticket saved = new Ticket();
        UUID id = UUID.randomUUID();
        saved.setId(id);
        saved.setReservationId(UUID.randomUUID());
        saved.setOrderId(UUID.randomUUID());
        saved.setUserId(UUID.randomUUID());
        saved.setEventId(UUID.randomUUID());
        saved.setSessionId(UUID.randomUUID());
        saved.setTierId(UUID.randomUUID());
        saved.setStatus(TicketStatus.ISSUED);
        saved.setIssuedAt(new Date());

        when(ticketsService.issueTicket(any(Ticket.class))).thenReturn(saved);

        String body = String.format(
                "{ \"reservationId\":\"%s\", \"orderId\":\"%s\", \"userId\":\"%s\", \"eventId\":\"%s\", \"sessionId\":\"%s\", \"tierId\":\"%s\" }",
                saved.getReservationId(),
                saved.getOrderId(),
                saved.getUserId(),
                saved.getEventId(),
                saved.getSessionId(),
                saved.getTierId()
        );

        mockMvc.perform(post("/tickets/issue-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("ISSUED"));
    }

    @Test
    void issueTicket_invalid_returns400() throws Exception {
        when(ticketsService.issueTicket(any(Ticket.class)))
                .thenThrow(new InvalidTicketUpdateException("Reservation ID is required"));

        // payload “incompleto” só para bater no controller
        String body = "{ \"reservationId\": null }";

        mockMvc.perform(post("/tickets/issue-ticket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Reservation ID is required"));
    }

    // -------------------------
    // PUT /tickets/use-ticket/{id}
    // -------------------------

    @Test
    void useTicket_valid_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Ticket used = new Ticket();
        used.setId(id);
        used.setStatus(TicketStatus.USED);
        used.setValidatedAt(new Date());

        when(ticketsService.useTicket(id)).thenReturn(used);

        mockMvc.perform(put("/tickets/use-ticket/{id}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("USED"));
    }

    @Test
    void useTicket_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketsService.useTicket(id)).thenThrow(new TicketNotFoundException("Ticket not found"));

        mockMvc.perform(put("/tickets/use-ticket/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Ticket not found"));
    }

    @Test
    void useTicket_invalid_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketsService.useTicket(id)).thenThrow(new InvalidTicketUpdateException("Ticket is already used"));

        mockMvc.perform(put("/tickets/use-ticket/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Ticket is already used"));
    }

    // -------------------------
    // PUT /tickets/cancel-ticket/{id}
    // -------------------------

    @Test
    void cancelTicket_valid_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Ticket canceled = new Ticket();
        canceled.setId(id);
        canceled.setStatus(TicketStatus.CANCELED);

        when(ticketsService.cancelTicket(id)).thenReturn(canceled);

        mockMvc.perform(put("/tickets/cancel-ticket/{id}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void cancelTicket_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketsService.cancelTicket(id)).thenThrow(new TicketNotFoundException("Ticket not found"));

        mockMvc.perform(put("/tickets/cancel-ticket/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Ticket not found"));
    }

    @Test
    void cancelTicket_invalid_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        when(ticketsService.cancelTicket(id)).thenThrow(new InvalidTicketUpdateException("Ticket is already canceled"));

        mockMvc.perform(put("/tickets/cancel-ticket/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Ticket is already canceled"));
    }
}