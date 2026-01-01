package org.example.dtos.eventSessions;

import lombok.Getter;
import lombok.Setter;
import org.example.dtos.sessionTiers.SessionTierDTO;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class EventSessionDTO {

    private UUID id;

    private UUID eventId;

    private UUID venueId;

    private Instant startsAt;

    private Instant endsAt;

    private UUID createdBy;

    private Date createdAt;

    private UUID updatedBy;

    private Date updatedAt;

    private List<SessionTierDTO> tiers;
}
