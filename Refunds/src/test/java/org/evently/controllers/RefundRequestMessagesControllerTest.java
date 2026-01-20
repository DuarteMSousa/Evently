package org.evently.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.evently.dtos.RefundRequestMessages.RefundRequestMessageCreateDTO;
import org.evently.exceptions.InvalidRefundRequestMessageException;
import org.evently.exceptions.RefundRequestMessageNotFoundException;
import org.evently.exceptions.RefundRequestNotFoundException;
import org.evently.models.RefundRequest;
import org.evently.models.RefundRequestMessage;
import org.evently.services.RefundRequestMessagesService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(RefundRequestMessagesController.class)
class RefundRequestMessagesControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private RefundRequestMessagesService messagesService;

    @Test
    void getMessage_success_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        RefundRequestMessage m = new RefundRequestMessage();
        m.setId(id);
        m.setUserId(UUID.randomUUID());
        m.setContent("hi");
        m.setCreatedAt(new Date());
        RefundRequest rr = new RefundRequest(); rr.setId(UUID.randomUUID());
        m.setRefundRequest(rr);

        when(messagesService.getRefundRequestMessage(id)).thenReturn(m);

        mockMvc.perform(get("/refunds/messages/get-message/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.content").value("hi"))
                .andExpect(jsonPath("$.refundRequestId").value(rr.getId().toString()));
    }

    @Test
    void getMessage_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(messagesService.getRefundRequestMessage(id))
                .thenThrow(new RefundRequestMessageNotFoundException("Refund Request Message not found"));

        mockMvc.perform(get("/refunds/messages/get-message/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Refund Request Message not found"));
    }

    @Test
    void getMessage_genericError_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        when(messagesService.getRefundRequestMessage(id))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/refunds/messages/get-message/{id}", id))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("boom"));
    }

    @Test
    void getMessagesByRequest_success_returns200Page() throws Exception {
        UUID requestId = UUID.randomUUID();
        RefundRequest rr = new RefundRequest(); rr.setId(requestId);

        RefundRequestMessage m1 = new RefundRequestMessage(); m1.setId(UUID.randomUUID()); m1.setContent("c1"); m1.setRefundRequest(rr);
        RefundRequestMessage m2 = new RefundRequestMessage(); m2.setId(UUID.randomUUID()); m2.setContent("c2"); m2.setRefundRequest(rr);

        Page<RefundRequestMessage> page = new PageImpl<>(Arrays.asList(m1, m2));

        when(messagesService.getRefundRequestMessagesByRequest(any(RefundRequest.class), eq(1), eq(10)))
                .thenReturn(page);

        mockMvc.perform(get("/refunds/messages/request/{requestId}/{pageNumber}/{pageSize}", requestId, 1, 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].content").value("c1"));
    }

    @Test
    void sendMessage_success_returns201() throws Exception {
        UUID requestId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        RefundRequestMessageCreateDTO dto = new RefundRequestMessageCreateDTO();
        dto.setRefundRequestId(requestId);
        dto.setUser(userId);
        dto.setContent("hello");

        RefundRequestMessage saved = new RefundRequestMessage();
        saved.setId(UUID.randomUUID());
        saved.setUserId(userId);
        saved.setContent("hello");
        saved.setCreatedAt(new Date());
        RefundRequest rr = new RefundRequest(); rr.setId(requestId);
        saved.setRefundRequest(rr);

        when(messagesService.sendRefundRequestMessage(any(RefundRequestMessage.class))).thenReturn(saved);

        mockMvc.perform(post("/refunds/messages/send-message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.refundRequestId").value(requestId.toString()))
                .andExpect(jsonPath("$.content").value("hello"));
    }

    @Test
    void sendMessage_refundNotFound_returns404() throws Exception {
        RefundRequestMessageCreateDTO dto = new RefundRequestMessageCreateDTO();
        dto.setRefundRequestId(UUID.randomUUID());
        dto.setUser(UUID.randomUUID());
        dto.setContent("hello");

        when(messagesService.sendRefundRequestMessage(any(RefundRequestMessage.class)))
                .thenThrow(new RefundRequestNotFoundException("Refund Request not found"));

        mockMvc.perform(post("/refunds/messages/send-message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Refund Request not found"));
    }

    @Test
    void sendMessage_invalidPayload_returns400() throws Exception {
        RefundRequestMessageCreateDTO dto = new RefundRequestMessageCreateDTO();
        dto.setRefundRequestId(UUID.randomUUID());

        when(messagesService.sendRefundRequestMessage(any(RefundRequestMessage.class)))
                .thenThrow(new InvalidRefundRequestMessageException("User ID is required"));

        mockMvc.perform(post("/refunds/messages/send-message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User ID is required"));
    }

    @Test
    void sendMessage_genericError_returns400() throws Exception {
        RefundRequestMessageCreateDTO dto = new RefundRequestMessageCreateDTO();
        dto.setRefundRequestId(UUID.randomUUID());
        dto.setUser(UUID.randomUUID());
        dto.setContent("hello");

        when(messagesService.sendRefundRequestMessage(any(RefundRequestMessage.class)))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/refunds/messages/send-message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("boom"));
    }

}
