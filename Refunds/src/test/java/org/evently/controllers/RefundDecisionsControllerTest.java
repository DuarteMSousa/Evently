package org.evently.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.evently.dtos.RefundDecisions.RefundDecisionCreateDTO;
import org.evently.enums.DecisionType;
import org.evently.exceptions.InvalidRefundRequestDecisionException;
import org.evently.exceptions.RefundRequestDecisionNotFoundException;
import org.evently.exceptions.RefundRequestNotFoundException;
import org.evently.models.RefundDecision;
import org.evently.models.RefundRequest;
import org.evently.services.RefundDecisionsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(RefundDecisionsController.class)
class RefundDecisionsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private RefundDecisionsService refundDecisionsService;

    @Test
    void getDecision_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        RefundDecision d = new RefundDecision();
        d.setId(id);
        d.setDecidedBy(UUID.randomUUID());
        d.setDecisionType(DecisionType.APPROVE);
        d.setDescription("ok");
        d.setCreatedAt(new Date());
        RefundRequest rr = new RefundRequest(); rr.setId(UUID.randomUUID());
        d.setRefundRequest(rr);

        when(refundDecisionsService.getRefundDecision(id)).thenReturn(d);

        mockMvc.perform(get("/refunds/decisions/get-decision/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.decisionType").value("APPROVE"))
                .andExpect(jsonPath("$.refundRequestId").value(rr.getId().toString()));
    }

    @Test
    void getDecision_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(refundDecisionsService.getRefundDecision(id))
                .thenThrow(new RefundRequestDecisionNotFoundException("Refund Decision not found"));

        mockMvc.perform(get("/refunds/decisions/get-decision/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Refund Decision not found"));
    }

    @Test
    void getDecision_genericError_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        when(refundDecisionsService.getRefundDecision(id))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/refunds/decisions/get-decision/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("boom"));
    }

    @Test
    void getDecisionByRequest_success_returns200() throws Exception {
        UUID requestId = UUID.randomUUID();
        RefundDecision d = new RefundDecision();
        d.setId(UUID.randomUUID());
        d.setDecidedBy(UUID.randomUUID());
        d.setDecisionType(DecisionType.REJECT);
        d.setDescription("no");
        RefundRequest rr = new RefundRequest(); rr.setId(requestId);
        d.setRefundRequest(rr);

        when(refundDecisionsService.getRefundDecisionByRequest(requestId)).thenReturn(d);

        mockMvc.perform(get("/refunds/decisions/get-decision-by-request/{requestId}", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundRequestId").value(requestId.toString()))
                .andExpect(jsonPath("$.decisionType").value("REJECT"));
    }

    @Test
    void getDecisionByRequest_notFound_returns404() throws Exception {
        UUID requestId = UUID.randomUUID();
        when(refundDecisionsService.getRefundDecisionByRequest(requestId))
                .thenThrow(new RefundRequestDecisionNotFoundException("Refund Decision not found"));

        mockMvc.perform(get("/refunds/decisions/get-decision-by-request/{requestId}", requestId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Refund Decision not found"));
    }

    @Test
    void registerDecision_success_returns201() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID decidedBy = UUID.randomUUID();

        RefundDecisionCreateDTO dto = new RefundDecisionCreateDTO();
        dto.setRefundRequestId(requestId);
        dto.setDecidedBy(decidedBy);
        dto.setDecisionType(DecisionType.APPROVE);
        dto.setDescription("ok");

        RefundDecision saved = new RefundDecision();
        saved.setId(UUID.randomUUID());
        saved.setDecidedBy(decidedBy);
        saved.setDecisionType(DecisionType.APPROVE);
        saved.setDescription("ok");
        saved.setCreatedAt(new Date());
        RefundRequest rr = new RefundRequest(); rr.setId(requestId);
        saved.setRefundRequest(rr);

        when(refundDecisionsService.registerRefundDecision(any(RefundDecision.class))).thenReturn(saved);

        mockMvc.perform(post("/refunds/decisions/register-decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.refundRequestId").value(requestId.toString()))
                .andExpect(jsonPath("$.decisionType").value("APPROVE"));
    }

    @Test
    void registerDecision_refundRequestNotFound_returns404() throws Exception {
        RefundDecisionCreateDTO dto = new RefundDecisionCreateDTO();
        dto.setRefundRequestId(UUID.randomUUID());
        dto.setDecidedBy(UUID.randomUUID());
        dto.setDecisionType(DecisionType.APPROVE);
        dto.setDescription("ok");

        when(refundDecisionsService.registerRefundDecision(any(RefundDecision.class)))
                .thenThrow(new RefundRequestNotFoundException("Refund Request not found"));

        mockMvc.perform(post("/refunds/decisions/register-decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Refund Request not found"));
    }

    @Test
    void registerDecision_invalidPayload_returns400() throws Exception {
        RefundDecisionCreateDTO dto = new RefundDecisionCreateDTO();
        dto.setRefundRequestId(UUID.randomUUID());

        when(refundDecisionsService.registerRefundDecision(any(RefundDecision.class)))
                .thenThrow(new InvalidRefundRequestDecisionException("DecidedBy is required"));

        mockMvc.perform(post("/refunds/decisions/register-decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("DecidedBy is required"));
    }

    @Test
    void registerDecision_genericError_returns400() throws Exception {
        RefundDecisionCreateDTO dto = new RefundDecisionCreateDTO();
        dto.setRefundRequestId(UUID.randomUUID());
        dto.setDecidedBy(UUID.randomUUID());
        dto.setDecisionType(DecisionType.APPROVE);
        dto.setDescription("ok");

        when(refundDecisionsService.registerRefundDecision(any(RefundDecision.class)))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/refunds/decisions/register-decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("boom"));
    }

}
