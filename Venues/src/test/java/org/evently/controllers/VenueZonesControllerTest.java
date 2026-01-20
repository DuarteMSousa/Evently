package org.evently.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.evently.dtos.venueszone.VenueZoneCreateDTO;
import org.evently.dtos.venueszone.VenueZoneUpdateDTO;
import org.evently.exceptions.InvalidVenueZoneException;
import org.evently.exceptions.VenueNotFoundException;
import org.evently.exceptions.VenueZoneNotFoundException;
import org.evently.models.Venue;
import org.evently.models.VenueZone;
import org.evently.service.VenueZonesService;
import org.junit.jupiter.api.Test;
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
@WebMvcTest(VenueZonesController.class)
class VenueZonesControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private VenueZonesService venueZonesService;

    @Test
    void createZone_success_returns201AndDto() throws Exception {
        UUID venueId = UUID.randomUUID();
        VenueZoneCreateDTO dto = new VenueZoneCreateDTO();
        dto.setVenueId(venueId);
        dto.setName("Zona A");
        dto.setCapacity(50);
        dto.setCreatedBy(UUID.randomUUID());

        VenueZone saved = new VenueZone();
        saved.setId(UUID.randomUUID());
        saved.setName("Zona A");
        saved.setCapacity(50);
        Venue v = new Venue(); v.setId(venueId); v.setName("Altice Arena");
        saved.setVenue(v);

        when(venueZonesService.createVenueZone(eq(venueId), any(VenueZone.class))).thenReturn(saved);

        mockMvc.perform(post("/venues/zones/create-zone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.name").value("Zona A"))
                .andExpect(jsonPath("$.venueId").value(venueId.toString()))
                .andExpect(jsonPath("$.venueName").value("Altice Arena"));
    }

    @Test
    void createZone_venueNotFound_returns404() throws Exception {
        UUID venueId = UUID.randomUUID();
        VenueZoneCreateDTO dto = new VenueZoneCreateDTO();
        dto.setVenueId(venueId);

        when(venueZonesService.createVenueZone(eq(venueId), any(VenueZone.class)))
                .thenThrow(new VenueNotFoundException("Venue not found"));

        mockMvc.perform(post("/venues/zones/create-zone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Venue not found"));
    }

    @Test
    void createZone_invalidPayload_returns400() throws Exception {
        UUID venueId = UUID.randomUUID();
        VenueZoneCreateDTO dto = new VenueZoneCreateDTO();
        dto.setVenueId(venueId);

        when(venueZonesService.createVenueZone(eq(venueId), any(VenueZone.class)))
                .thenThrow(new InvalidVenueZoneException("Zone name is required"));

        mockMvc.perform(post("/venues/zones/create-zone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Zone name is required"));
    }

    @Test
    void getZone_success_returns200() throws Exception {
        UUID zoneId = UUID.randomUUID();

        VenueZone zone = new VenueZone();
        zone.setId(zoneId);
        zone.setName("Zona A");
        Venue v = new Venue(); v.setId(UUID.randomUUID()); v.setName("Altice Arena");
        zone.setVenue(v);

        when(venueZonesService.getVenueZone(zoneId)).thenReturn(zone);

        mockMvc.perform(get("/venues/zones/get-zone/{id}", zoneId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(zoneId.toString()))
                .andExpect(jsonPath("$.name").value("Zona A"))
                .andExpect(jsonPath("$.venueName").value("Altice Arena"));
    }

    @Test
    void getZone_notFound_returns404() throws Exception {
        UUID zoneId = UUID.randomUUID();
        when(venueZonesService.getVenueZone(zoneId))
                .thenThrow(new VenueZoneNotFoundException("Venue zone not found"));

        mockMvc.perform(get("/venues/zones/get-zone/{id}", zoneId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Venue zone not found"));
    }

    @Test
    void getZonesByVenue_success_returns200AndList() throws Exception {
        UUID venueId = UUID.randomUUID();

        VenueZone z1 = new VenueZone(); z1.setId(UUID.randomUUID()); z1.setName("Z1");
        VenueZone z2 = new VenueZone(); z2.setId(UUID.randomUUID()); z2.setName("Z2");
        Venue v = new Venue(); v.setId(venueId); v.setName("Altice Arena");
        z1.setVenue(v); z2.setVenue(v);

        when(venueZonesService.getVenueZonesByVenue(venueId)).thenReturn(List.of(z1, z2));

        mockMvc.perform(get("/venues/zones/by-venue/{venueId}", venueId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].venueId").value(venueId.toString()));
    }

    @Test
    void getZonesByVenue_venueNotFound_returns404() throws Exception {
        UUID venueId = UUID.randomUUID();
        when(venueZonesService.getVenueZonesByVenue(venueId))
                .thenThrow(new VenueNotFoundException("Venue not found"));

        mockMvc.perform(get("/venues/zones/by-venue/{venueId}", venueId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Venue not found"));
    }

    @Test
    void updateZone_success_returns200() throws Exception {
        UUID zoneId = UUID.randomUUID();
        VenueZoneUpdateDTO dto = new VenueZoneUpdateDTO();
        dto.setId(zoneId);
        dto.setName("Nova");
        dto.setCapacity(20);
        dto.setUpdatedBy(UUID.randomUUID());

        VenueZone updated = new VenueZone();
        updated.setId(zoneId);
        updated.setName("Nova");
        updated.setCapacity(20);
        Venue v = new Venue(); v.setId(UUID.randomUUID()); v.setName("Altice Arena");
        updated.setVenue(v);

        when(venueZonesService.updateVenueZone(eq(zoneId), any(VenueZone.class))).thenReturn(updated);

        mockMvc.perform(put("/venues/zones/update-zone/{id}", zoneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(zoneId.toString()))
                .andExpect(jsonPath("$.name").value("Nova"));
    }

    @Test
    void updateZone_zoneNotFound_returns404() throws Exception {
        UUID zoneId = UUID.randomUUID();
        VenueZoneUpdateDTO dto = new VenueZoneUpdateDTO();
        dto.setUpdatedBy(UUID.randomUUID());

        when(venueZonesService.updateVenueZone(eq(zoneId), any(VenueZone.class)))
                .thenThrow(new VenueZoneNotFoundException("Venue zone not found"));

        mockMvc.perform(put("/venues/zones/update-zone/{id}", zoneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Venue zone not found"));
    }

    @Test
    void updateZone_invalidPayload_returns400() throws Exception {
        UUID zoneId = UUID.randomUUID();
        VenueZoneUpdateDTO dto = new VenueZoneUpdateDTO();

        when(venueZonesService.updateVenueZone(eq(zoneId), any(VenueZone.class)))
                .thenThrow(new InvalidVenueZoneException("UpdatedBy is required"));

        mockMvc.perform(put("/venues/zones/update-zone/{id}", zoneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("UpdatedBy is required"));
    }

}
