package org.evently.services;

import feign.FeignException;
import feign.Request;
import feign.Response;
import org.evently.clients.UsersClient;
import org.evently.exceptions.ExternalServiceException;
import org.evently.exceptions.InvalidRefundRequestMessageException;
import org.evently.exceptions.RefundRequestMessageNotFoundException;
import org.evently.exceptions.RefundRequestNotFoundException;
import org.evently.exceptions.externalServices.UserNotFoundException;
import org.evently.models.RefundRequest;
import org.evently.models.RefundRequestMessage;
import org.evently.publishers.RefundsEventsPublisher;
import org.evently.repositories.RefundRequestMessagesRepository;
import org.evently.repositories.RefundRequestsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefundRequestMessagesServiceTest {

    @Mock private RefundRequestMessagesRepository refundRequestMessagesRepository;
    @Mock private RefundRequestsRepository refundRequestsRepository;
    @Mock private UsersClient usersClient;
    @Mock private RefundsEventsPublisher refundsEventsPublisher;

    @InjectMocks private RefundRequestMessagesService messagesService;

    private UUID requestId;
    private UUID userId;

    @BeforeEach
    void setup() {
        requestId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void getRefundRequestMessage_exists_returnsMessage() {
        UUID id = UUID.randomUUID();
        RefundRequestMessage m = new RefundRequestMessage();
        m.setId(id);

        when(refundRequestMessagesRepository.findById(id)).thenReturn(Optional.of(m));

        RefundRequestMessage res = messagesService.getRefundRequestMessage(id);
        assertEquals(id, res.getId());
    }

    @Test
    void getRefundRequestMessage_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(refundRequestMessagesRepository.findById(id)).thenReturn(Optional.empty());

        RefundRequestMessageNotFoundException ex = assertThrows(RefundRequestMessageNotFoundException.class,
                () -> messagesService.getRefundRequestMessage(id));

        assertEquals("Refund Request Message not found", ex.getMessage());
    }

    @Test
    void sendRefundRequestMessage_userIdNull_throwsInvalid() {
        RefundRequestMessage m = baseMessage();
        m.setUserId(null);

        InvalidRefundRequestMessageException ex = assertThrows(InvalidRefundRequestMessageException.class,
                () -> messagesService.sendRefundRequestMessage(m));

        assertEquals("User ID is required", ex.getMessage());
        verifyNoInteractions(usersClient);
    }

    @Test
    void sendRefundRequestMessage_contentBlank_throwsInvalid() {
        RefundRequestMessage m = baseMessage();
        m.setContent("   ");

        InvalidRefundRequestMessageException ex = assertThrows(InvalidRefundRequestMessageException.class,
                () -> messagesService.sendRefundRequestMessage(m));

        assertEquals("Message content cannot be empty", ex.getMessage());
        verifyNoInteractions(usersClient);
    }

    @Test
    void sendRefundRequestMessage_refundRequestNull_throwsInvalid() {
        RefundRequestMessage m = baseMessage();
        m.setRefundRequest(null);

        InvalidRefundRequestMessageException ex = assertThrows(InvalidRefundRequestMessageException.class,
                () -> messagesService.sendRefundRequestMessage(m));

        assertEquals("Message must be linked to a Refund Request", ex.getMessage());
        verifyNoInteractions(usersClient);
    }

    @Test
    void sendRefundRequestMessage_userNotFound_throwsUserNotFound() {
        RefundRequestMessage m = baseMessage();
        when(usersClient.getUser(userId)).thenThrow(feignNotFound());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> messagesService.sendRefundRequestMessage(m));

        assertTrue(ex.getMessage().contains("User not found"));
        verify(refundRequestMessagesRepository, never()).save(any());
    }

    @Test
    void sendRefundRequestMessage_usersServiceError_throwsExternalServiceException() {
        RefundRequestMessage m = baseMessage();
        when(usersClient.getUser(userId)).thenThrow(feignGenericError(500));

        ExternalServiceException ex = assertThrows(ExternalServiceException.class,
                () -> messagesService.sendRefundRequestMessage(m));

        assertTrue(ex.getMessage().contains("Users service error"));
        verify(refundRequestMessagesRepository, never()).save(any());
    }

    @Test
    void sendRefundRequestMessage_refundRequestNotFound_throwsRefundRequestNotFound() {
        RefundRequestMessage m = baseMessage();
        when(usersClient.getUser(userId)).thenReturn(null);
        when(refundRequestsRepository.existsById(requestId)).thenReturn(false);

        RefundRequestNotFoundException ex = assertThrows(RefundRequestNotFoundException.class,
                () -> messagesService.sendRefundRequestMessage(m));

        assertEquals("Refund Request not found", ex.getMessage());
        verify(refundRequestMessagesRepository, never()).save(any());
    }

    @Test
    void sendRefundRequestMessage_success_savesAndPublishes() {
        RefundRequestMessage m = baseMessage();
        when(usersClient.getUser(userId)).thenReturn(null);
        when(refundRequestsRepository.existsById(requestId)).thenReturn(true);

        when(refundRequestMessagesRepository.save(any(RefundRequestMessage.class))).thenAnswer(inv -> {
            RefundRequestMessage saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        RefundRequestMessage saved = messagesService.sendRefundRequestMessage(m);

        assertNotNull(saved.getId());
        verify(refundsEventsPublisher).publishRefundRequestMessageSentEvent(saved);
    }

    @Test
    void getRefundRequestMessagesByRequest_pageSizeOutOfRange_correctsTo50() {
        RefundRequest rr = new RefundRequest(); rr.setId(requestId);

        when(refundRequestMessagesRepository.findAllByRefundRequest(eq(rr), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));


        Page<RefundRequestMessage> page = messagesService.getRefundRequestMessagesByRequest(rr, 1, 999);

        assertNotNull(page);
        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(refundRequestMessagesRepository).findAllByRefundRequest(eq(rr), captor.capture());
        assertEquals(50, captor.getValue().getPageSize());
    }

    @Test
    void getRefundRequestMessagesByRequest_pageNumberLessThan1_correctsTo0() {
        RefundRequest rr = new RefundRequest(); rr.setId(requestId);

        when(refundRequestMessagesRepository.findAllByRefundRequest(eq(rr), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));


        messagesService.getRefundRequestMessagesByRequest(rr, 0, 10);

        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(refundRequestMessagesRepository).findAllByRefundRequest(eq(rr), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
    }

    private RefundRequestMessage baseMessage() {
        RefundRequestMessage m = new RefundRequestMessage();
        m.setUserId(userId);
        m.setContent("hi");
        RefundRequest rr = new RefundRequest();
        rr.setId(requestId);
        m.setRefundRequest(rr);
        return m;
    }

    private FeignException.NotFound feignNotFound() {
        Request req = Request.create(Request.HttpMethod.GET, "/users/" + userId,
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        return new FeignException.NotFound("not found", req, null, null);
    }

    private FeignException feignGenericError(int status) {
        Request req = Request.create(Request.HttpMethod.GET, "/users/" + userId,
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        Response resp = Response.builder().status(status).reason("err").request(req).headers(Collections.emptyMap()).build();
        return FeignException.errorStatus("UsersClient#getUser", resp);
    }

}
