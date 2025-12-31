package org.example.dtos.events;

import org.example.enums.EventStatus;
import org.example.models.Category;
import org.example.models.EventSession;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class EventUpdateDTO {

    private String name;

    private String description;

    private EventStatus status;

    private List<Category> categories;
}
