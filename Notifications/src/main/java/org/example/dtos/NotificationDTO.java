package org.example.dtos;

import lombok.Getter;
import lombok.Setter;
import org.example.enums.NotificationType;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class NotificationDTO {

    private UUID id;
    private UUID userId;
    private NotificationType type;
    private String title;
    private String body;
    private String status;
    private Date createdAt;
    private Date readAt;
}