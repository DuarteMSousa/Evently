package org.example.service;

import jakarta.transaction.Transactional;
import org.example.exceptions.InvalidNotificationException;
import org.example.exceptions.UserNotFoundException;
import org.example.models.Notification;
import org.example.models.OutBoxMessage;
import org.example.repositories.NotificationsRepository;
import org.example.repositories.OutBoxMessagesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class NotificationsService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationsService.class);

    private static final Marker NOTIF_VALIDATE = MarkerFactory.getMarker("NOTIFICATION_VALIDATE");
    private static final Marker NOTIF_SEND = MarkerFactory.getMarker("NOTIFICATION_SEND");
    private static final Marker OUTBOX_CREATE = MarkerFactory.getMarker("OUTBOX_CREATE");
    private static final Marker OUTBOX_UPDATE = MarkerFactory.getMarker("OUTBOX_UPDATE");
    private static final Marker EMAIL_FLOW = MarkerFactory.getMarker("NOTIFICATION_EMAIL_FLOW");

    @Autowired
    private NotificationsRepository notificationsRepository;

    @Autowired
    private OutBoxMessagesRepository outBoxMessagesRepository;

    @Autowired
    private EmailService emailService;

    private void validateNotification(Notification notification,
                                      String channel,
                                      String emailTo) {

        logger.debug(NOTIF_VALIDATE,
                "Validating notification (userId={}, type={}, channel={}, emailToPresent={})",
                notification != null ? notification.getUserId() : null,
                notification != null ? notification.getType() : null,
                channel,
                emailTo != null
        );

        if (notification.getUserId() == null) {
            logger.warn(NOTIF_VALIDATE, "Missing userId");
            throw new InvalidNotificationException("UserId is required");
        }
        if (notification.getType() == null) {
            logger.warn(NOTIF_VALIDATE, "Missing type (userId={})", notification.getUserId());
            throw new InvalidNotificationException("Type is required");
        }
        if (notification.getTitle() == null) {
            logger.warn(NOTIF_VALIDATE, "Missing title (userId={}, type={})", notification.getUserId(), notification.getType());
            throw new InvalidNotificationException("Title is required");
        }
        if (notification.getBody() == null) {
            logger.warn(NOTIF_VALIDATE, "Missing body (userId={}, type={})", notification.getUserId(), notification.getType());
            throw new InvalidNotificationException("Body is required");
        }
        if (channel == null) {
            logger.warn(NOTIF_VALIDATE, "Missing channel (userId={})", notification.getUserId());
            throw new InvalidNotificationException("Channel is required");
        }

        if ("EMAIL".equalsIgnoreCase(channel) && emailTo == null) {
            logger.warn(NOTIF_VALIDATE, "EMAIL channel requires emailTo (userId={})", notification.getUserId());
            throw new InvalidNotificationException("emailTo is required for EMAIL channel");
        }

        if (notification.getUserId().equals(new UUID(0L, 0L))) {
            logger.warn(NOTIF_VALIDATE, "User not found (userId={})", notification.getUserId());
            throw new UserNotFoundException("User not found");
        }
    }

    @Transactional
    public Notification sendNotification(Notification notification,
                                         String channel,
                                         String emailTo) {

        logger.info(NOTIF_SEND,
                "Send notification requested (userId={}, type={}, channel={})",
                notification != null ? notification.getUserId() : null,
                notification != null ? notification.getType() : null,
                channel
        );

        validateNotification(notification, channel, emailTo);

        // 1) Guardar notificação
        notification.setStatus("UNREAD");
        Notification saved = notificationsRepository.save(notification);

        logger.info(NOTIF_SEND,
                "Notification saved (notificationId={}, userId={}, status={})",
                saved.getId(), saved.getUserId(), saved.getStatus()
        );

        // 2) Criar outbox
        OutBoxMessage message = new OutBoxMessage();
        message.setNotificationId(saved.getId());
        message.setChannel(channel);
        message.setStatus("PENDING");
        message.setAttempts(0);
        message.setSentAt(null);

        OutBoxMessage savedMsg = outBoxMessagesRepository.save(message);

        logger.info(OUTBOX_CREATE,
                "Outbox message created (outboxId={}, notificationId={}, channel={}, status={})",
                savedMsg.getId(), saved.getId(), channel, savedMsg.getStatus()
        );

        // 3) Enviar (apenas EMAIL aqui)
        if ("EMAIL".equalsIgnoreCase(channel)) {
            logger.info(EMAIL_FLOW,
                    "Attempting EMAIL send (notificationId={}, outboxId={}, to={}, subject={})",
                    saved.getId(), savedMsg.getId(), emailTo, saved.getTitle()
            );

            try {
                emailService.sendNotificationEmail(
                        emailTo,
                        saved.getTitle(),
                        saved.getBody()
                );

                savedMsg.setStatus("SENT");
                savedMsg.setAttempts(savedMsg.getAttempts() + 1);
                savedMsg.setSentAt(new Date());
                outBoxMessagesRepository.save(savedMsg);

                logger.info(OUTBOX_UPDATE,
                        "Outbox updated after success (outboxId={}, status={}, attempts={}, sentAt={})",
                        savedMsg.getId(), savedMsg.getStatus(), savedMsg.getAttempts(), savedMsg.getSentAt()
                );

            } catch (Exception e) {
                savedMsg.setStatus("FAILED");
                savedMsg.setAttempts(savedMsg.getAttempts() + 1);
                outBoxMessagesRepository.save(savedMsg);

                logger.error(OUTBOX_UPDATE,
                        "Outbox updated after failure (outboxId={}, status={}, attempts={})",
                        savedMsg.getId(), savedMsg.getStatus(), savedMsg.getAttempts(),
                        e
                );
            }
        } else {
            logger.debug(NOTIF_SEND, "Channel {} currently not implemented for immediate send (notificationId={})",
                    channel, saved.getId());
        }

        return saved;
    }
}
