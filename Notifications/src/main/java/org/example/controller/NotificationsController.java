package org.example.controller;

import org.example.dtos.NotificationCreateDTO;
import org.example.dtos.NotificationDTO;
import org.example.exceptions.InvalidNotificationException;
import org.example.exceptions.UserNotFoundException;
import org.example.models.Notification;
import org.example.service.NotificationsService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationsController {

    @Autowired
    private NotificationsService notificationsService;

    private final ModelMapper modelMapper;

    public NotificationsController() {
        this.modelMapper = new ModelMapper();
    }

    @PostMapping("/send-notification")
    public ResponseEntity<?> sendNotification(@RequestBody NotificationCreateDTO dto) {
        /*
         * 201 – Notificação criada + email enfileirado/enviado
         * 400 – Campos inválidos
         * 404 – Utilizador não encontrado
         */

        try {
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

            NotificationDTO response = modelMapper.map(created, NotificationDTO.class);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (InvalidNotificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
