package org.example.controller;

import org.example.dtos.NotificationCreateDTO;
import org.example.dtos.NotificationDTO;
import org.example.exceptions.InvalidNotificationException;
import org.example.exceptions.UserNotFoundException;
import org.example.models.Notification;
import org.example.service.NotificationsService;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationsController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationsController.class);

    private static final Marker NOTIFICATION_SEND = MarkerFactory.getMarker("NOTIFICATION_SEND");
    private static final Marker NOTIFICATION_VALIDATION = MarkerFactory.getMarker("NOTIFICATION_VALIDATION");

    @Autowired
    private NotificationsService notificationsService;

    private final ModelMapper modelMapper;

    public NotificationsController() {
        this.modelMapper = new ModelMapper();
    }

    @PostMapping("/send-notification")
    public ResponseEntity<?> sendNotification(@RequestBody NotificationCreateDTO dto) {
        /* HttpStatus(produces)
         * 201 CREATED - Notification created successfully and message/email queued/sent.
         * 404 NOT_FOUND - Target user does not exist.
         * 400 BAD_REQUEST - Invalid data provided / generic error.
         */
        logger.info(NOTIFICATION_SEND,
                "POST /notifications/send-notification requested (userId={}, type={}, channel={}, emailTo={})",
                dto.getUserId(), dto.getType(), dto.getChannel(), dto.getEmailTo());

        try {
            logger.debug(NOTIFICATION_VALIDATION,
                    "Mapping NotificationCreateDTO to Notification (userId={}, title={})",
                    dto.getUserId(), dto.getTitle());

            Notification notification = new Notification();
            notification.setUserId(dto.getUserId());
            notification.setType(dto.getType());
            notification.setTitle(dto.getTitle());
            notification.setBody(dto.getBody());

            Notification created = notificationsService.sendNotification(
                    notification,
                    dto.getChannel(),
                    dto.getEmailTo()
            );

            logger.info(NOTIFICATION_SEND,
                    "Send notification succeeded (notificationId={}, userId={}, type={}, channel={})",
                    created.getId(), created.getUserId(), created.getType(), dto.getChannel());

            NotificationDTO response = modelMapper.map(created, NotificationDTO.class);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (UserNotFoundException e) {
            logger.warn(NOTIFICATION_SEND,
                    "Send notification failed - user not found (userId={})",
                    dto.getUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch (InvalidNotificationException e) {
            logger.warn(NOTIFICATION_SEND,
                    "Send notification failed - invalid payload (userId={}) reason={}",
                    dto.getUserId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());

        } catch (Exception e) {
            logger.error(NOTIFICATION_SEND,
                    "Send notification failed - unexpected error (userId={})",
                    dto.getUserId(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
