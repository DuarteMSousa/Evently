package org.example.dtos.events;

import org.example.dtos.categories.CategoryDTO;
import org.example.dtos.eventSessions.EventSessionDTO;
import org.example.enums.EventStatus;
import org.example.models.Category;
import org.example.models.EventSession;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class EventDTO {

    private String name;

    private String description;

    private UUID organizationId;

    private EventStatus status;

    private UUID createdBy;

    private Date createdAt;

    private UUID updatedBy;

    private Date updatedAt;

    private List<EventSessionDTO> sessions;

    private List<CategoryDTO> categories;
}
