package org.example.events;

import org.example.dtos.categories.CategoryDTO;
import org.example.dtos.eventSessions.EventSessionDTO;
import org.example.enums.EventStatus;

import java.util.List;
import java.util.UUID;

public class EventPublishedEvent {

    private UUID id;

    private String name;

    private String description;

    private UUID organizationId;

    private EventStatus status;

    private List<EventSessionDTO> sessions;

    private List<CategoryDTO> categories;
}
