package org.example.dtos.events;

import lombok.Getter;
import lombok.Setter;
import org.example.models.Category;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class EventUpdateDTO {

    private UUID id;

    private String name;

    private String description;

    private List<Category> categories;

}
