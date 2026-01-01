package org.example.dtos.externalServices.events;


import lombok.Getter;
import lombok.Setter;
import org.example.dtos.externalServices.categories.CategoryDTO;
import org.example.dtos.externalServices.eventSessions.EventSessionDTO;
import org.example.enums.externalServices.EventStatus;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
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
