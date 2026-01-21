package org.example.services;

import jakarta.mail.internet.MimeMessage;
import org.example.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendNotificationEmailWithAttachment_success_sendsMimeMessage() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        byte[] bytes = "pdf".getBytes();

        assertDoesNotThrow(() ->
                emailService.sendNotificationEmailWithAttachment(
                        "a@b.com",
                        "S",
                        "B",
                        bytes,
                        "f.pdf"
                )
        );

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendNotificationEmailWithAttachment_failure_wrapsRuntimeException() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(mimeMessage);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                emailService.sendNotificationEmailWithAttachment(
                        "a@b.com",
                        "S",
                        "B",
                        "pdf".getBytes(),
                        "f.pdf"
                )
        );

        assertEquals("Failed to send email with attachment", ex.getMessage());
        verify(mailSender).send(mimeMessage);
    }

}
