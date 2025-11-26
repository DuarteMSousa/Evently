package org.example.service;

import org.example.exceptions.InvalidNotificationException;
import org.example.exceptions.UserNotFoundException;
import org.example.models.Notification;
import org.example.models.OutBoxMessage;
import org.example.repositories.NotificationsRepository;
import org.example.repositories.OutBoxMessagesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.UUID;

@Service
public class NotificationsService {

    @Autowired
    private NotificationsRepository notificationsRepository;

    @Autowired
    private OutBoxMessagesRepository outBoxMessagesRepository;

    @Autowired
    private EmailService emailService;

    private void validateNotification(Notification notification,
                                      String channel,
                                      String emailTo) {
        if (notification.getUserId() == null) {
            throw new InvalidNotificationException("UserId is required");
        }
        if (notification.getType() == null) {
            throw new InvalidNotificationException("Type is required");
        }
        if (notification.getTitle() == null) {
            throw new InvalidNotificationException("Title is required");
        }
        if (notification.getBody() == null) {
            throw new InvalidNotificationException("Body is required");
        }
        if (channel == null) {
            throw new InvalidNotificationException("Channel is required");
        }

        if ("EMAIL".equalsIgnoreCase(channel)) {
            if (emailTo == null) {
                throw new InvalidNotificationException("emailTo is required for EMAIL channel");
            }
        }

        // aqui mais tarde podes integrar com Users para validar se o user existe
        if (notification.getUserId().equals(new UUID(0L, 0L))) {
            throw new UserNotFoundException("User not found");
        }
    }

    @Transactional
    public Notification sendNotification(Notification notification,
                                         String channel,
                                         String emailTo) {

        validateNotification(notification, channel, emailTo);

        notification.setStatus("UNREAD");
        Notification saved = notificationsRepository.save(notification);

        OutBoxMessage message = new OutBoxMessage();
        message.setNotificationId(saved.getId());
        message.setChannel(channel);
        message.setStatus("PENDING");
        message.setAttempts(0);
        message.setSentAt(null);

        OutBoxMessage savedMsg = outBoxMessagesRepository.save(message);

        if ("EMAIL".equalsIgnoreCase(channel)) {
            try {
                emailService.sendNotificationEmail(emailTo,
                        notification.getTitle(),
                        notification.getBody());

                savedMsg.setStatus("SENT");
                savedMsg.setAttempts(savedMsg.getAttempts() + 1);
                savedMsg.setSentAt(new Date());
                outBoxMessagesRepository.save(savedMsg);
            } catch (Exception e) {
                savedMsg.setStatus("FAILED");
                savedMsg.setAttempts(savedMsg.getAttempts() + 1);
                outBoxMessagesRepository.save(savedMsg);
            }
        }

        return saved;
    }
}
