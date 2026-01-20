package org.example.dtos.eventSessions;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class EventSessionCreateDTO {

    private UUID eventId;

    private UUID venueId;

    private Instant startsAt;

    private Instant endsAt;

}
