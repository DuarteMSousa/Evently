package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private static final Marker EMAIL_SEND = MarkerFactory.getMarker("EMAIL_SEND");
    private static final Marker EMAIL_ERROR = MarkerFactory.getMarker("EMAIL_ERROR");

    @Autowired
    private JavaMailSender mailSender;

    /**
     * Sends a notification email using Spring's {@link JavaMailSender}.
     *
     *
     * @param to recipient email address
     * @param subject email subject
     * @param body email body content (plain text)
     * @throws RuntimeException if the underlying mail sender throws an exception while sending the message
     */
    public void sendNotificationEmail(String to, String subject, String body) {

        logger.info(EMAIL_SEND, "Sending email (to={}, subject={})", to, subject);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            logger.info(EMAIL_SEND, "Email sent successfully (to={})", to);

        } catch (Exception ex) {
            logger.error(EMAIL_ERROR, "Failed to send email (to={}, subject={})", to, subject, ex);
            throw ex;
        }
    }
}
