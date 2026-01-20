package org.example.dtos.eventSessions;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class EventSessionUpdateDTO {

    private UUID id;

    private Instant startsAt;

    private Instant endsAt;

}
