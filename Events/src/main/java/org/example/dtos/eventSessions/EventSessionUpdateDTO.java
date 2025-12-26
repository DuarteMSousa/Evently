package org.example.dtos.eventSessions;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class EventSessionUpdateDTO {

    private UUID id;

    private UUID eventId;

    private UUID venueId;

    private Instant startsAt;

    private Instant endsAt;

    private UUID createdBy;

    private Date createdAt;

    private UUID updatedBy;

    private Date updatedAt;

    private List<SessionTierUpdateDTO> tiers;
}
