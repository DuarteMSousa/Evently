package org.example.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dtos.organizations.OrganizationCreateDTO;
import org.example.dtos.organizations.OrganizationUpdateDTO;
import org.example.exceptions.InvalidOrganizationException;
import org.example.exceptions.OrganizationNotFoundException;
import org.example.exceptions.PermissionDeniedException;
import org.example.models.Organization;
import org.example.service.OrganizationsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(OrganizationsController.class)
class OrganizationsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private OrganizationsService organizationsService;

    @Test
    void getOrganizations_success_returns200AndList() throws Exception {
        Organization o1 = new Organization(); o1.setId(UUID.randomUUID()); o1.setName("A");
        Organization o2 = new Organization(); o2.setId(UUID.randomUUID()); o2.setName("B");

        when(organizationsService.getOrganizations()).thenReturn(Arrays.asList(o1, o2));

        mockMvc.perform(get("/organizations/get-organizations").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getOrganization_success_returns200() throws Exception {
        UUID orgId = UUID.randomUUID();
        Organization org = new Organization();
        org.setId(orgId);
        org.setName("Org A");

        when(organizationsService.getOrganization(orgId)).thenReturn(org);

        mockMvc.perform(get("/organizations/get-organization/{orgId}", orgId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orgId.toString()))
                .andExpect(jsonPath("$.name").value("Org A"));
    }

    @Test
    void getOrganization_notFound_returns404() throws Exception {
        UUID orgId = UUID.randomUUID();
        when(organizationsService.getOrganization(orgId))
                .thenThrow(new OrganizationNotFoundException("Organization not found"));

        mockMvc.perform(get("/organizations/get-organization/{orgId}", orgId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Organization not found"));
    }

    @Test
    void createOrganization_success_returns201() throws Exception {
        OrganizationCreateDTO dto = new OrganizationCreateDTO();
        dto.setName("Org A");
        dto.setNipc("123456789");
        dto.setCreatedBy(UUID.randomUUID());

        Organization saved = new Organization();
        saved.setId(UUID.randomUUID());
        saved.setName("Org A");
        saved.setNipc("123456789");

        when(organizationsService.createOrganization(any(Organization.class))).thenReturn(saved);

        mockMvc.perform(post("/organizations/create-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.name").value("Org A"));
    }

    @Test
    void createOrganization_invalid_returns400() throws Exception {
        OrganizationCreateDTO dto = new OrganizationCreateDTO();
        dto.setName("Org A");

        when(organizationsService.createOrganization(any(Organization.class)))
                .thenThrow(new InvalidOrganizationException("Name is required"));

        mockMvc.perform(post("/organizations/create-organization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Name is required"));
    }

    @Test
    void updateOrganization_success_returns200() throws Exception {
        UUID orgId = UUID.randomUUID();

        OrganizationUpdateDTO dto = new OrganizationUpdateDTO();
        dto.setName("Nova");
        dto.setNipc("123456789");
        dto.setUpdatedBy(UUID.randomUUID());

        Organization updated = new Organization();
        updated.setId(orgId);
        updated.setName("Nova");

        when(organizationsService.updateOrganization(eq(orgId), any(Organization.class))).thenReturn(updated);

        mockMvc.perform(put("/organizations/update-organization/{orgId}", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orgId.toString()))
                .andExpect(jsonPath("$.name").value("Nova"));
    }

    @Test
    void updateOrganization_permissionDenied_returns403() throws Exception {
        UUID orgId = UUID.randomUUID();
        OrganizationUpdateDTO dto = new OrganizationUpdateDTO();
        dto.setName("Nova");
        dto.setNipc("123456789");
        dto.setUpdatedBy(UUID.randomUUID());

        when(organizationsService.updateOrganization(eq(orgId), any(Organization.class)))
                .thenThrow(new PermissionDeniedException("User is not the creator of the organization"));

        mockMvc.perform(put("/organizations/update-organization/{orgId}", orgId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(content().string("User is not the creator of the organization"));
    }
}
