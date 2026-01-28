package org.example.dtos.categories;

import lombok.Getter;
import lombok.Setter;
import org.example.dtos.events.EventDTO;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CategoryDTO {

    private UUID id;

    private String name;

    private UUID createdBy;

    private Date createdAt;

    private UUID updatedBy;

    private Date updatedAt;

}
