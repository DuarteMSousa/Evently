package org.example.dtos.eventSessions;

import jakarta.persistence.*;
import org.example.models.Event;
import org.example.models.SessionTier;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
