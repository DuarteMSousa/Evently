package org.example.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public class NotificationDTO {

    private UUID id;
    private UUID userId;
    private String type;
    private String title;
    private String body;
    private String status;
    private Date createdAt;
    private Date readAt;
}