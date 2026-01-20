package org.example.dtos.externalServices.eventSessions;

import lombok.Getter;
import org.example.dtos.externalServices.sessionTiers.SessionTierDTO;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
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
