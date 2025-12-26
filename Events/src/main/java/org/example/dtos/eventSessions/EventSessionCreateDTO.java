package org.example.dtos.eventSessions;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class EventSessionCreateDTO {

    private UUID eventId;

    private UUID venueId;

    private Instant startsAt;

    private Instant endsAt;

    private List<SessionTierCreateDTO> tiers;
}
