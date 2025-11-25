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

    // para OutBox
    private String channel; // EMAIL, PUSH, SMS

    private String emailTo;
}