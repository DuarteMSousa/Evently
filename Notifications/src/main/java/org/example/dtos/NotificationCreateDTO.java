package org.example.dtos;

import lombok.Getter;
import lombok.Setter;
import org.example.enums.NotificationChannel;
import org.example.enums.NotificationType;

import java.util.UUID;

@Getter
@Setter
public class NotificationCreateDTO {

    private UUID userId;
    private NotificationType type;
    private String title;
    private String body;
    private NotificationChannel channel;
    private String emailTo;
}