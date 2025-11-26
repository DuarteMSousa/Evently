package org.example.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class NotificationCreateDTO {

    private UUID userId;
    private String type;
    private String title;
    private String body;
    private String channel;
    private String emailTo;
}