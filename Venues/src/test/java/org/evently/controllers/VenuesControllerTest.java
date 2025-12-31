package org.evently.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.evently.dtos.venues.VenueCreateDTO;
import org.evently.dtos.venues.VenueSearchDTO;
import org.evently.exceptions.InvalidVenueException;
import org.evently.exceptions.VenueAlreadyDeactivatedException;
import org.evently.exceptions.VenueNotFoundException;
import org.evently.models.Venue;
import org.evently.service.VenuesService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(VenuesController.class)
class VenuesControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private VenuesService venuesService;

    @Test
    void getVenue_success_returns200AndDto() throws Exception {
        UUID id = UUID.randomUUID();
        Venue venue = new Venue();
        venue.setId(id);
        venue.setName("Altice Arena");

        when(venuesService.getVenue(id)).thenReturn(venue);

        mockMvc.perform(get("/venues/get-venue/{id}", id)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Altice Arena"));
    }

    @Test
    void getVenue_notFound_returns404AndMessage() throws Exception {
        UUID id = UUID.randomUUID();
        when(venuesService.getVenue(id)).thenThrow(new VenueNotFoundException("Venue not found"));

        mockMvc.perform(get("/venues/get-venue/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Venue not found"));
    }

    @Test
    void getVenue_genericError_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        when(venuesService.getVenue(id)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/venues/get-venue/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("boom"));
    }

    @Test
    void createVenue_success_returns201AndDto() throws Exception {
        VenueCreateDTO dto = new VenueCreateDTO();
        dto.setName("Altice Arena");
        dto.setCity("Lisboa");
        dto.setCountry("Portugal");

        Venue saved = new Venue();
        saved.setId(UUID.randomUUID());
        saved.setName("Altice Arena");

        when(venuesService.createVenue(any(Venue.class))).thenReturn(saved);

        mockMvc.perform(post("/venues/create-venue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.name").value("Altice Arena"));
    }

    @Test
    void createVenue_invalidPayload_returns400AndMessage() throws Exception {
        VenueCreateDTO dto = new VenueCreateDTO();
        dto.setName("X");

        when(venuesService.createVenue(any(Venue.class)))
                .thenThrow(new InvalidVenueException("Name is required"));

        mockMvc.perform(post("/venues/create-venue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Name is required"));
    }

    @Test
    void searchVenues_success_returns200AndList() throws Exception {
        VenueSearchDTO dto = new VenueSearchDTO();

        Venue v1 = new Venue(); v1.setId(UUID.randomUUID()); v1.setName("Arena 1");
        Venue v2 = new Venue(); v2.setId(UUID.randomUUID()); v2.setName("Arena 2");

        when(venuesService.searchVenues(any(VenueSearchDTO.class))).thenReturn(List.of(v1, v2));

        mockMvc.perform(post("/venues/search-venues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Arena 1"));
    }

    @Test
    void searchVenues_invalidCriteria_returns400AndMessage() throws Exception {
        VenueSearchDTO dto = new VenueSearchDTO();

        when(venuesService.searchVenues(any(VenueSearchDTO.class)))
                .thenThrow(new InvalidVenueException("minCapacity must be >= 0"));

        mockMvc.perform(post("/venues/search-venues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("minCapacity must be >= 0"));
    }

    @Test
    void deactivateVenue_success_returns200AndDto() throws Exception {
        UUID id = UUID.randomUUID();
        Venue updated = new Venue();
        updated.setId(id);
        updated.setActive(false);

        when(venuesService.deactivateVenue(id)).thenReturn(updated);

        mockMvc.perform(put("/venues/deactivate-venue/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void deactivateVenue_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(venuesService.deactivateVenue(id)).thenThrow(new VenueNotFoundException("Venue not found"));

        mockMvc.perform(put("/venues/deactivate-venue/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Venue not found"));
    }

    @Test
    void deactivateVenue_alreadyDeactivated_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(venuesService.deactivateVenue(id))
                .thenThrow(new VenueAlreadyDeactivatedException("Venue already deactivated"));

        mockMvc.perform(put("/venues/deactivate-venue/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(content().string("Venue already deactivated"));
    }
}
