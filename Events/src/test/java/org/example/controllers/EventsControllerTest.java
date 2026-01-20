package org.example.controllers;

import org.example.exceptions.EventNotFoundException;
import org.example.models.Event;
import org.example.services.EventsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventsController.class)
@AutoConfigureMockMvc(addFilters = false)
class EventsControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private EventsService eventsService;

    @Test
    void getEventsPage_success_returns200() throws Exception {
        when(eventsService.getEventPage(1, 10)).thenReturn(new PageImpl<>(Collections.singletonList(new Event())));

        mockMvc.perform(get("/events/get-events-page/{p}/{s}", 1, 10))
                .andExpect(status().isOk());
    }

    @Test
    void getEvent_notFound_returns200WithNullBody_becauseControllerDoesNotReturn404() throws Exception {
        UUID id = UUID.randomUUID();
        when(eventsService.getEvent(id)).thenThrow(new EventNotFoundException("Event not found"));

        mockMvc.perform(get("/events/get-event/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    // create/update/cancel/publish
    @Test
    void createEvent_success_returns200() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        String body = "{\"name\":\"E1\",\"description\":\"D\",\"organizationId\":\""+orgId+"\",\"createdBy\":\""+createdBy+"\"}";

        Event created = new Event();
        created.setId(UUID.randomUUID());
        created.setName("E1");

        when(eventsService.createEvent(any(Event.class))).thenReturn(created);

        mockMvc.perform(post("/events/create-event/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("E1"));
    }

    @Test
    void updateEvent_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        String body = "{\"id\":\""+id+"\",\"name\":\"E2\",\"description\":\"D2\"}";

        Event updated = new Event();
        updated.setId(id);
        updated.setName("E2");

        when(eventsService.updateEvent(eq(id), any(Event.class))).thenReturn(updated);

        mockMvc.perform(put("/events/update-event/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("E2"));
    }

    @Test
    void cancelEvent_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Event canceled = new Event();
        canceled.setId(id);

        when(eventsService.cancelEvent(id)).thenReturn(canceled);

        mockMvc.perform(put("/events/cancel-event/{id}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void publishEvent_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        Event published = new Event();
        published.setId(id);

        when(eventsService.publishEvent(id)).thenReturn(published);

        mockMvc.perform(put("/events/publish-event/{id}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

}
