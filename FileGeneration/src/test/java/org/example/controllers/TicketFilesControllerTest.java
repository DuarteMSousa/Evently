package org.example.controllers;

import jakarta.servlet.ServletException;
import org.example.messages.TicketGeneratedMessage;
import org.example.services.TicketFileGenerationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;


import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketFilesController.class)
@AutoConfigureMockMvc(addFilters = false)
class TicketFilesControllerTest {

    @Autowired MockMvc mockMvc;

    @MockBean TicketFileGenerationService ticketFileGenerationService;

    @Test
    void post_generateTicketFile_success_returns200() throws Exception {

        String body = String.format(
                "{ \"id\":\"%s\", \"eventId\":\"%s\", \"sessionId\":\"%s\", \"tierId\":\"%s\" }",
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        doNothing().when(ticketFileGenerationService)
                .saveTicketFile(any(TicketGeneratedMessage.class));

        mockMvc.perform(post("/fileGeneration/ticket-files/generate-ticket-file")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }


    @Test
    void post_generateTicketFile_serviceThrows_exceptionBubblesUp() {
        doThrow(new RuntimeException("boom"))
                .when(ticketFileGenerationService).saveTicketFile(any(TicketGeneratedMessage.class));

        String body = String.format(
                "{ \"id\":\"%s\", \"eventId\":\"%s\", \"sessionId\":\"%s\", \"tierId\":\"%s\" }",
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
        );

        ServletException ex = assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/fileGeneration/ticket-files/generate-ticket-file")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andReturn()
        );

        assertTrue(ex.getCause() instanceof RuntimeException);
        assertEquals("boom", ex.getCause().getMessage());
    }

    @Test
    void get_ticketPdf_success_returns200WithPdfContentTypeAndDisposition() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] pdf = "PDF".getBytes();

        when(ticketFileGenerationService.getTicketPdf(id)).thenReturn(pdf);

        mockMvc.perform(get("/fileGeneration/ticket-files/get-ticket-file/{id}", id))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + id + ".pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdf));
    }

    @Test
    void get_ticketPdf_notFound_serviceThrows_exceptionBubblesUp() {
        UUID id = UUID.randomUUID();

        when(ticketFileGenerationService.getTicketPdf(id))
                .thenThrow(new RuntimeException("Ticket file not found"));

        ServletException ex = assertThrows(ServletException.class, () ->
                mockMvc.perform(get("/fileGeneration/ticket-files/get-ticket-file/{id}", id))
                        .andReturn()
        );

        assertTrue(ex.getCause() instanceof RuntimeException);
        assertEquals("Ticket file not found", ex.getCause().getMessage());
    }

}