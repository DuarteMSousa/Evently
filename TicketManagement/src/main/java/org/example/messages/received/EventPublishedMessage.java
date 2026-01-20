package org.example.messages.received;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.dtos.externalServices.categories.CategoryDTO;
import org.example.dtos.externalServices.eventSessions.EventSessionDTO;
import org.example.enums.externalServices.events.EventStatus;


import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class EventPublishedMessage {

    private UUID id;

    private String name;

    private String description;

    private UUID organizationId;

    private EventStatus status;

    private List<EventSessionDTO> sessions;

    private List<CategoryDTO> categories;
}
