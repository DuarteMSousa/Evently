package org.example.service;

import feign.FeignException;
import jakarta.transaction.Transactional;
import org.example.clients.UsersClient;
import org.example.dtos.UserDTO;
import org.example.enums.NotificationChannel;
import org.example.enums.NotificationType;
import org.example.enums.OutboxStatus;
import org.example.enums.externalServices.DecisionType;
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
import org.springframework.http.ResponseEntity;
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
    private static final Marker USERS_CLIENT = MarkerFactory.getMarker("USERS_CLIENT");

    @Autowired
    private NotificationsRepository notificationsRepository;

    @Autowired
    private OutBoxMessagesRepository outBoxMessagesRepository;

    @Autowired
    private UsersClient usersClient;

    @Autowired
    private EmailService emailService;

    // -------------------------
    // Helpers
    // -------------------------

    private String resolveUserEmail(UUID userId) {
        try {
            ResponseEntity<UserDTO> resp = usersClient.getUser(userId);
            UserDTO user = resp != null ? resp.getBody() : null;

            if (user == null || user.getEmail() == null) {
                logger.warn(USERS_CLIENT, "Users service returned null/empty email (userId={})", userId);
                return null;
            }

            String email = user.getEmail().trim();
            if (email.isEmpty()) {
                logger.warn(USERS_CLIENT, "Users service returned blank email (userId={})", userId);
                return null;
            }

            return email;

        } catch (FeignException.NotFound e) {
            logger.warn(USERS_CLIENT, "User not found in Users service (userId={})", userId);
            return null;

        } catch (FeignException e) {
            logger.error(USERS_CLIENT, "Users service error while fetching user (userId={})", userId, e);
            return null;

        } catch (Exception e) {
            logger.error(USERS_CLIENT, "Unexpected error while fetching user email (userId={})", userId, e);
            return null;
        }
    }

    /**
     * Envia IN_APP sempre e (opcionalmente) EMAIL, buscando o email no Users pelo userId.
     */
    private Notification sendInAppAndMaybeEmail(Notification base, boolean sendEmail) {

        Notification inApp = sendNotification(base, NotificationChannel.IN_APP, null);

        if (sendEmail) {
            String email = resolveUserEmail(base.getUserId());
            if (email != null) {
                Notification emailNotif = new Notification();
                emailNotif.setUserId(base.getUserId());
                emailNotif.setType(base.getType());
                emailNotif.setTitle(base.getTitle());
                emailNotif.setBody(base.getBody());

                sendNotification(emailNotif, NotificationChannel.EMAIL, email);
            } else {
                logger.warn(EMAIL_FLOW, "Skipping EMAIL send - could not resolve user email (userId={})", base.getUserId());
            }
        }

        return inApp;
    }


    // -------------------------
    // Validation + Core send
    // -------------------------

    private void validateNotification(Notification notification, NotificationChannel channel, String emailTo) {

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

        if (channel == NotificationChannel.EMAIL && (emailTo == null || emailTo.trim().isEmpty())) {
            logger.warn(NOTIF_VALIDATE, "EMAIL channel requires emailTo (userId={})", notification.getUserId());
            throw new InvalidNotificationException("emailTo is required for EMAIL channel");
        }

        if (notification.getUserId().equals(new UUID(0L, 0L))) {
            logger.warn(NOTIF_VALIDATE, "User not found (userId={})", notification.getUserId());
            throw new UserNotFoundException("User not found");
        }
    }

    @Transactional
    public Notification sendNotification(Notification notification, NotificationChannel  channel, String emailTo) {

        logger.info(NOTIF_SEND,
                "Send notification requested (userId={}, type={}, channel={})",
                notification != null ? notification.getUserId() : null,
                notification != null ? notification.getType() : null,
                channel
        );

        validateNotification(notification, channel, emailTo);

        notification.setStatus("UNREAD");
        Notification saved = notificationsRepository.save(notification);

        logger.info(NOTIF_SEND,
                "Notification saved (notificationId={}, userId={}, status={})",
                saved.getId(), saved.getUserId(), saved.getStatus()
        );

        OutBoxMessage message = new OutBoxMessage();
        message.setNotificationId(saved.getId());
        message.setChannel(channel);
        message.setStatus(OutboxStatus.PENDING);
        message.setAttempts(0);
        message.setSentAt(null);

        OutBoxMessage savedMsg = outBoxMessagesRepository.save(message);

        logger.info(OUTBOX_CREATE,
                "Outbox message created (outboxId={}, notificationId={}, channel={}, status={})",
                savedMsg.getId(), saved.getId(), channel, savedMsg.getStatus()
        );

        if (channel == NotificationChannel.EMAIL) {
            logger.info(EMAIL_FLOW,
                    "Attempting EMAIL send (notificationId={}, outboxId={}, to={}, subject={})",
                    saved.getId(), savedMsg.getId(), emailTo, saved.getTitle()
            );

            try {
                emailService.sendNotificationEmail(emailTo, saved.getTitle(), saved.getBody());

                savedMsg.setStatus(OutboxStatus.SENT);
                savedMsg.setAttempts(savedMsg.getAttempts() + 1);
                savedMsg.setSentAt(new Date());
                outBoxMessagesRepository.save(savedMsg);

                logger.info(OUTBOX_UPDATE,
                        "Outbox updated after success (outboxId={}, status={}, attempts={}, sentAt={})",
                        savedMsg.getId(), savedMsg.getStatus(), savedMsg.getAttempts(), savedMsg.getSentAt()
                );

            } catch (Exception e) {
                savedMsg.setStatus(OutboxStatus.FAILED);
                savedMsg.setAttempts(savedMsg.getAttempts() + 1);
                outBoxMessagesRepository.save(savedMsg);

                logger.error(OUTBOX_UPDATE,
                        "Outbox updated after failure (outboxId={}, status={}, attempts={})",
                        savedMsg.getId(), savedMsg.getStatus(), savedMsg.getAttempts(), e
                );
            }
        } else {
            logger.debug(NOTIF_SEND, "Channel {} currently not implemented for immediate send (notificationId={})",
                    channel, saved.getId());
        }

        return saved;
    }

    // -------------------------
    // Domain notify methods
    // -------------------------

    @Transactional
    public Notification notifyPaymentCaptured(UUID userId, UUID orderId, float amount) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(NotificationType.PAYMENT);
        n.setTitle("Pagamento confirmado");
        n.setBody("O pagamento da encomenda " + orderId + " foi confirmado. Total: " + amount);

        return sendInAppAndMaybeEmail(n, true);
    }

    @Transactional
    public Notification notifyPaymentFailed(UUID userId, UUID orderId, float amount) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(NotificationType.PAYMENT);
        n.setTitle("Pagamento falhou");
        n.setBody("O pagamento da encomenda " + orderId + " falhou. Total: " + amount);

        return sendInAppAndMaybeEmail(n, true);
    }

    @Transactional
    public Notification notifyPaymentRefunded(UUID userId, UUID orderId, float amount) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(NotificationType.REFUND);
        n.setTitle("Reembolso realizado");
        n.setBody("Foi feito reembolso da encomenda " + orderId + ". Valor: " + amount);

        return sendInAppAndMaybeEmail(n, true);
    }

    @Transactional
    public Notification notifyPdfGenerated(UUID userId, UUID orderId, String fileName, String url) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(NotificationType.FILE);
        n.setTitle("PDF gerado");
        n.setBody("O PDF " + fileName + " da encomenda " + orderId + " está pronto. Link: " + url);

        return sendInAppAndMaybeEmail(n, true);
    }

    @Transactional
    public Notification notifyRefundRequestSent(UUID userId, UUID refundRequestId, String content) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(NotificationType.REFUND);
        n.setTitle("Pedido de reembolso submetido");
        n.setBody("O teu pedido de reembolso foi enviado. Nº: " + refundRequestId + ". Mensagem: " + content);

        return sendInAppAndMaybeEmail(n, true);
    }

    @Transactional
    public Notification notifyRefundDecision(UUID userId, UUID paymentId, DecisionType decisionType, String description) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(NotificationType.REFUND);

        boolean approved = decisionType == DecisionType.APPROVE;

        n.setTitle(approved ? "Reembolso aprovado" : "Reembolso rejeitado");

        String body = approved
                ? "O teu pedido de reembolso foi aprovado."
                : "O teu pedido de reembolso foi rejeitado.";

        if (description != null && !description.trim().isEmpty()) {
            body += " Motivo: " + description;
        }

        body += " (Pagamento: " + paymentId + ")";
        n.setBody(body);

        return sendInAppAndMaybeEmail(n, true);
    }
}
