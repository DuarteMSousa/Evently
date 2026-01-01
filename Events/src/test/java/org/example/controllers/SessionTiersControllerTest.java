package org.example.controllers;

import org.example.models.SessionTier;
import org.example.services.SessionTiersService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionTiersController.class)
class SessionTiersControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private SessionTiersService sessionTiersService;

    @Test
    void getSessionTier_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(sessionTiersService.getSessionTier(id)).thenReturn(new SessionTier());

        mockMvc.perform(get("/events/sessions/tiers/get-session-tier/{id}", id))
                .andExpect(status().isOk());
    }

    // create/update -> assumem @RequestBody
    @Test
    void createSessionTier_success_returns200() throws Exception {
        String body = "{}";

        when(sessionTiersService.createSessionTier(any(SessionTier.class))).thenReturn(new SessionTier());

        mockMvc.perform(post("/events/sessions/tiers/create-session-tier/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void updateSessionTier_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        String body = "{\"id\":\""+id+"\"}";

        when(sessionTiersService.updateSessionTier(eq(id), any(SessionTier.class))).thenReturn(new SessionTier());

        mockMvc.perform(put("/events/sessions/tiers/update-event-session/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void deleteSessionTier_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/events/sessions/tiers/delete-event-session/{id}", id))
                .andExpect(status().isOk());

        verify(sessionTiersService).deleteSessionTier(id);
    }
}
