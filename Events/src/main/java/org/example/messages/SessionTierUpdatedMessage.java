package org.example.messages;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
public class SessionTierUpdatedMessage {

    private UUID id;

    private UUID eventSessionId;

    private UUID zoneId;

    private float price;

    private UUID createdBy;

    private Date createdAt;

    private UUID updatedBy;

    private Date updatedAt;
}
