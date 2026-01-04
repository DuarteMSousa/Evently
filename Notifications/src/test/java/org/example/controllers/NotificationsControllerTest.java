package org.example.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.controller.NotificationsController;
import org.example.dtos.NotificationCreateDTO;
import org.example.enums.NotificationChannel;
import org.example.enums.NotificationType;
import org.example.exceptions.InvalidNotificationException;
import org.example.exceptions.UserNotFoundException;
import org.example.models.Notification;
import org.example.service.NotificationsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(NotificationsController.class)
class NotificationsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private NotificationsService notificationsService;

    @Test
    void sendNotification_success_returns201AndDto() throws Exception {
        UUID userId = UUID.randomUUID();

        NotificationCreateDTO dto = new NotificationCreateDTO();
        dto.setUserId(userId);
        dto.setType(NotificationType.PAYMENT);
        dto.setTitle("T");
        dto.setBody("B");
        dto.setChannel(NotificationChannel.IN_APP);
        dto.setEmailTo(null);

        Notification saved = new Notification();
        saved.setId(UUID.randomUUID());
        saved.setUserId(userId);
        saved.setType(dto.getType());
        saved.setTitle(dto.getTitle());
        saved.setBody(dto.getBody());

        when(notificationsService.sendNotification(any(Notification.class), eq(NotificationChannel.IN_APP), isNull()))
                .thenReturn(saved);

        mockMvc.perform(post("/notifications/send-notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.title").value("T"))
                .andExpect(jsonPath("$.body").value("B"));
    }

    @Test
    void sendNotification_userNotFound_returns404() throws Exception {
        UUID userId = UUID.randomUUID();

        NotificationCreateDTO dto = new NotificationCreateDTO();
        dto.setUserId(userId);
        dto.setType(NotificationType.PAYMENT);
        dto.setTitle("T");
        dto.setBody("B");
        dto.setChannel(NotificationChannel.IN_APP);

        when(notificationsService.sendNotification(any(Notification.class), any(NotificationChannel.class), any()))
                .thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(post("/notifications/send-notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found"));
    }

    @Test
    void sendNotification_invalidPayload_returns400() throws Exception {
        UUID userId = UUID.randomUUID();

        NotificationCreateDTO dto = new NotificationCreateDTO();
        dto.setUserId(userId);
        dto.setChannel(NotificationChannel.IN_APP);

        when(notificationsService.sendNotification(any(Notification.class), any(NotificationChannel.class), nullable(String.class)))
                .thenThrow(new InvalidNotificationException("Type is required"));

        mockMvc.perform(post("/notifications/send-notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Type is required"));
    }

    @Test
    void sendNotification_genericError_returns400() throws Exception {
        UUID userId = UUID.randomUUID();

        NotificationCreateDTO dto = new NotificationCreateDTO();
        dto.setUserId(userId);
        dto.setType(NotificationType.PAYMENT);
        dto.setTitle("T");
        dto.setBody("B");
        dto.setChannel(NotificationChannel.IN_APP);

        when(notificationsService.sendNotification(any(Notification.class), any(NotificationChannel.class), any()))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/notifications/send-notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("boom"));
    }
}
