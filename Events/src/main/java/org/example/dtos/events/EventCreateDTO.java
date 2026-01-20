package org.example.dtos.events;

import lombok.Getter;
import lombok.Setter;
import org.example.dtos.categories.CategoryDTO;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class EventCreateDTO {

    private String name;

    private String description;

    private UUID organizationId;

    private UUID createdBy;

    private List<CategoryDTO> categories;

}
