package org.evently.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.evently.dtos.RefundRequests.RefundRequestCreateDTO;
import org.evently.enums.RefundRequestStatus;
import org.evently.exceptions.InvalidRefundRequestException;
import org.evently.exceptions.RefundRequestNotFoundException;
import org.evently.models.RefundRequest;
import org.evently.services.RefundRequestsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(RefundRequestsController.class)
class RefundRequestsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private RefundRequestsService refundRequestsService;

    @Test
    void getRefundRequest_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        RefundRequest rr = new RefundRequest();
        rr.setId(id);
        rr.setPaymentId(UUID.randomUUID());
        rr.setUserId(UUID.randomUUID());
        rr.setTitle("t");
        rr.setDescription("d");
        rr.setStatus(RefundRequestStatus.PENDING);
        rr.setCreatedAt(new Date());

        when(refundRequestsService.getRefundRequest(id)).thenReturn(rr);

        mockMvc.perform(get("/refunds/get-refund/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getRefundRequest_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(refundRequestsService.getRefundRequest(id))
                .thenThrow(new RefundRequestNotFoundException("Refund Request not found"));

        mockMvc.perform(get("/refunds/get-refund/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Refund Request not found"));
    }

    @Test
    void getRefundRequest_genericError_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        when(refundRequestsService.getRefundRequest(id))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/refunds/get-refund/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("boom"));
    }

    @Test
    void getRefundRequestsByUser_success_returns200Page() throws Exception {
        UUID userId = UUID.randomUUID();

        RefundRequest r1 = new RefundRequest(); r1.setId(UUID.randomUUID()); r1.setUserId(userId); r1.setStatus(RefundRequestStatus.PENDING);
        RefundRequest r2 = new RefundRequest(); r2.setId(UUID.randomUUID()); r2.setUserId(userId); r2.setStatus(RefundRequestStatus.APPROVED);

        Page<RefundRequest> page = new PageImpl<>(Arrays.asList(r1, r2));
        when(refundRequestsService.getRefundRequestsByUser(userId, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/refunds/get-by-user/{userId}/{pageNumber}/{pageSize}", userId, 1, 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void registerRefund_success_returns201() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        RefundRequestCreateDTO dto = new RefundRequestCreateDTO();
        dto.setPayment(paymentId);
        dto.setUser(userId);
        dto.setTitle("t");
        dto.setDescription("d");

        RefundRequest saved = new RefundRequest();
        saved.setId(UUID.randomUUID());
        saved.setPaymentId(paymentId);
        saved.setUserId(userId);
        saved.setTitle("t");
        saved.setDescription("d");
        saved.setStatus(RefundRequestStatus.PENDING);
        saved.setCreatedAt(new Date());

        when(refundRequestsService.createRefundRequest(any(RefundRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/refunds/register-refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void registerRefund_invalidPayload_returns400() throws Exception {
        RefundRequestCreateDTO dto = new RefundRequestCreateDTO();
        dto.setUser(UUID.randomUUID());

        when(refundRequestsService.createRefundRequest(any(RefundRequest.class)))
                .thenThrow(new InvalidRefundRequestException("Payment ID is required"));

        mockMvc.perform(post("/refunds/register-refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Payment ID is required"));
    }

    @Test
    void registerRefund_genericError_returns400() throws Exception {
        RefundRequestCreateDTO dto = new RefundRequestCreateDTO();
        dto.setPayment(UUID.randomUUID());
        dto.setUser(UUID.randomUUID());
        dto.setTitle("t");
        dto.setDescription("d");

        when(refundRequestsService.createRefundRequest(any(RefundRequest.class)))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/refunds/register-refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("boom"));
    }
}
