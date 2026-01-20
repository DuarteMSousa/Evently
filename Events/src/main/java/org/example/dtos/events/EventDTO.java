package org.example.dtos.events;

import lombok.Getter;
import lombok.Setter;
import org.example.dtos.categories.CategoryDTO;
import org.example.dtos.eventSessions.EventSessionDTO;
import org.example.enums.EventStatus;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class EventDTO {

    private UUID id;

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
