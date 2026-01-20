package org.example.controllers;

import org.example.models.EventSession;
import org.example.services.EventSessionsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventSessionsController.class)
@AutoConfigureMockMvc(addFilters = false)
class EventSessionsControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private EventSessionsService eventSessionsService;

    @Test
    void getEventSession_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventSessionsService.getEventSession(id)).thenReturn(new EventSession());

        mockMvc.perform(get("/events/sessions/get-event-session/{id}", id))
                .andExpect(status().isOk());
    }

    // create/update
    @Test
    void createEventSession_success_returns200() throws Exception {
        String body = "{\"eventId\":null,\"venueId\":null}";

        when(eventSessionsService.createEventSession(any(EventSession.class)))
                .thenReturn(new EventSession());

        mockMvc.perform(post("/events/sessions/create-event-session/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }


    @Test
    void updateEventSession_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        String body = "{\"id\":\""+id+"\"}";

        when(eventSessionsService.updateEventSession(eq(id), any(EventSession.class))).thenReturn(new EventSession());

        mockMvc.perform(put("/events/sessions/update-event-session/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void deleteEventSession_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/events/sessions/delete-event-session/{id}", id))
                .andExpect(status().isOk());

        verify(eventSessionsService).deleteEventSession(id);
    }

}
