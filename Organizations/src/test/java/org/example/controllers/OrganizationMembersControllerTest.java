package org.example.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.exceptions.*;
import org.example.models.Member;
import org.example.models.MemberId;
import org.example.models.Organization;
import org.example.service.OrganizationMembersService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(OrganizationMembersController.class)
class OrganizationMembersControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private OrganizationMembersService organizationMembersService;

    @Test
    void getMembers_success_returns200AndList() throws Exception {
        UUID orgId = UUID.randomUUID();

        Member m1 = new Member(); m1.setId(new MemberId(orgId, UUID.randomUUID()));
        Member m2 = new Member(); m2.setId(new MemberId(orgId, UUID.randomUUID()));

        when(organizationMembersService.getMembers(orgId)).thenReturn(Arrays.asList(m1, m2));

        mockMvc.perform(get("/organizations/get-members/{orgId}", orgId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].organizationId").value(orgId.toString()));
    }

    @Test
    void addMember_success_returns201() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        Member member = new Member();
        member.setId(new MemberId(orgId, userId));
        Organization org = new Organization(); org.setId(orgId);
        member.setOrganization(org);

        when(organizationMembersService.addMember(orgId, userId, requesterId)).thenReturn(member);

        mockMvc.perform(post("/organizations/add-member/{orgId}/{userId}", orgId, userId)
                        .param("requesterId", requesterId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizationId").value(orgId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void addMember_userNotFound_returns405() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        when(organizationMembersService.addMember(orgId, userId, requesterId))
                .thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(post("/organizations/add-member/{orgId}/{userId}", orgId, userId)
                        .param("requesterId", requesterId.toString()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().string("User not found"));
    }

    @Test
    void removeMember_success_returns200() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        mockMvc.perform(delete("/organizations/remove-member/{orgId}/{userId}", orgId, userId)
                        .param("requesterId", requesterId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string("Member removed"));
    }

    @Test
    void removeMember_memberNotFound_returns405() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        doThrow(new MemberNotFoundException("Member not found"))
                .when(organizationMembersService).removeMember(orgId, userId, requesterId);

        mockMvc.perform(delete("/organizations/remove-member/{orgId}/{userId}", orgId, userId)
                        .param("requesterId", requesterId.toString()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().string("Member not found"));
    }
}
