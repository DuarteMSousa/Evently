package org.example.service;

import feign.FeignException;
import jakarta.transaction.Transactional;
import org.example.clients.UsersClient;
import org.example.dtos.UserDTO;
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
    private UsersClient usersClient;

    @Autowired
    private EmailService emailService;

    /**
     * Validates a notification payload and delivery parameters before sending/queuing.
     *
     * @param notification notification payload to validate
     * @param channel      delivery channel (e.g. "EMAIL")
     * @param emailTo      destination email (required when channel == EMAIL)
     * @throws InvalidNotificationException if any required field is missing or invalid
     * @throws UserNotFoundException        if the user is considered not found by the current validation rule
     */
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

    /**
     * Sends a notification through a given channel.
     *
     * @param notification notification payload
     * @param channel      delivery channel (e.g. "EMAIL")
     * @param emailTo      destination email (required when channel == EMAIL)
     * @return persisted notification
     * @throws InvalidNotificationException if payload is invalid
     * @throws UserNotFoundException        if user is considered not found by the current validation rule
     */
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

        notification.setStatus("UNREAD");
        Notification saved = notificationsRepository.save(notification);

        logger.info(NOTIF_SEND,
                "Notification saved (notificationId={}, userId={}, status={})",
                saved.getId(), saved.getUserId(), saved.getStatus()
        );

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

    @Transactional
    public Notification notifyPaymentCaptured(UUID userId, UUID orderId, float amount) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType("PAYMENT");
        n.setTitle("Pagamento confirmado");
        n.setBody("O pagamento da encomenda " + orderId + " foi confirmado. Total: " + amount);
        return sendNotification(n, "IN_APP", null);
    }

    @Transactional
    public Notification notifyPaymentFailed(UUID userId, UUID orderId, float amount) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType("PAYMENT");
        n.setTitle("Pagamento falhou");
        n.setBody("O pagamento da encomenda " + orderId + " falhou. Total: " + amount);
        return sendNotification(n, "IN_APP", null);
    }

    @Transactional
    public Notification notifyPaymentRefunded(UUID userId, UUID orderId, float amount) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType("REFUND");
        n.setTitle("Reembolso realizado");
        n.setBody("Foi feito reembolso da encomenda " + orderId + ". Valor: " + amount);
        return sendNotification(n, "IN_APP", null);
    }

    @Transactional
    public Notification notifyPdfGenerated(UUID userId, UUID orderId, String fileName, String url) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType("FILE");
        n.setTitle("PDF gerado");
        n.setBody("O PDF " + fileName + " da encomenda " + orderId + " está pronto. Link: " + url);
        return sendNotification(n, "IN_APP", null);
    }

    @Transactional
    public Notification notifyPdfFailed(UUID userId, UUID orderId, String fileName) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType("FILE");
        n.setTitle("Falha ao gerar PDF");
        n.setBody("Falhou a geração do PDF " + fileName + " da encomenda " + orderId + ".");
        return sendNotification(n, "IN_APP", null);
    }

    @Transactional
    public Notification notifyRefundRequestSent(UUID userId, UUID refundRequestId, String content) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType("REFUND");
        n.setTitle("Pedido de reembolso submetido");
        n.setBody("O teu pedido de reembolso foi enviado. Nº: " + refundRequestId + ". Mensagem: " + content);
        return sendNotification(n, "IN_APP", null);
    }

    @Transactional
    public Notification notifyRefundDecision(UUID userId, UUID paymentId, String decisionType, String description) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType("REFUND");

        boolean accepted = "ACCEPTED".equalsIgnoreCase(decisionType)
                || "APPROVE".equalsIgnoreCase(decisionType);

        n.setTitle(accepted ? "Reembolso aprovado" : "Reembolso rejeitado");

        String body = accepted
                ? "O teu pedido de reembolso foi aprovado."
                : "O teu pedido de reembolso foi rejeitado.";

        if (description != null && !description.trim().isEmpty()) {
            body += " Motivo: " + description;
        }

        body += " (Pagamento: " + paymentId + ")";

        n.setBody(body);
        return sendNotification(n, "IN_APP", null);
    }

}
