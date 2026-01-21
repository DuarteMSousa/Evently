package org.example.services;


import feign.FeignException;
import feign.Request;
import feign.Response;
import org.example.clients.UsersClient;
import org.example.dtos.externalServices.UserDTO;
import org.example.enums.NotificationChannel;
import org.example.enums.NotificationType;
import org.example.enums.externalServices.DecisionType;
import org.example.exceptions.InvalidNotificationException;
import org.example.models.Notification;
import org.example.models.OutBoxMessage;
import org.example.repositories.NotificationsRepository;
import org.example.repositories.OutBoxMessagesRepository;
import org.example.service.EmailService;
import org.example.service.NotificationsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationsServiceTest {

    @Mock private NotificationsRepository notificationsRepository;
    @Mock private OutBoxMessagesRepository outBoxMessagesRepository;
    @Mock private UsersClient usersClient;
    @Mock private EmailService emailService;

    @InjectMocks private NotificationsService notificationsService;

    private UUID userId;
    private UUID orderId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        // Default repository save behavior: assign ids
        lenient().when(notificationsRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            if (n.getId() == null) n.setId(UUID.randomUUID());
            return n;
        });

        lenient().when(outBoxMessagesRepository.save(any(OutBoxMessage.class))).thenAnswer(inv -> {
            OutBoxMessage m = inv.getArgument(0);
            if (m.getId() == null) m.setId(UUID.randomUUID());
            return m;
        });
    }

    // blank emailTo for EMAIL
    @Test
    void sendNotification_emailChannel_blankEmailTo_throwsInvalidNotificationException() {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(NotificationType.PAYMENT);
        n.setTitle("T");
        n.setBody("B");

        InvalidNotificationException ex = assertThrows(InvalidNotificationException.class,
                () -> notificationsService.sendNotification(n, NotificationChannel.EMAIL, "   "));

        assertEquals("emailTo is required for EMAIL channel", ex.getMessage());
        verifyNoInteractions(notificationsRepository);
        verifyNoInteractions(outBoxMessagesRepository);
        verifyNoInteractions(emailService);
    }

    // resolveUserEmail (indirect): ensure EMAIL is skipped when cannot resolve email

    @Test
    void notifyPaymentCaptured_usersClientBodyNull_skipsEmail() {
        when(usersClient.getUser(userId)).thenReturn(ResponseEntity.ok(null));

        notificationsService.notifyPaymentCaptured(userId, orderId, 10.5f);

        // IN_APP should be saved once
        verify(notificationsRepository, times(1)).save(any(Notification.class));
        // outbox created once for IN_APP
        verify(outBoxMessagesRepository, times(1)).save(any(OutBoxMessage.class));
        // email not attempted
        verify(emailService, never()).sendNotificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void notifyPaymentCaptured_usersClientEmailBlank_skipsEmail() {
        UserDTO user = new UserDTO();
        user.setEmail("   ");
        when(usersClient.getUser(userId)).thenReturn(ResponseEntity.ok(user));

        notificationsService.notifyPaymentCaptured(userId, orderId, 10.5f);

        verify(notificationsRepository, times(1)).save(any(Notification.class));
        verify(outBoxMessagesRepository, times(1)).save(any(OutBoxMessage.class));
        verify(emailService, never()).sendNotificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void notifyPaymentCaptured_usersClientNotFound_skipsEmail() {
        when(usersClient.getUser(userId)).thenThrow(feignNotFound());

        notificationsService.notifyPaymentCaptured(userId, orderId, 10.5f);

        verify(notificationsRepository, times(1)).save(any(Notification.class));
        verify(outBoxMessagesRepository, times(1)).save(any(OutBoxMessage.class));
        verify(emailService, never()).sendNotificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void notifyPaymentCaptured_usersClientFeignError_skipsEmail() {
        when(usersClient.getUser(userId)).thenThrow(feignGenericError(500));

        notificationsService.notifyPaymentCaptured(userId, orderId, 10.5f);

        verify(notificationsRepository, times(1)).save(any(Notification.class));
        verify(outBoxMessagesRepository, times(1)).save(any(OutBoxMessage.class));
        verify(emailService, never()).sendNotificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void notifyPaymentCaptured_usersClientRuntimeException_skipsEmail() {
        when(usersClient.getUser(userId)).thenThrow(new RuntimeException("boom"));

        notificationsService.notifyPaymentCaptured(userId, orderId, 10.5f);

        verify(notificationsRepository, times(1)).save(any(Notification.class));
        verify(outBoxMessagesRepository, times(1)).save(any(OutBoxMessage.class));
        verify(emailService, never()).sendNotificationEmail(anyString(), anyString(), anyString());
    }

    // Domain notify tests

    @Test
    void notifyPaymentCaptured_emailResolved_sendsInAppAndEmail() {
        UserDTO user = new UserDTO();
        user.setEmail("a@b.com");
        when(usersClient.getUser(userId)).thenReturn(ResponseEntity.ok(user));

        notificationsService.notifyPaymentCaptured(userId, orderId, 12.34f);

        // IN_APP notification saved + EMAIL notification saved => 2 saves
        verify(notificationsRepository, times(2)).save(any(Notification.class));
        // outbox created twice (IN_APP + EMAIL) and updated once for EMAIL SENT/FAILED
        verify(outBoxMessagesRepository, atLeast(2)).save(any(OutBoxMessage.class));
        verify(emailService, times(1)).sendNotificationEmail(eq("a@b.com"), eq("Pagamento confirmado"), contains(orderId.toString()));
    }

    @Test
    void notifyRefundDecision_approve_withoutDescription_hasApprovedTitle_andNoMotivo() {
        UserDTO user = new UserDTO();
        user.setEmail("a@b.com");
        when(usersClient.getUser(userId)).thenReturn(ResponseEntity.ok(user));

        UUID paymentId = UUID.randomUUID();

        notificationsService.notifyRefundDecision(userId, paymentId, DecisionType.APPROVE, null);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        // first save is IN_APP, second save is EMAIL (same title/body)
        verify(notificationsRepository, atLeast(1)).save(captor.capture());

        Notification first = captor.getAllValues().get(0);
        assertEquals("Reembolso aprovado", first.getTitle());
        assertFalse(first.getBody().contains("Motivo:"), "Body não deve conter 'Motivo:' quando description é null");
        assertTrue(first.getBody().contains(paymentId.toString()));
    }

    @Test
    void notifyRefundDecision_reject_withDescription_hasRejectedTitle_andMotivo() {
        UserDTO user = new UserDTO();
        user.setEmail("a@b.com");
        when(usersClient.getUser(userId)).thenReturn(ResponseEntity.ok(user));

        UUID paymentId = UUID.randomUUID();

        notificationsService.notifyRefundDecision(userId, paymentId, DecisionType.REJECT, "X");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationsRepository, atLeast(1)).save(captor.capture());

        Notification first = captor.getAllValues().get(0);
        assertEquals("Reembolso rejeitado", first.getTitle());
        assertTrue(first.getBody().contains("Motivo: X"));
        assertTrue(first.getBody().contains(paymentId.toString()));
    }

    @Test
    void notifyPdfGenerated_bodyContainsLink() {
        UserDTO user = new UserDTO();
        user.setEmail("a@b.com");
        when(usersClient.getUser(userId)).thenReturn(ResponseEntity.ok(user));

        String url = "http://example/p.pdf";
        notificationsService.notifyPdfGenerated(userId, orderId, "f.pdf", url);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationsRepository, atLeast(1)).save(captor.capture());

        Notification first = captor.getAllValues().get(0);
        assertTrue(first.getBody().contains("Link: " + url));
    }

    // notifyPdfGeneratedWithAttachment

    @Test
    void notifyPdfGeneratedWithAttachment_emailResolved_sendsAttachmentEmail() {
        UserDTO user = new UserDTO();
        user.setEmail("a@b.com");
        when(usersClient.getUser(userId)).thenReturn(ResponseEntity.ok(user));

        byte[] pdf = "pdf".getBytes();

        notificationsService.notifyPdfGeneratedWithAttachment(userId, orderId, "f.pdf", pdf);

        // IN_APP persisted once
        verify(notificationsRepository, times(1)).save(any(Notification.class));
        // outbox created once for IN_APP
        verify(outBoxMessagesRepository, times(1)).save(any(OutBoxMessage.class));

        verify(emailService, times(1)).sendNotificationEmailWithAttachment(
                eq("a@b.com"),
                eq("PDF gerado"),
                anyString(),
                eq(pdf),
                eq("f.pdf")
        );
    }

    @Test
    void notifyPdfGeneratedWithAttachment_emailNotResolved_doesNotSendAttachmentEmail() {
        when(usersClient.getUser(userId)).thenReturn(ResponseEntity.ok(null));

        byte[] pdf = "pdf".getBytes();
        notificationsService.notifyPdfGeneratedWithAttachment(userId, orderId, "f.pdf", pdf);

        verify(notificationsRepository, times(1)).save(any(Notification.class));
        verify(outBoxMessagesRepository, times(1)).save(any(OutBoxMessage.class));
        verify(emailService, never()).sendNotificationEmailWithAttachment(anyString(), anyString(), anyString(), any(), anyString());
    }

    // Helpers for Feign exceptions

    private FeignException.NotFound feignNotFound() {
        Request req = Request.create(Request.HttpMethod.GET, "/users/" + userId,
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        Response resp = Response.builder()
                .status(404)
                .reason("Not Found")
                .request(req)
                .headers(Collections.emptyMap())
                .build();
        return new FeignException.NotFound("not found", req, null, null);
    }

    private FeignException feignGenericError(int status) {
        Request req = Request.create(Request.HttpMethod.GET, "/users/" + userId,
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        Response resp = Response.builder()
                .status(status)
                .reason("err")
                .request(req)
                .headers(Collections.emptyMap())
                .build();
        return FeignException.errorStatus("UsersClient#getUser", resp);
    }

}
