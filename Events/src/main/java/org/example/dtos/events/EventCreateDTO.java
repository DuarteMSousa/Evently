package org.example.dtos.events;

import org.example.dtos.categories.CategoryDTO;
import org.example.dtos.eventSessions.EventSessionCreateDTO;
import org.example.enums.EventStatus;
import org.example.models.Category;
import org.example.models.EventSession;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class EventCreateDTO {

    private String name;

    private String description;

    private EventStatus status;

    private List<EventSessionCreateDTO> sessions;

    private List<CategoryDTO> categories;
}
